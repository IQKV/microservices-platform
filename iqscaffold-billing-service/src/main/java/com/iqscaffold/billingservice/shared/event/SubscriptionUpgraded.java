package com.iqscaffold.billingservice.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when a subscription is upgraded to a higher-tier plan.
 * This event is triggered after successful subscription upgrade and persistence.
 *
 * @param eventId         unique identifier for this event
 * @param occurredAt      timestamp when the event occurred
 * @param aggregateId     subscription ID
 * @param tenantId        tenant ID
 * @param fromPlanId      previous plan ID
 * @param toPlanId        new plan ID
 * @param prorationAmount proration amount charged/credited
 * @param effectiveDate   when the upgrade takes effect
 */
public record SubscriptionUpgraded(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    Long fromPlanId,
    Long toPlanId,
    BigDecimal prorationAmount,
    Instant effectiveDate
) implements DomainEvent {

  public SubscriptionUpgraded {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.SUBSCRIPTION_UPGRADED;
  }
}
