package com.iqscaffold.billingservice.shared.event;

import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when a canceled subscription is reactivated.
 * This event is triggered after successful subscription reactivation and persistence.
 *
 * @param eventId       unique identifier for this event
 * @param occurredAt    timestamp when the event occurred
 * @param aggregateId   subscription ID
 * @param tenantId      tenant ID
 * @param reactivatedBy user ID who reactivated the subscription
 * @param planId        plan ID of the reactivated subscription
 * @param newPeriodEnd  new period end date after reactivation
 */
public record SubscriptionReactivated(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    UUID reactivatedBy,
    Long planId,
    Instant newPeriodEnd
) implements DomainEvent {

  public SubscriptionReactivated {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.SUBSCRIPTION_REACTIVATED;
  }
}
