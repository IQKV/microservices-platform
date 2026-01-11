package com.iqscaffold.billingservice.webhook;

import com.iqscaffold.billingservice.payout.PayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles payout-related webhook events from all payment providers.
 * <p>
 * This handler processes normalized payout events (paid, failed, canceled)
 * and updates the local payout state accordingly. It works with the abstract
 * {@link WebhookEvent} structure, making it provider-independent.
 * <p>
 * Supported Event Types:
 * <ul>
 *   <li>{@link WebhookEvent.EventType#PAYOUT_PAID} - Payout successfully sent to bank account</li>
 *   <li>{@link WebhookEvent.EventType#PAYOUT_FAILED} - Payout failed (e.g., invalid bank details)</li>
 * </ul>
 * <p>
 * Note: The actual payout processing logic delegates to the provider-specific
 * implementation since different providers may have different payout data structures.
 * The handler extracts the raw provider payload and passes it to the appropriate service.
 */
@Component
public class PayoutWebhookEventHandler implements WebhookEventHandler {
  private static final Logger logger = LoggerFactory.getLogger(PayoutWebhookEventHandler.class);

  private final PayoutService payoutService;

  public PayoutWebhookEventHandler(final PayoutService payoutService) {
    this.payoutService = payoutService;
  }

  @Override
  public boolean supports(WebhookEvent event) {
    return event.isPayoutEvent();
  }

  @Override
  public void handleEvent(WebhookEvent event) {
    if (!supports(event)) {
      throw new IllegalArgumentException("Event type not supported by PayoutWebhookEventHandler: " + event.eventType());
    }

    logger.info("Processing payout webhook event: type={}, resourceId={}, provider={}",
        event.eventType(), event.resourceId(), event.provider());

    switch (event.eventType()) {
      case WebhookEvent.EventType.PAYOUT_PAID -> handlePayoutPaid(event);
      case WebhookEvent.EventType.PAYOUT_FAILED -> handlePayoutFailed(event);
      default -> logger.warn("Unhandled payout event type: {}", event.eventType());
    }
  }

  private void handlePayoutPaid(WebhookEvent event) {
    // Delegate to PayoutService which handles provider-specific payout objects
    // For Stripe, the raw payload will be a com.stripe.model.Payout
    Object rawPayload = event.rawPayload();
    
    if (rawPayload instanceof com.stripe.model.Payout stripePayout) {
      payoutService.processPayout(stripePayout);
      logger.info("Processed Stripe payout: payoutId={}, amount={}, currency={}",
          stripePayout.getId(), stripePayout.getAmount(), stripePayout.getCurrency());
    } else {
      // For other providers, add handling here
      logger.warn("Unsupported payout payload type: {}", rawPayload.getClass().getName());
    }
  }

  private void handlePayoutFailed(WebhookEvent event) {
    logger.warn("Payout failed: payoutId={}, provider={}", event.resourceId(), event.provider());
    // TODO: Add failure handling logic when needed
    // This could involve updating payout status, sending notifications, etc.
  }
}
