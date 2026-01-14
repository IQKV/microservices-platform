package com.iqscaffold.billingservice.subscription.webhook;

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
 * to keep local invoice state in sync with Stripe.
 */
@Component
@Transactional
public class InvoiceWebhookEventHandler implements com.iqscaffold.billingservice.webhook.WebhookEventHandler {

  private static final Logger log = LoggerFactory.getLogger(InvoiceWebhookEventHandler.class);

  private final SubscriptionInvoiceRepository invoiceRepository;
  private final TenantSubscriptionRepository subscriptionRepository;

  public InvoiceWebhookEventHandler(
      final SubscriptionInvoiceRepository invoiceRepository,
      final TenantSubscriptionRepository subscriptionRepository) {
    this.invoiceRepository = invoiceRepository;
    this.subscriptionRepository = subscriptionRepository;
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
    String stripeInvoiceId = event.resourceId();

    log.info("Processing invoice.created for: {}", stripeInvoiceId);

    // Check if invoice already exists
    var existing = invoiceRepository.findByStripeInvoiceId(stripeInvoiceId);
    if (existing.isPresent()) {
      log.info("Invoice already exists: {}", stripeInvoiceId);
      return;
    }

    // Extract invoice from metadata
    Object rawInvoice = event.metadata().get("invoice");
    if (!(rawInvoice instanceof com.stripe.model.Invoice stripeInvoice)) {
      log.warn("Invoice object not found in metadata for: {}", stripeInvoiceId);
      return;
    }

    createOrUpdateInvoice(stripeInvoice, event);
  }

  private void handleInvoiceFinalized(WebhookEvent event) {
    log.info("Processing invoice.finalized for: {}", event.resourceId());
    updateInvoiceStatus(event, InvoiceStatus.OPEN);
  }

  private void handleInvoicePaid(WebhookEvent event) {
    log.info("Processing invoice.paid for: {}", event.resourceId());
    updateInvoiceStatus(event, InvoiceStatus.PAID);

    // Mark payment timestamp
    var invoiceOpt = invoiceRepository.findByStripeInvoiceId(event.resourceId());
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
    String stripeInvoiceId = event.resourceId();

    var invoiceOpt = invoiceRepository.findByStripeInvoiceId(stripeInvoiceId);
    if (invoiceOpt.isEmpty()) {
      log.warn("Invoice not found for status update: {}", stripeInvoiceId);
      // Try to create it
      handleInvoiceCreated(event);
      return;
    }

    SubscriptionInvoice invoice = invoiceOpt.get();
    invoice.setStatus(status);
    invoiceRepository.save(invoice);

    log.info("Updated invoice {} status to: {}", stripeInvoiceId, status);
  }

  private void createOrUpdateInvoice(com.stripe.model.Invoice stripeInvoice, WebhookEvent event) {
    String stripeInvoiceId = stripeInvoice.getId();

    // Find tenant subscription - extract from metadata or event
    String stripeSubscriptionId = event.getMetadataString("subscription").orElse(null);
    if (stripeSubscriptionId == null) {
      log.warn("Invoice has no subscription in metadata: {}", stripeInvoiceId);
      return;
    }

    var subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOpt.isEmpty()) {
      log.warn("Subscription not found for invoice: {}", stripeSubscriptionId);
      return;
    }

    var tenantSubscription = subscriptionOpt.get();

    // Create or update invoice
    var invoiceOpt = invoiceRepository.findByStripeInvoiceId(stripeInvoiceId);
    SubscriptionInvoice invoice = invoiceOpt.orElse(new SubscriptionInvoice());

    invoice.setStripeInvoiceId(stripeInvoiceId);
    invoice.setTenantSubscription(tenantSubscription);
    // No need to set tenant_id - schema provides context
    invoice.setInvoiceNumber(stripeInvoice.getNumber());

    // Set amounts
    if (stripeInvoice.getAmountDue() != null) {
      invoice.setAmountDue(java.math.BigDecimal.valueOf(stripeInvoice.getAmountDue()).divide(java.math.BigDecimal.valueOf(100)));
    }
    if (stripeInvoice.getAmountPaid() != null) {
      invoice.setAmountPaid(java.math.BigDecimal.valueOf(stripeInvoice.getAmountPaid()).divide(java.math.BigDecimal.valueOf(100)));
    }

    invoice.setCurrency(stripeInvoice.getCurrency().toUpperCase());
    invoice.setStatus(mapStripeStatusToLocal(stripeInvoice.getStatus()));

    // Set URLs
    invoice.setHostedInvoiceUrl(stripeInvoice.getHostedInvoiceUrl());
    invoice.setInvoicePdfUrl(stripeInvoice.getInvoicePdf());

    // Set due date if available
    if (stripeInvoice.getDueDate() != null) {
      invoice.setDueDate(java.time.Instant.ofEpochSecond(stripeInvoice.getDueDate()));
    }

    invoiceRepository.save(invoice);
    log.info("Created/updated local invoice record for: {}", stripeInvoiceId);
  }

  private InvoiceStatus mapStripeStatusToLocal(String stripeStatus) {
    return switch (stripeStatus.toLowerCase()) {
      case "draft" -> InvoiceStatus.DRAFT;
      case "open" -> InvoiceStatus.OPEN;
      case "paid" -> InvoiceStatus.PAID;
      case "void" -> InvoiceStatus.VOID;
      case "uncollectible" -> InvoiceStatus.UNCOLLECTIBLE;
      default -> {
        log.warn("Unknown Stripe invoice status: {}, defaulting to OPEN", stripeStatus);
        yield InvoiceStatus.OPEN;
      }
    };
  }
}
