package com.iqscaffold.billingservice.webhook;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.invoice.Invoice;
import com.iqscaffold.billingservice.invoice.InvoiceRepository;
import com.iqscaffold.billingservice.payment.Payment;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.payment.PaymentProviderFactory;
import com.iqscaffold.billingservice.payment.PaymentRepository;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.event.InvoicePaid;
import com.iqscaffold.billingservice.shared.event.PaymentFailed;
import com.iqscaffold.billingservice.shared.event.PaymentRefunded;
import com.iqscaffold.billingservice.shared.event.PaymentSucceeded;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for webhook processing from payment providers.
 *
 * <p>This service provides a thin orchestration layer for webhook event processing,
 * implementing async processing patterns for reliable webhook handling.
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Async Processing:</strong> Webhook endpoint returns 200 OK immediately,
 *       actual processing happens asynchronously via RabbitMQ</li>
 *   <li><strong>Idempotency:</strong> Critical for webhooks - providers may send duplicate events</li>
 *   <li><strong>Retry Logic:</strong> Failed webhook processing retried with exponential backoff</li>
 *   <li><strong>Signature Verification:</strong> Always verify webhook signatures before processing</li>
 *   <li><strong>Event Logging:</strong> Log all webhook events for debugging and audit</li>
 * </ul>
 *
 * <h2>Processing Pattern</h2>
 * <ol>
 *   <li>Webhook endpoint validates signature</li>
 *   <li>Publishes event to RabbitMQ queue</li>
 *   <li>Returns 200 OK immediately (< 5 seconds)</li>
 *   <li>Consumer fetches from queue</li>
 *   <li>Processes event and updates domain</li>
 *   <li>Publishes domain events</li>
 * </ol>
 *
 * <h2>Supported Event Types</h2>
 * <ul>
 *   <li>payment_intent.succeeded - Payment completed successfully</li>
 *   <li>payment_intent.failed - Payment failed</li>
 *   <li>charge.refunded - Payment refunded</li>
 *   <li>customer.subscription.updated - Subscription updated</li>
 * </ul>
 */
@Service
public class WebhookApplicationService {

  private static final Logger log = LoggerFactory.getLogger(WebhookApplicationService.class);

