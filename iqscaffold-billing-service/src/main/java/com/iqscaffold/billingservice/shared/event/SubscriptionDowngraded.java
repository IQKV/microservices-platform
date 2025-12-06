package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a subscription is downgraded to a lower-tier plan.
 * This event is triggered after successful subscription downgrade and persistence.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param fromPlanId previous plan ID
 * @param toPlanId new plan ID
 * @param prorationCredit proration credit amount
 * @param effectiveDate when the downgrade takes effect
 * @param immediate whether the downgrade is immediate or at period end
 */
public record SubscriptionDowngraded(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  Long fromPlanId,
  Long toPlanId,
  BigDecimal prorationCredit,
  Instant effectiveDate,
  boolean immediate
) implements DomainEvent {

  public SubscriptionDowngraded {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.SUBSCRIPTION_DOWNGRADED;
  }
}
