package com.iqscaffold.billingservice.subscription.webhook;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import com.iqscaffold.billingservice.subscription.InvoiceStatus;
import com.iqscaffold.billingservice.subscription.SubscriptionInvoice;
import com.iqscaffold.billingservice.subscription.SubscriptionInvoiceRepository;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles invoice webhook events from payment providers.
 * <p>
 * Processes invoice created, paid, payment failed, and voided events
 * to keep local invoice state in sync with payment providers.
 * <p>
 * Provider-specific data extraction is delegated to specialized services
 * to maintain separation of concerns and support multiple payment gateways.
 */
@Component
@Transactional
public class InvoiceWebhookEventHandler implements com.iqscaffold.billingservice.webhook.WebhookEventHandler {

  private static final Logger log = LoggerFactory.getLogger(InvoiceWebhookEventHandler.class);

  private final SubscriptionInvoiceRepository invoiceRepository;
  private final TenantSubscriptionRepository subscriptionRepository;
  private final StripeInvoiceDataExtractor stripeDataExtractor;

  public InvoiceWebhookEventHandler(
      final SubscriptionInvoiceRepository invoiceRepository,
      final TenantSubscriptionRepository subscriptionRepository,
      final StripeInvoiceDataExtractor stripeDataExtractor) {
    this.invoiceRepository = invoiceRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.stripeDataExtractor = stripeDataExtractor;
  }

  @Override
  public boolean supports(WebhookEvent event) {
    return event.isInvoiceEvent();
  }

  /**
   * Process an invoice webhook event.
   *
   * @param event The webhook event
   */
  @Override
  public void handleEvent(WebhookEvent event) {
    log.debug("Processing invoice webhook event: {} - {}", event.eventType(), event.resourceId());

    switch (event.eventType()) {
      case WebhookEvent.EventType.INVOICE_CREATED -> handleInvoiceCreated(event);
      case WebhookEvent.EventType.INVOICE_FINALIZED -> handleInvoiceFinalized(event);
      case WebhookEvent.EventType.INVOICE_PAID -> handleInvoicePaid(event);
      case WebhookEvent.EventType.INVOICE_PAYMENT_FAILED -> handleInvoicePaymentFailed(event);
      case WebhookEvent.EventType.INVOICE_VOIDED -> handleInvoiceVoided(event);
      default -> log.warn("Unhandled invoice event type: {}", event.eventType());
    }
  }

  private void handleInvoiceCreated(WebhookEvent event) {
    String invoiceId = event.resourceId();

    log.info("Processing invoice.created for: {}", invoiceId);

    // Check if invoice already exists
    var existing = findExistingInvoice(invoiceId, event.provider());
    if (existing.isPresent()) {
      log.info("Invoice already exists: {}", invoiceId);
      return;
    }

    // Extract invoice data using provider-specific extractor
    var invoiceData = extractInvoiceData(event);
    if (invoiceData == null) {
      log.warn("Failed to extract invoice data from event: {}", invoiceId);
      return;
    }

    // Extract provider-specific invoice object
    var providerInvoice = extractProviderInvoice(event);
    if (providerInvoice == null) {
      log.warn("Provider invoice object not found in metadata for: {}", invoiceId);
      return;
    }

    createOrUpdateInvoice(providerInvoice, invoiceData, event);
  }

  private void handleInvoiceFinalized(WebhookEvent event) {
    log.info("Processing invoice.finalized for: {}", event.resourceId());
    updateInvoiceStatus(event, InvoiceStatus.OPEN);
  }

  private void handleInvoicePaid(WebhookEvent event) {
    log.info("Processing invoice.paid for: {}", event.resourceId());
    updateInvoiceStatus(event, InvoiceStatus.PAID);

    // Mark payment timestamp
    var invoiceOpt = findExistingInvoice(event.resourceId(), event.provider());
    invoiceOpt.ifPresent(invoice -> {
      invoice.setPaidAt(java.time.Instant.now());
      invoiceRepository.save(invoice);
    });
  }

  private void handleInvoicePaymentFailed(WebhookEvent event) {
    log.info("Processing invoice.payment_failed for: {}", event.resourceId());
    // TODO: Send payment failed notification
    log.warn("Invoice payment failed: {}", event.resourceId());
  }

  private void handleInvoiceVoided(WebhookEvent event) {
    log.info("Processing invoice.voided for: {}", event.resourceId());
    updateInvoiceStatus(event, InvoiceStatus.VOID);
  }