  private final PaymentProviderFactory paymentProviderFactory;
  private final WebhookEventRepository webhookEventRepository;
  private final PaymentRepository paymentRepository;
  private final InvoiceRepository invoiceRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final DomainEventPublisher eventPublisher;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public WebhookApplicationService(final PaymentProviderFactory paymentProviderFactory,
                                   final WebhookEventRepository webhookEventRepository,
                                   final PaymentRepository paymentRepository,
                                   final InvoiceRepository invoiceRepository,
                                   final SubscriptionRepository subscriptionRepository,
                                   final DomainEventPublisher eventPublisher,
                                   final RabbitTemplate rabbitTemplate,
                                   final ObjectMapper objectMapper) {
    this.paymentProviderFactory = paymentProviderFactory;
    this.webhookEventRepository = webhookEventRepository;
    this.paymentRepository = paymentRepository;
    this.invoiceRepository = invoiceRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.eventPublisher = eventPublisher;
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  private static final String WEBHOOK_EXCHANGE = "billing.webhooks";
  private static final int WEBHOOK_EVENT_TTL_DAYS = 30;

  /**
   * Verifies webhook signature for a given provider.
   *
   * @param provider  the payment provider name (stripe, paypal)
   * @param payload   the raw webhook payload
   * @param signature the signature header value
   * @return true if signature is valid, false otherwise
   */
  public boolean verifyWebhookSignature(String provider, String payload, String signature) {
    log.debug("Verifying webhook signature for provider: {}", provider);

    try {
      PaymentProviderAdapter providerAdapter = paymentProviderFactory.getProvider(provider);
      return providerAdapter.verifyWebhookSignature(payload, signature);
    } catch (final Exception e) {
      log.error("Error verifying webhook signature for provider: {}", provider, e);
      return false;
    }
  }

  /**
   * Processes Stripe webhook events.
   *
   * <p><strong>Sync Response Pattern:</strong> Validates signature → publishes to queue → returns 200 OK
   *
   * <p><strong>Why Async:</strong> Payment providers expect fast response (< 5 seconds),
   * processing may take longer. Async processing prevents timeout and allows retry logic.
   *
   * @param payload   the raw webhook payload (JSON string)
   * @param signature the Stripe signature header value
   * @return webhook event ID for tracking
   * @throws PaymentException if signature verification fails
   */
  @Transactional
  public String processStripeWebhook(String payload, String signature) {
    log.info("Received Stripe webhook");

    // Verify webhook signature
    PaymentProviderAdapter provider = paymentProviderFactory.getProvider("stripe");
    if (!provider.verifyWebhookSignature(payload, signature)) {
      log.error("Invalid Stripe webhook signature");
      throw new PaymentException("Invalid webhook signature");
    }

    try {
      // Parse webhook event
      JsonNode event = objectMapper.readTree(payload);
      String eventId = event.get("id").asText();
      String eventType = event.get("type").asText();

      // Check idempotency - return success if already processed
      if (webhookEventRepository.existsByProviderEventId(eventId)) {
        log.info("Webhook event already processed: {}", eventId);
        return eventId;
      }

      // Store webhook event for idempotency and audit
      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId(eventId)
          .provider("stripe")
          .eventType(eventType)
          .payload(payload)
          .signature(signature)
          .status(WebhookEventStatus.PENDING)
          .receivedAt(LocalDateTime.now())
          .build();

      webhookEventRepository.save(webhookEvent);

      // Publish to RabbitMQ for async processing
      rabbitTemplate.convertAndSend(
          WEBHOOK_EXCHANGE,
          "stripe." + eventType.replace('.', '_'),
          webhookEvent
      );

      log.info("Stripe webhook event queued for processing: {} ({})", eventId, eventType);
      return eventId;

    } catch (final Exception e) {
      log.error("Error processing Stripe webhook", e);
      throw new PaymentException("Error processing webhook", e);
    }
  }

  /**
   * Processes PayPal webhook events.
   *
   * <p><strong>Sync Response Pattern:</strong> Validates signature → publishes to queue → returns 200 OK
   *
   * @param payload   the raw webhook payload (JSON string)
   * @param signature the PayPal signature header value
   * @return webhook event ID for tracking
   * @throws PaymentException if signature verification fails
   */
  @Transactional
  public String processPayPalWebhook(String payload, String signature) {
    log.info("Received PayPal webhook");

    // Verify webhook signature
    PaymentProviderAdapter provider = paymentProviderFactory.getProvider("paypal");
    if (!provider.verifyWebhookSignature(payload, signature)) {
      log.error("Invalid PayPal webhook signature");
      throw new PaymentException("Invalid webhook signature");
    }

    try {
      // Parse webhook event
      JsonNode event = objectMapper.readTree(payload);
      String eventId = event.get("id").asText();
      String eventType = event.get("event_type").asText();

      // Check idempotency - return success if already processed
      if (webhookEventRepository.existsByProviderEventId(eventId)) {
        log.info("Webhook event already processed: {}", eventId);
        return eventId;
      }

      // Store webhook event for idempotency and audit
      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId(eventId)
          .provider("paypal")
          .eventType(eventType)
          .payload(payload)
          .signature(signature)
          .status(WebhookEventStatus.PENDING)
          .receivedAt(LocalDateTime.now())
          .build();

      webhookEventRepository.save(webhookEvent);

      // Publish to RabbitMQ for async processing
      rabbitTemplate.convertAndSend(
          WEBHOOK_EXCHANGE,
          "paypal." + eventType.replace('.', '_'),
          webhookEvent
      );

      log.info("PayPal webhook event queued for processing: {} ({})", eventId, eventType);
      return eventId;

    } catch (final Exception e) {
      log.error("Error processing PayPal webhook", e);
      throw new PaymentException("Error processing webhook", e);
    }
  }

  /**
   * Handles payment_intent.succeeded event (async processing).
   *
   * <p><strong>Async Handler:</strong> Processes event from queue, updates domain, publishes domain events.
   *
   * @param webhookEvent the webhook event to process
   */
  @Transactional
  public void handlePaymentSucceeded(WebhookEvent webhookEvent) {
    log.info("Processing payment_intent.succeeded event: {}", webhookEvent.getProviderEventId());

    try {
      JsonNode event = objectMapper.readTree(webhookEvent.getPayload());
      JsonNode paymentIntent = event.get("data").get("object");

      String providerPaymentId = paymentIntent.get("id").asText();

      // Find payment by provider ID
      Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(providerPaymentId);

      if (paymentOpt.isEmpty()) {
        log.warn("Payment not found for provider ID: {}", providerPaymentId);
        webhookEvent.setStatus(WebhookEventStatus.FAILED);
        webhookEvent.setErrorMessage("Payment not found");
        webhookEventRepository.save(webhookEvent);
        return;
      }

      Payment payment = paymentOpt.get();

      // Update payment status using aggregate method
      payment.markAsSucceeded(providerPaymentId);
      paymentRepository.save(payment);

      // Update invoice if associated
      Invoice invoice = payment.getInvoice();
      if (invoice != null) {
        LocalDateTime now = LocalDateTime.now();
        invoice.markAsPaid(now);
        invoiceRepository.save(invoice);

        // Publish InvoicePaid event
        eventPublisher.publish(new InvoicePaid(
            UUID.randomUUID(),
            Instant.now(),
            invoice.getId(),
            payment.getTenantId(),
            invoice.getSubscription() != null ? invoice.getSubscription().getId() : null,
            invoice.getInvoiceNumber(),
            payment.getId(),
            invoice.getTotal(),
            invoice.getCurrency(),
            now.atZone(java.time.ZoneId.systemDefault()).toInstant()
        ));
      }

      // Publish PaymentSucceeded event
      eventPublisher.publish(new PaymentSucceeded(
          UUID.randomUUID(),
          Instant.now(),
          payment.getId(),
          payment.getTenantId(),
          invoice != null ? invoice.getId() : null,
          invoice != null && invoice.getSubscription() != null ? invoice.getSubscription().getId() : null,
          payment.getAmount(),
          payment.getCurrency(),
          payment.getPaymentMethod() != null ? payment.getPaymentMethod().getId() : null,
          providerPaymentId
      ));

      // Mark webhook as processed
      webhookEvent.setStatus(WebhookEventStatus.PROCESSED);
      webhookEvent.setProcessedAt(LocalDateTime.now());
      webhookEventRepository.save(webhookEvent);

      log.info("Payment succeeded event processed: {}", providerPaymentId);

    } catch (final Exception e) {
      log.error("Error handling payment succeeded event", e);
      webhookEvent.setStatus(WebhookEventStatus.FAILED);
      webhookEvent.setErrorMessage(e.getMessage());
      webhookEvent.setRetryCount(webhookEvent.getRetryCount() + 1);
      webhookEventRepository.save(webhookEvent);
      throw new PaymentException("Error handling payment succeeded event", e);
    }
  }

  /**
   * Handles payment_intent.failed event (async processing).
   *
   * @param webhookEvent the webhook event to process
   */
  @Transactional
  public void handlePaymentFailed(WebhookEvent webhookEvent) {
    log.info("Processing payment_intent.failed event: {}", webhookEvent.getProviderEventId());

    try {
      JsonNode event = objectMapper.readTree(webhookEvent.getPayload());
      JsonNode paymentIntent = event.get("data").get("object");

      String providerPaymentId = paymentIntent.get("id").asText();
      String failureReason = paymentIntent.has("last_payment_error")
          ? paymentIntent.get("last_payment_error").get("message").asText()
          : "Unknown error";

      // Find payment by provider ID
      Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(providerPaymentId);

      if (paymentOpt.isEmpty()) {
        log.warn("Payment not found for provider ID: {}", providerPaymentId);
        webhookEvent.setStatus(WebhookEventStatus.FAILED);
        webhookEvent.setErrorMessage("Payment not found");
        webhookEventRepository.save(webhookEvent);
        return;
      }

      Payment payment = paymentOpt.get();

      // Update payment status using aggregate method
      payment.markAsFailed(failureReason);
      paymentRepository.save(payment);

      // Publish PaymentFailed event (triggers retry scheduling)
      Invoice invoice = payment.getInvoice();
      eventPublisher.publish(new PaymentFailed(
          UUID.randomUUID(),
          Instant.now(),
          payment.getId(),
          payment.getTenantId(),
          invoice != null ? invoice.getId() : null,
          invoice != null && invoice.getSubscription() != null ? invoice.getSubscription().getId() : null,
          payment.getAmount(),
          payment.getCurrency(),
          payment.getPaymentMethod() != null ? payment.getPaymentMethod().getId() : null,
          failureReason,
          0 // Initial retry attempt
      ));

      // Mark webhook as processed
      webhookEvent.setStatus(WebhookEventStatus.PROCESSED);
      webhookEvent.setProcessedAt(LocalDateTime.now());
      webhookEventRepository.save(webhookEvent);

      log.info("Payment failed event processed: {}", providerPaymentId);

    } catch (final Exception e) {
      log.error("Error handling payment failed event", e);
      webhookEvent.setStatus(WebhookEventStatus.FAILED);
      webhookEvent.setErrorMessage(e.getMessage());
      webhookEvent.setRetryCount(webhookEvent.getRetryCount() + 1);
      webhookEventRepository.save(webhookEvent);
      throw new PaymentException("Error handling payment failed event", e);
    }
  }

  /**
   * Handles charge.refunded event (async processing).
   *
   * @param webhookEvent the webhook event to process
   */
  @Transactional
  public void handleChargeRefunded(WebhookEvent webhookEvent) {
    log.info("Processing charge.refunded event: {}", webhookEvent.getProviderEventId());

    try {
      JsonNode event = objectMapper.readTree(webhookEvent.getPayload());
      JsonNode charge = event.get("data").get("object");

      String providerPaymentId = charge.get("payment_intent").asText();
      BigDecimal refundAmount = new BigDecimal(charge.get("amount_refunded").asText()).divide(new BigDecimal("100"));
      String currency = charge.get("currency").asText().toUpperCase();

      // Find payment by provider ID
      Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(providerPaymentId);

      if (paymentOpt.isEmpty()) {
        log.warn("Payment not found for provider ID: {}", providerPaymentId);
        webhookEvent.setStatus(WebhookEventStatus.FAILED);
        webhookEvent.setErrorMessage("Payment not found");
        webhookEventRepository.save(webhookEvent);
        return;
      }

      Payment payment = paymentOpt.get();

      // Update payment status using aggregate method
      payment.processRefund(refundAmount);
      paymentRepository.save(payment);

      // Publish PaymentRefunded event
      Invoice invoice = payment.getInvoice();
      eventPublisher.publish(new PaymentRefunded(
          UUID.randomUUID(),
          Instant.now(),
          payment.getId(),
          payment.getTenantId(),
          invoice != null ? invoice.getId() : null,
          invoice != null && invoice.getSubscription() != null ? invoice.getSubscription().getId() : null,
          refundAmount,
          currency,
          "Refund processed via webhook",
          null, // refundedBy not available from webhook
          charge.get("id").asText() // provider refund ID
      ));

      // Mark webhook as processed
      webhookEvent.setStatus(WebhookEventStatus.PROCESSED);
      webhookEvent.setProcessedAt(LocalDateTime.now());
      webhookEventRepository.save(webhookEvent);

      log.info("Charge refunded event processed: {}", providerPaymentId);

    } catch (final Exception e) {
      log.error("Error handling charge refunded event", e);
      webhookEvent.setStatus(WebhookEventStatus.FAILED);
      webhookEvent.setErrorMessage(e.getMessage());
      webhookEvent.setRetryCount(webhookEvent.getRetryCount() + 1);
      webhookEventRepository.save(webhookEvent);
      throw new PaymentException("Error handling charge refunded event", e);
    }
  }

  /**
   * Handles customer.subscription.updated event (async processing).
   *
   * <p><strong>Note:</strong> This handler is currently a placeholder.
   * Full implementation requires adding providerSubscriptionId field to Subscription entity.
   *
   * @param webhookEvent the webhook event to process
   */
  @Transactional
  public void handleSubscriptionUpdated(WebhookEvent webhookEvent) {
    log.info("Processing customer.subscription.updated event: {}", webhookEvent.getProviderEventId());

    try {
      JsonNode event = objectMapper.readTree(webhookEvent.getPayload());
      JsonNode subscription = event.get("data").get("object");

      String providerSubscriptionId = subscription.get("id").asText();
      String status = subscription.get("status").asText();

      // TODO: Implement subscription synchronization when providerSubscriptionId field is added
      // For now, just log the event and mark as processed
      log.info("Subscription webhook received: {} with status {}", providerSubscriptionId, status);
      log.warn("Subscription synchronization not yet implemented - requires providerSubscriptionId field");

      // Mark webhook as processed
      webhookEvent.setStatus(WebhookEventStatus.PROCESSED);
      webhookEvent.setProcessedAt(LocalDateTime.now());
      webhookEventRepository.save(webhookEvent);

      log.info("Subscription updated event logged: {}", providerSubscriptionId);

    } catch (final Exception e) {
      log.error("Error handling subscription updated event", e);
      webhookEvent.setStatus(WebhookEventStatus.FAILED);
      webhookEvent.setErrorMessage(e.getMessage());
      webhookEvent.setRetryCount(webhookEvent.getRetryCount() + 1);
      webhookEventRepository.save(webhookEvent);
      throw new PaymentException("Error handling subscription updated event", e);
    }
  }
}
