package com.iqscaffold.billingservice.webhook;

import com.iqscaffold.billingservice.payment.PaymentService;
import com.iqscaffold.billingservice.shared.BillingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles payment-related webhook events from all payment providers.
 * <p>
 * This handler processes normalized payment events (succeeded, failed, refunded, etc.)
 * and updates the local payment state accordingly. It works with the abstract
 * {@link WebhookEvent} structure, making it provider-independent.
 * <p>
 * Supported Event Types:
 * <ul>
 *   <li>{@link WebhookEvent.EventType#PAYMENT_SUCCEEDED} - Payment completed successfully</li>
 *   <li>{@link WebhookEvent.EventType#PAYMENT_FAILED} - Payment failed or was declined</li>
 *   <li>{@link WebhookEvent.EventType#PAYMENT_REFUNDED} - Full refund processed</li>
 *   <li>{@link WebhookEvent.EventType#PAYMENT_PARTIALLY_REFUNDED} - Partial refund processed</li>
 * </ul>
 */
@Component
public class PaymentWebhookEventHandler implements WebhookEventHandler {
  private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookEventHandler.class);

  private final PaymentService paymentService;

  public PaymentWebhookEventHandler(final PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @Override
  public boolean supports(WebhookEvent event) {
    return event.isPaymentEvent();
  }

  @Override
  public void handleEvent(WebhookEvent event) {
    if (!supports(event)) {
      throw new IllegalArgumentException("Event type not supported by PaymentWebhookEventHandler: " + event.eventType());
    }

    logger.info("Processing payment webhook event: type={}, resourceId={}, provider={}",
        event.eventType(), event.resourceId(), event.provider());

    switch (event.eventType()) {
      case WebhookEvent.EventType.PAYMENT_SUCCEEDED -> handlePaymentSuccess(event);
      case WebhookEvent.EventType.PAYMENT_FAILED -> handlePaymentFailure(event);
      case WebhookEvent.EventType.PAYMENT_REFUNDED -> handleRefund(event, true);
      case WebhookEvent.EventType.PAYMENT_PARTIALLY_REFUNDED -> handleRefund(event, false);
      default -> logger.warn("Unhandled payment event type: {}", event.eventType());
    }
  }

  private void handlePaymentSuccess(WebhookEvent event) {
    String paymentIntentId = event.resourceId();
    paymentService.updateStatus(paymentIntentId, BillingConstants.PaymentStatus.SUCCEEDED);
    logger.info("Updated payment status to SUCCEEDED: paymentIntentId={}, provider={}",
        paymentIntentId, event.provider());
  }

  private void handlePaymentFailure(WebhookEvent event) {
    String paymentIntentId = event.resourceId();
    paymentService.updateStatus(paymentIntentId, BillingConstants.PaymentStatus.FAILED);
    logger.info("Updated payment status to FAILED: paymentIntentId={}, provider={}",
        paymentIntentId, event.provider());
  }

  private void handleRefund(WebhookEvent event, boolean fullRefund) {
    String paymentIntentId = event.resourceId();
    
    // Check if this is a full or partial refund from metadata
    boolean isFullyRefunded = event.getMetadataBoolean("refunded").orElse(fullRefund);
    
    String status = isFullyRefunded
        ? BillingConstants.PaymentStatus.REFUNDED 
        : BillingConstants.PaymentStatus.PARTIALLY_REFUNDED;
    
    paymentService.updateStatus(paymentIntentId, status);
    logger.info("Updated payment status to {}: paymentIntentId={}, provider={}",
        status, paymentIntentId, event.provider());
  }
}