  private void updateInvoiceStatus(WebhookEvent event, InvoiceStatus status) {
    String invoiceId = event.resourceId();

    var invoiceOpt = findExistingInvoice(invoiceId, event.provider());
    if (invoiceOpt.isEmpty()) {
      log.warn("Invoice not found for status update: {}", invoiceId);
      // Try to create it
      handleInvoiceCreated(event);
      return;
    }

    SubscriptionInvoice invoice = invoiceOpt.get();
    invoice.setStatus(status);
    invoiceRepository.save(invoice);

    log.info("Updated invoice {} status to: {}", invoiceId, status);
  }

  /**
   * Find existing invoice by provider-specific ID.
   */
  private java.util.Optional<SubscriptionInvoice> findExistingInvoice(String invoiceId, PaymentGatewayProvider provider) {
    return switch (provider) {
      case STRIPE -> invoiceRepository.findByStripeInvoiceId(invoiceId);
      // Add other providers as needed
      // case PAYPAL -> invoiceRepository.findByPaypalInvoiceId(invoiceId);
      default -> {
        log.warn("Unsupported provider for invoice lookup: {}", provider);
        yield java.util.Optional.empty();
      }
    };
  }

  /**
   * Extract invoice data using provider-specific extractor.
   */
  private StripeInvoiceDataExtractor.InvoiceData extractInvoiceData(WebhookEvent event) {
    return switch (event.provider()) {
      case STRIPE -> stripeDataExtractor.extractBasicData(event);
      // Add other providers as needed
      default -> {
        log.warn("Unsupported provider for invoice data extraction: {}", event.provider());
        yield null;
      }
    };
  }

  /**
   * Extract provider-specific invoice object from webhook event.
   */
  private Object extractProviderInvoice(WebhookEvent event) {
    return switch (event.provider()) {
      case STRIPE -> stripeDataExtractor.extractStripeInvoice(event);
      // Add other providers as needed
      default -> {
        log.warn("Unsupported provider for invoice object extraction: {}", event.provider());
        yield null;
      }
    };
  }

  /**
   * Create or update invoice using provider-specific data.
   */
  private void createOrUpdateInvoice(Object providerInvoice, StripeInvoiceDataExtractor.InvoiceData invoiceData, WebhookEvent event) {
    // Find tenant subscription
    if (invoiceData.subscriptionId() == null) {
      log.warn("Invoice has no subscription in metadata: {}", invoiceData.invoiceId());
      return;
    }

    var subscriptionOpt = findSubscriptionByProvider(invoiceData.subscriptionId(), event.provider());
    if (subscriptionOpt.isEmpty()) {
      log.warn("Subscription not found for invoice: {}", invoiceData.subscriptionId());
      return;
    }

    var tenantSubscription = subscriptionOpt.get();

    // Create or update invoice
    var invoiceOpt = findExistingInvoice(invoiceData.invoiceId(), event.provider());
    SubscriptionInvoice invoice = invoiceOpt.orElse(new SubscriptionInvoice());

    // Set basic invoice data
    invoice.setTenantSubscription(tenantSubscription);

    // Populate with provider-specific data
    populateInvoiceFromProvider(invoice, providerInvoice, event.provider());

    invoiceRepository.save(invoice);
    log.info("Created/updated local invoice record for: {}", invoiceData.invoiceId());
  }

  /**
   * Find subscription by provider-specific ID.
   */
  private java.util.Optional<com.iqscaffold.billingservice.subscription.TenantSubscription> findSubscriptionByProvider(String subscriptionId, PaymentGatewayProvider provider) {
    return switch (provider) {
      case STRIPE -> subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
      // Add other providers as needed
      default -> {
        log.warn("Unsupported provider for subscription lookup: {}", provider);
        yield java.util.Optional.empty();
      }
    };
  }

  /**
   * Populate invoice with provider-specific data.
   */
  private void populateInvoiceFromProvider(SubscriptionInvoice invoice, Object providerInvoice, PaymentGatewayProvider provider) {
    switch (provider) {
      case STRIPE -> {
        if (providerInvoice instanceof com.stripe.model.Invoice stripeInvoice) {
          stripeDataExtractor.populateFromStripeInvoice(invoice, stripeInvoice);
        }
      }
      // Add other providers as needed
      default -> log.warn("Unsupported provider for invoice population: {}", provider);
    }
  }
}
