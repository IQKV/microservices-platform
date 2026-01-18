package com.iqscaffold.billingservice.subscription.webhook;

import com.iqscaffold.billingservice.subscription.SubscriptionService;
import com.iqscaffold.billingservice.subscription.SubscriptionStateMachine;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import com.iqscaffold.billingservice.webhook.WebhookEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles subscription lifecycle webhook events from payment providers.
 * <p>
 * Processes events like subscription created, updated, canceled, and trial
 * ending
 * to keep local subscription state in sync with Stripe.
 */
@Component
@Transactional
public class SubscriptionWebhookEventHandler implements WebhookEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionWebhookEventHandler.class);

  private final TenantSubscriptionRepository subscriptionRepository;
  private final SubscriptionService subscriptionService;
  private final SubscriptionStateMachine subscriptionStateMachine;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  public SubscriptionWebhookEventHandler(
      final TenantSubscriptionRepository subscriptionRepository,
      final SubscriptionService subscriptionService,
      final SubscriptionStateMachine subscriptionStateMachine,
      final com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    this.subscriptionRepository = subscriptionRepository;
    this.subscriptionService = subscriptionService;
    this.subscriptionStateMachine = subscriptionStateMachine;
    this.objectMapper = objectMapper;
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
    logger.debug("Processing subscription webhook event: {} - {}", event.eventType(), event.resourceId());

    switch (event.eventType()) {
      case WebhookEvent.EventType.SUBSCRIPTION_CREATED -> handleSubscriptionCreated(event);
      case WebhookEvent.EventType.SUBSCRIPTION_UPDATED -> handleSubscriptionUpdated(event);
      case WebhookEvent.EventType.SUBSCRIPTION_CANCELED -> handleSubscriptionCanceled(event);
      case WebhookEvent.EventType.SUBSCRIPTION_TRIAL_ENDING -> handleSubscriptionTrialEnding(event);
      default -> logger.warn("Unhandled subscription event type: {}", event.eventType());
    }
  }

  private void handleSubscriptionCreated(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();
    String tenantId = event.tenantId().orElse(null);

    if (tenantId == null) {
      logger.warn("Cannot process subscription.created without tenant_id: {}", stripeSubscriptionId);
      return;
    }

    logger.info("Processing subscription.created for tenant: {}, subscription: {}", tenantId, stripeSubscriptionId);

    // Check if subscription already exists
    var existing = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (existing.isPresent()) {
      logger.info("Subscription already exists: {}", stripeSubscriptionId);
      return;
    }

    // Extract subscription details from metadata
    String status = event.getMetadataString("status").orElse("active");
    String customerId = event.getMetadataString("customer").orElse(null);

    // Create local subscription record (no tenant_id needed - schema provides
    // context)
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
    logger.info("Created local subscription record for: {}", stripeSubscriptionId);
  }

  private void handleSubscriptionUpdated(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();

    logger.info("Processing subscription.updated for: {}", stripeSubscriptionId);

    var subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOpt.isEmpty()) {
      logger.warn("Subscription not found for update: {}", stripeSubscriptionId);
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
    logger.info("Updated local subscription record for: {}", stripeSubscriptionId);
  }

  private void handleSubscriptionCanceled(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();

    logger.info("Processing subscription.canceled for: {}", stripeSubscriptionId);

    var subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOpt.isEmpty()) {
      logger.warn("Received subscription deleted event for {} but no local subscription found", stripeSubscriptionId);
      return;
    }

    TenantSubscription subscription = subscriptionOpt.get();
    subscription.setStatus(SubscriptionStatus.CANCELED);
    subscription.setCanceledAt(java.time.Instant.now());

    subscriptionRepository.save(subscription);
    logger.info("Canceled local subscription record for: {}", stripeSubscriptionId);
  }

  private void handleSubscriptionTrialEnding(WebhookEvent event) {
    String stripeSubscriptionId = event.resourceId();

    logger.info("Processing subscription.trial_ending for: {}", stripeSubscriptionId);

    var subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOpt.isEmpty()) {
      logger.warn("Subscription not found for trial ending notification: {}", stripeSubscriptionId);
      return;
    }

    // TODO: Send notification email to tenant about trial ending
    logger.info("Handling trial will end event for subscription: {}", stripeSubscriptionId);
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
        logger.warn("Unknown Stripe status: {}, defaulting to ACTIVE", stripeStatus);
        yield SubscriptionStatus.ACTIVE;
      }
    };
  }

  /**
   * Populate subscription entity from Stripe subscription object.
   * Note: Detailed field extraction skipped due to Stripe SDK version
   * compatibility.
   * Main subscription status and customer info are still populated.
   */
  private void populateFromStripeSubscription(TenantSubscription subscription,
                                              com.stripe.model.Subscription stripeSubscription) {
    // Status and customer are already set from metadata

    // Use Jackson to parse raw JSON as standard getters (getCurrentPeriodEnd/Start)
    // are accessible via toJson() string in this SDK version
    try {
      String jsonString = stripeSubscription.toJson();
      com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(jsonString);

      if (root.has("current_period_start") && !root.get("current_period_start").isNull()) {
        subscription.setCurrentPeriodStart(java.time.Instant.ofEpochSecond(root.get("current_period_start").asLong()));
      }

      if (root.has("current_period_end") && !root.get("current_period_end").isNull()) {
        subscription.setCurrentPeriodEnd(java.time.Instant.ofEpochSecond(root.get("current_period_end").asLong()));
      }
    } catch (final Exception e) {
      logger.error("Failed to parse Stripe subscription JSON for dates", e);
    }

    if (stripeSubscription.getTrialEnd() != null) {
      subscription.setTrialEnd(java.time.Instant.ofEpochSecond(stripeSubscription.getTrialEnd()));
    }

    if (stripeSubscription.getCancelAt() != null) {
      subscription.setCancelAt(java.time.Instant.ofEpochSecond(stripeSubscription.getCancelAt()));
    }

    if (stripeSubscription.getCanceledAt() != null) {
      subscription.setCanceledAt(java.time.Instant.ofEpochSecond(stripeSubscription.getCanceledAt()));
    }

    // Also map cancellation reason/details if available
    if (stripeSubscription.getCancellationDetails() != null
        && stripeSubscription.getCancellationDetails().getReason() != null) {
      subscription.setCancellationReason(stripeSubscription.getCancellationDetails().getReason());
    }
  }
}
