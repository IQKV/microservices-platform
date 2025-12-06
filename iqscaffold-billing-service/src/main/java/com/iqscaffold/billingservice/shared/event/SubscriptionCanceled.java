package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a subscription is canceled.
 * This event is triggered after successful subscription cancellation and persistence.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param reason cancellation reason
 * @param canceledBy user ID who canceled the subscription
 * @param immediate whether the cancellation is immediate or at period end
 * @param effectiveDate when the cancellation takes effect
 */
public record SubscriptionCanceled(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  String reason,
  UUID canceledBy,
  boolean immediate,
  Instant effectiveDate
) implements DomainEvent {

  public SubscriptionCanceled {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.SUBSCRIPTION_CANCELED;
  }
}
