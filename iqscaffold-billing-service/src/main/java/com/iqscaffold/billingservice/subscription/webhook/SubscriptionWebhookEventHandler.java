package com.iqscaffold.billingservice.subscription.webhook;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
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
 * ending to keep local subscription state in sync with payment providers.
 * <p>
 * Provider-specific data extraction is delegated to specialized services
 * to maintain separation of concerns and support multiple payment gateways.
 */
@Component
@Transactional
public class SubscriptionWebhookEventHandler implements WebhookEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionWebhookEventHandler.class);

  private final TenantSubscriptionRepository subscriptionRepository;
  private final SubscriptionService subscriptionService;
  private final SubscriptionStateMachine subscriptionStateMachine;
  private final StripeSubscriptionDataExtractor stripeDataExtractor;

  public SubscriptionWebhookEventHandler(
      final TenantSubscriptionRepository subscriptionRepository,
      final SubscriptionService subscriptionService,
      final SubscriptionStateMachine subscriptionStateMachine,
      final StripeSubscriptionDataExtractor stripeDataExtractor) {
    this.subscriptionRepository = subscriptionRepository;
    this.subscriptionService = subscriptionService;
    this.subscriptionStateMachine = subscriptionStateMachine;
    this.stripeDataExtractor = stripeDataExtractor;
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
    String subscriptionId = event.resourceId();
    String tenantId = event.tenantId().orElse(null);

    if (tenantId == null) {
      logger.warn("Cannot process subscription.created without tenant_id: {}", subscriptionId);
      return;
    }

    logger.info("Processing subscription.created for tenant: {}, subscription: {}", tenantId, subscriptionId);

    // Check if subscription already exists
    var existing = findExistingSubscription(subscriptionId, event.provider());
    if (existing.isPresent()) {
      logger.info("Subscription already exists: {}", subscriptionId);
      return;
    }

    // Extract subscription data using provider-specific extractor
    var subscriptionData = extractSubscriptionData(event);
    if (subscriptionData == null) {
      logger.warn("Failed to extract subscription data from event: {}", subscriptionId);
      return;
    }

    // Create local subscription record
    TenantSubscription subscription = createSubscriptionEntity(subscriptionData, event);

    // Populate with detailed provider-specific data
    populateDetailedSubscriptionData(subscription, event);

    subscriptionRepository.save(subscription);
    logger.info("Created local subscription record for: {}", subscriptionId);
  }

  private void handleSubscriptionUpdated(WebhookEvent event) {
    String subscriptionId = event.resourceId();

    logger.info("Processing subscription.updated for: {}", subscriptionId);

    var subscriptionOpt = findExistingSubscription(subscriptionId, event.provider());
    if (subscriptionOpt.isEmpty()) {
      logger.warn("Subscription not found for update: {}", subscriptionId);
      // Try to create it
      handleSubscriptionCreated(event);
      return;
    }

    TenantSubscription subscription = subscriptionOpt.get();

    // Update basic subscription data
    var subscriptionData = extractSubscriptionData(event);
    if (subscriptionData != null) {
      updateSubscriptionFromData(subscription, subscriptionData);
    }

    // Update detailed provider-specific data
    populateDetailedSubscriptionData(subscription, event);

    subscriptionRepository.save(subscription);
    logger.info("Updated local subscription record for: {}", subscriptionId);
  }

  private void handleSubscriptionCanceled(WebhookEvent event) {
    String subscriptionId = event.resourceId();

    logger.info("Processing subscription.canceled for: {}", subscriptionId);

    var subscriptionOpt = findExistingSubscription(subscriptionId, event.provider());
    if (subscriptionOpt.isEmpty()) {
      logger.warn("Received subscription deleted event for {} but no local subscription found", subscriptionId);
      return;
    }

    TenantSubscription subscription = subscriptionOpt.get();
    subscription.setStatus(SubscriptionStatus.CANCELED);
    subscription.setCanceledAt(java.time.Instant.now());

    // Update with any additional cancellation details from provider
    populateDetailedSubscriptionData(subscription, event);

    subscriptionRepository.save(subscription);
    logger.info("Canceled local subscription record for: {}", subscriptionId);
  }

  private void handleSubscriptionTrialEnding(WebhookEvent event) {
    String subscriptionId = event.resourceId();

    logger.info("Processing subscription.trial_ending for: {}", subscriptionId);

    var subscriptionOpt = findExistingSubscription(subscriptionId, event.provider());
    if (subscriptionOpt.isEmpty()) {
      logger.warn("Subscription not found for trial ending notification: {}", subscriptionId);
      return;
    }

    // TODO: Send notification email to tenant about trial ending
    logger.info("Handling trial will end event for subscription: {}", subscriptionId);
  }

  /**
   * Find existing subscription by provider-specific ID.
   */
  private java.util.Optional<TenantSubscription> findExistingSubscription(String subscriptionId, PaymentGatewayProvider provider) {
    return switch (provider) {
      case STRIPE -> subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
      // Add other providers as needed
      // case PAYPAL -> subscriptionRepository.findByPaypalSubscriptionId(subscriptionId);
      default -> {
        logger.warn("Unsupported provider for subscription lookup: {}", provider);
        yield java.util.Optional.empty();
      }
    };
  }

  /**
   * Extract subscription data using provider-specific extractor.
   */
  private StripeSubscriptionDataExtractor.SubscriptionData extractSubscriptionData(WebhookEvent event) {
    return switch (event.provider()) {
      case STRIPE -> stripeDataExtractor.extractBasicData(event);
      // Add other providers as needed
      default -> {
        logger.warn("Unsupported provider for data extraction: {}", event.provider());
        yield null;
      }
    };
  }

  /**
   * Create subscription entity from extracted data.
   */
  private TenantSubscription createSubscriptionEntity(StripeSubscriptionDataExtractor.SubscriptionData data, WebhookEvent event) {
    TenantSubscription subscription = new TenantSubscription();

    // Set provider-specific fields based on the provider
    switch (event.provider()) {
      case STRIPE -> {
        subscription.setStripeSubscriptionId(data.subscriptionId());
        subscription.setStripeCustomerId(data.customerId());
      }
      // Add other providers as needed
      default -> logger.warn("Unsupported provider for subscription creation: {}", event.provider());
    }

    subscription.setStatus(data.status());
    return subscription;
  }

  /**
   * Update subscription entity from extracted data.
   */
  private void updateSubscriptionFromData(TenantSubscription subscription, StripeSubscriptionDataExtractor.SubscriptionData data) {
    subscription.setStatus(data.status());

    // Update provider-specific fields if needed
    if (data.customerId() != null) {
      // Determine provider based on existing subscription data
      if (subscription.getStripeSubscriptionId() != null) {
        subscription.setStripeCustomerId(data.customerId());
      }
      // Add other provider checks as needed
    }
  }

  /**
   * Populate detailed subscription data using provider-specific extractor.
   */
  private void populateDetailedSubscriptionData(TenantSubscription subscription, WebhookEvent event) {
    switch (event.provider()) {
      case STRIPE -> stripeDataExtractor.populateFromStripeSubscription(subscription, event);
      // Add other providers as needed
      default -> logger.debug("No detailed data extraction available for provider: {}", event.provider());
    }
  }
}
