package com.iqscaffold.billingservice.subscription.webhook;

import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles subscription lifecycle webhook events from payment providers.
 * <p>
 * Processes events like subscription created, updated, canceled, and trial ending
 * to keep local subscription state in sync with Stripe.
 */
@Component
@Transactional
public class SubscriptionWebhookEventHandler implements com.iqscaffold.billingservice.webhook.WebhookEventHandler {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionWebhookEventHandler.class);

  private final TenantSubscriptionRepository subscriptionRepository;

  public SubscriptionWebhookEventHandler(final TenantSubscriptionRepository subscriptionRepository) {
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public boolean supports(WebhookEvent event) {
    return event.isSubscriptionEvent();
  }

  /**
   * Process a subscription webhook event.
   *
   * @param event The webhook event
   */
  @Override
  public void handleEvent(WebhookEvent event) {
    log.debug("Processing subscription webhook event: {} - {}", event.eventType(), event.resourceId());

    switch (event.eventType()) {
      case WebhookEvent.EventType.SUBSCRIPTION_CREATED -> handleSubscriptionCreated(event);
      case WebhookEvent.EventType.SUBSCRIPTION_UPDATED -> handleSubscriptionUpdated(event);
      case WebhookEvent.EventType.SUBSCRIPTION_CANCELED -> handleSubscriptionCanceled(event);
      case WebhookEvent.EventType.SUBSCRIPTION_TRIAL_ENDING -> handleSubscriptionTrialEnding(event);
      default -> log.warn("Unhandled subscription event type: {}", event.eventType());
    }
  }

  private void handleSubscriptionCreated(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();
    String tenantId = event.tenantId().orElse(null);

    if (tenantId == null) {
      log.warn("Cannot process subscription.created without tenant_id: {}", stripeSubscriptionId);
      return;
    }

    log.info("Processing subscription.created for tenant: {}, subscription: {}", tenantId, stripeSubscriptionId);

    // Check if subscription already exists
    var existing = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (existing.isPresent()) {
      log.info("Subscription already exists: {}", stripeSubscriptionId);
      return;
    }

    // Extract subscription details from metadata
    String status = event.getMetadataString("status").orElse("active");
    String customerId = event.getMetadataString("customer").orElse(null);

    // Create local subscription record (no tenant_id needed - schema provides context)
    TenantSubscription subscription = new TenantSubscription();
    subscription.setStripeSubscriptionId(stripeSubscriptionId);
    subscription.setStripeCustomerId(customerId);
    subscription.setStatus(mapStripeStatusToLocal(status));

    // Get the Stripe subscription object from metadata if available
    Object rawSubscription = event.metadata().get("subscription");
    if (rawSubscription instanceof com.stripe.model.Subscription stripeSubscription) {
      populateFromStripeSubscription(subscription, stripeSubscription);
    }

    subscriptionRepository.save(subscription);
    log.info("Created local subscription record for: {}", stripeSubscriptionId);
  }

  private void handleSubscriptionUpdated(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();

    log.info("Processing subscription.updated for: {}", stripeSubscriptionId);

    var subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOpt.isEmpty()) {
      log.warn("Subscription not found for update: {}", stripeSubscriptionId);
      // Try to create it
      handleSubscriptionCreated(event);
      return;
    }

    TenantSubscription subscription = subscriptionOpt.get();
    String status = event.getMetadataString("status").orElse(null);

    if (status != null) {
      subscription.setStatus(mapStripeStatusToLocal(status));
    }

    // Get the Stripe subscription object from metadata if available
    Object rawSubscription = event.metadata().get("subscription");
    if (rawSubscription instanceof com.stripe.model.Subscription stripeSubscription) {
      populateFromStripeSubscription(subscription, stripeSubscription);
    }

    subscriptionRepository.save(subscription);
    log.info("Updated local subscription record for: {}", stripeSubscriptionId);
  }

  private void handleSubscriptionCanceled(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();

    log.info("Processing subscription.canceled for: {}", stripeSubscriptionId);

    var subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOpt.isEmpty()) {
      log.warn("Subscription not found for cancellation: {}", stripeSubscriptionId);
      return;
    }

    TenantSubscription subscription = subscriptionOpt.get();
    subscription.setStatus(SubscriptionStatus.CANCELED);
    subscription.setCanceledAt(java.time.Instant.now());

    subscriptionRepository.save(subscription);
    log.info("Canceled local subscription record for: {}", stripeSubscriptionId);
  }

  private void handleSubscriptionTrialEnding(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();

    log.info("Processing subscription.trial_ending for: {}", stripeSubscriptionId);

    var subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOpt.isEmpty()) {
      log.warn("Subscription not found for trial ending notification: {}", stripeSubscriptionId);
      return;
    }

    // TODO: Send notification email to tenant about trial ending
    log.info("Trial ending soon for subscription: {}", stripeSubscriptionId);
  }

  /**
   * Map Stripe subscription status to local status enum.
   */
  private SubscriptionStatus mapStripeStatusToLocal(String stripeStatus) {
    return switch (stripeStatus.toLowerCase()) {
      case "incomplete" -> SubscriptionStatus.INCOMPLETE;
      case "trialing" -> SubscriptionStatus.TRIALING;
      case "active" -> SubscriptionStatus.ACTIVE;
      case "past_due" -> SubscriptionStatus.PAST_DUE;
      case "canceled" -> SubscriptionStatus.CANCELED;
      case "unpaid" -> SubscriptionStatus.UNPAID;
      case "paused" -> SubscriptionStatus.PAUSED;
      default -> {
        log.warn("Unknown Stripe status: {}, defaulting to ACTIVE", stripeStatus);
        yield SubscriptionStatus.ACTIVE;
      }
    };
  }

  /**
   * Populate subscription entity from Stripe subscription object.
   * Note: Detailed field extraction skipped due to Stripe SDK version compatibility.
   * Main subscription status and customer info are still populated.
   */
  private void populateFromStripeSubscription(TenantSubscription subscription, com.stripe.model.Subscription stripeSubscription) {
    // Status and customer are already set from metadata
    // Additional fields would be extracted here if needed
    // For now, webhook metadata contains the essential information
  }
}
