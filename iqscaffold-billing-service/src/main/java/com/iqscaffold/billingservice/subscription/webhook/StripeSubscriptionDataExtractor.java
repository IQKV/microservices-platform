package com.iqscaffold.billingservice.subscription.webhook;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for extracting and mapping Stripe-specific subscription data.
 * <p>
 * This service isolates all Stripe-specific logic from the main webhook handler,
 * making it easier to support multiple payment providers in the future.
 */
@Service
public class StripeSubscriptionDataExtractor {

  private static final Logger logger = LoggerFactory.getLogger(StripeSubscriptionDataExtractor.class);

  private final ObjectMapper objectMapper;

  public StripeSubscriptionDataExtractor(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Extract basic subscription data from webhook event metadata.
   */
  public SubscriptionData extractBasicData(WebhookEvent event) {
    String status = event.getMetadataString("status").orElse("active");
    String customerId = event.getMetadataString("customer").orElse(null);

    return new SubscriptionData(
        event.resourceId(),
        customerId,
        mapStripeStatusToLocal(status)
    );
  }

  /**
   * Populate subscription entity with detailed Stripe subscription data.
   */
  public void populateFromStripeSubscription(TenantSubscription subscription, WebhookEvent event) {
    Object rawSubscription = event.metadata().get("subscription");
    if (rawSubscription instanceof com.stripe.model.Subscription stripeSubscription) {
      populateFromStripeObject(subscription, stripeSubscription);
    }
  }

  /**
   * Map Stripe subscription status to local status enum.
   */
  public SubscriptionStatus mapStripeStatusToLocal(String stripeStatus) {
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
   */
  private void populateFromStripeObject(TenantSubscription subscription,
                                        com.stripe.model.Subscription stripeSubscription) {
    // Extract period dates using JSON parsing for SDK compatibility
    extractPeriodDates(subscription, stripeSubscription);

    // Extract trial and cancellation dates
    extractTrialAndCancellationDates(subscription, stripeSubscription);

    // Extract cancellation details
    extractCancellationDetails(subscription, stripeSubscription);
  }

  private void extractPeriodDates(TenantSubscription subscription,
                                  com.stripe.model.Subscription stripeSubscription) {
    try {
      String jsonString = stripeSubscription.toJson();
      JsonNode root = objectMapper.readTree(jsonString);

      if (root.has("current_period_start") && !root.get("current_period_start").isNull()) {
        subscription.setCurrentPeriodStart(
            Instant.ofEpochSecond(root.get("current_period_start").asLong())
        );
      }

      if (root.has("current_period_end") && !root.get("current_period_end").isNull()) {
        subscription.setCurrentPeriodEnd(
            Instant.ofEpochSecond(root.get("current_period_end").asLong())
        );
      }
    } catch (final Exception e) {
      logger.error("Failed to parse Stripe subscription JSON for period dates", e);
    }
  }

  private void extractTrialAndCancellationDates(TenantSubscription subscription,
                                                com.stripe.model.Subscription stripeSubscription) {
    if (stripeSubscription.getTrialEnd() != null) {
      subscription.setTrialEnd(Instant.ofEpochSecond(stripeSubscription.getTrialEnd()));
    }

    if (stripeSubscription.getCancelAt() != null) {
      subscription.setCancelAt(Instant.ofEpochSecond(stripeSubscription.getCancelAt()));
    }

    if (stripeSubscription.getCanceledAt() != null) {
      subscription.setCanceledAt(Instant.ofEpochSecond(stripeSubscription.getCanceledAt()));
    }
  }

  private void extractCancellationDetails(TenantSubscription subscription,
                                          com.stripe.model.Subscription stripeSubscription) {
    if (stripeSubscription.getCancellationDetails() != null
        && stripeSubscription.getCancellationDetails().getReason() != null) {
      subscription.setCancellationReason(
          stripeSubscription.getCancellationDetails().getReason()
      );
    }
  }

  /**
   * Data transfer object for basic subscription information.
   */
  public record SubscriptionData(
      String subscriptionId,
      String customerId,
      SubscriptionStatus status
  ) {
  }
}
