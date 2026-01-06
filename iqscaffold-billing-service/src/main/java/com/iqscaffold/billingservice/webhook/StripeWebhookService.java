package com.iqscaffold.billingservice.webhook;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import com.iqscaffold.billingservice.payment.PaymentService;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles incoming webhooks from Stripe to synchronize local state with gateway
 * state.
 * <p>
 * Key Security Features:
 * <ul>
 * <li>Validates the {@code Stripe-Signature} header using the configured
 * webhook secret.</li>
 * <li>Throws specific exceptions for invalid signatures to return 400 Bad
 * Request to Stripe.</li>
 * </ul>
 * <p>
 * Supported Events:
 * <ul>
 * <li>{@code payment_intent.succeeded}, {@code payment_intent.payment_failed}:
 * Updates payment status.</li>
 * <li>{@code charge.refunded}: Syncs refund status (placeholder logic).</li>
 * <li>{@code payout.paid}: Records Payout entities via
 * {@link com.iqscaffold.billingservice.payout.PayoutService}.</li>
 * <li>{@code account.updated}: Updates local
 * {@link com.iqscaffold.billingservice.admin.MerchantStripeConfig}
 * capabilities.</li>
 * </ul>
 */
@Service
public class StripeWebhookService {
  private static final Logger logger = LoggerFactory.getLogger(StripeWebhookService.class);

  private final PaymentService paymentService;
  private final com.iqscaffold.billingservice.payout.PayoutService payoutService;
  private final com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository merchantConfigRepository;
  private final IqScaffoldProperties iqScaffoldProperties;

  public StripeWebhookService(
      PaymentService paymentService,
      com.iqscaffold.billingservice.payout.PayoutService payoutService,
      com.iqscaffold.billingservice.admin.MerchantStripeConfigRepository merchantConfigRepository,
      IqScaffoldProperties iqScaffoldProperties) {
    this.paymentService = paymentService;
    this.payoutService = payoutService;
    this.merchantConfigRepository = merchantConfigRepository;
    this.iqScaffoldProperties = iqScaffoldProperties;
  }

  /**
   * Main entry point for processing webhook payloads.
   * <p>
   * 1. Constructs the Stripe {@link Event} object, verifying the cryptographic
   * signature.
   * 2. Dispatches the event to the appropriate handler based on
   * {@code event.getType()}.
   * <p>
   * Note: This method is designed to be idempotent-safe. Repeated calls for the
   * same event
   * should generally result in the same outcome (e.g., updating status to
   * 'SUCCEEDED' is safe to do twice).
   *
   * @param payload   The raw JSON payload from the request body.
   * @param sigHeader The {@code Stripe-Signature} header value.
   * @throws IllegalArgumentException If signature verification fails.
   */
  public void processWebhook(String payload, String sigHeader) {
    Event event;
    try {
      event = Webhook.constructEvent(
          payload, sigHeader, iqScaffoldProperties.billing().payment().stripe().webhookSecret());
    } catch (SignatureVerificationException e) {
      logger.error("Invalid signature for webhook", e);
      throw new IllegalArgumentException("Invalid signature");
    } catch (Exception e) {
      logger.error("Webhook parsing failed", e);
      throw new IllegalArgumentException("Webhook parsing failed");
    }

    // Resolve tenant from event and set context
    resolveAndSetTenant(event);

    try {
      // Handle the event
      switch (event.getType()) {
        case "payment_intent.succeeded" -> handlePaymentSuccess(event);
        case "payment_intent.payment_failed" -> handlePaymentFailure(event);
        case "charge.refunded" -> handleRefund(event);
        case "payout.paid" -> handlePayout(event);
        case "account.updated" -> handleAccountUpdated(event);
        default -> logger.debug("Unhandled event type: {}", event.getType());
      }
    } finally {
      com.iqscaffold.billingservice.tenancy.TenantContext.clear();
    }
  }

  private void resolveAndSetTenant(Event event) {
    String tenantId = null;
    var dataObject = event.getDataObjectDeserializer().getObject().orElse(null);

    if (dataObject instanceof PaymentIntent pi) {
      tenantId = pi.getMetadata().get("tenant_id");
    } else if (dataObject instanceof com.stripe.model.Charge charge) {
      tenantId = charge.getMetadata().get("tenant_id");
      if (tenantId == null && charge.getPaymentIntent() != null) {
        // Try to get from PI if available in metadata (unlikely to be auto-expanded
        // here)
      }
    } else if (dataObject instanceof com.stripe.model.Account account) {
      // For account events, we might need to look up by account ID
      var config = merchantConfigRepository.findByStripeAccountId(account.getId());
      tenantId = config.map(com.iqscaffold.billingservice.admin.MerchantStripeConfig::getTenantId).orElse(null);
    }

    if (tenantId != null) {
      com.iqscaffold.billingservice.tenancy.TenantContext.setCurrentTenantId(tenantId);
      logger.debug("Resolved tenant {} from event {}", tenantId, event.getType());
    } else if (!isGlobalEvent(event.getType())) {
      logger.error("Rejecting webhook: Could not resolve tenant for event {}", event.getType());
      throw new IllegalArgumentException("Tenant resolution failed for event: " + event.getType());
    } else {
      logger.debug("Processing global event: {}", event.getType());
    }
  }

  private boolean isGlobalEvent(String eventType) {
    return "account.updated".equals(eventType);
  }

  private void handlePaymentSuccess(Event event) {
    PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
    if (intent != null) {
      paymentService.updateStatus(intent.getId(), BillingConstants.PaymentStatus.SUCCEEDED);
    }
  }

  private void handlePaymentFailure(Event event) {
    PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
    if (intent != null) {
      paymentService.updateStatus(intent.getId(), BillingConstants.PaymentStatus.FAILED);
    }
  }

  private void handleRefund(Event event) {
    com.stripe.model.Charge charge = (com.stripe.model.Charge) event.getDataObjectDeserializer().getObject()
        .orElse(null);
    if (charge != null && charge.getPaymentIntent() != null) {
      String status = charge.getRefunded() ? BillingConstants.PaymentStatus.REFUNDED
          : BillingConstants.PaymentStatus.PARTIALLY_REFUNDED;
      paymentService.updateStatus(charge.getPaymentIntent(), status);
      logger.info("Syncing refund status via webhook for PI: {} | Status: {}", charge.getPaymentIntent(), status);
    }
  }

  private void handlePayout(Event event) {
    com.stripe.model.Payout payout = (com.stripe.model.Payout) event.getDataObjectDeserializer().getObject()
        .orElse(null);
    if (payout != null) {
      payoutService.processPayout(payout);
    }
  }

  private void handleAccountUpdated(Event event) {
    com.stripe.model.Account account = (com.stripe.model.Account) event.getDataObjectDeserializer().getObject()
        .orElse(null);
    if (account != null) {
      String accountId = account.getId();
      // Find config by stripe account id
      var config = merchantConfigRepository.findByStripeAccountId(accountId);
      if (config.isPresent()) {
        var merchant = config.get();
        merchant.setChargesEnabled(Boolean.TRUE.equals(account.getChargesEnabled()));
        merchant.setPayoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()));
        merchantConfigRepository.save(merchant);
        logger.info("Updated merchant capabilities for account: {}", accountId);
      } else {
        logger.warn("Received account update for unknown account: {}", accountId);
      }
    }
  }
}
