package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new subscription is created.
 * This event is triggered after successful subscription creation and persistence.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param userId user ID who created the subscription
 * @param planId subscription plan ID
 * @param status initial subscription status
 * @param isTrialSubscription whether this is a trial subscription
 */
public record SubscriptionCreated(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  UUID userId,
  Long planId,
  String status,
  boolean isTrialSubscription
) implements DomainEvent {

  public SubscriptionCreated {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.SUBSCRIPTION_CREATED;
  }
}
