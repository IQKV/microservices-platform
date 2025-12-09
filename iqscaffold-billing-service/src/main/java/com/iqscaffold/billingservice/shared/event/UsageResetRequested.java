package com.iqscaffold.billingservice.shared.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when usage counter reset is requested for a new billing period.
 * This event is triggered by the UsageResetJob and processed asynchronously
 * to reset usage counters and archive old usage data.
 *
 * @param eventId        unique identifier for this event
 * @param occurredAt     timestamp when the event occurred
 * @param aggregateId    subscription ID
 * @param tenantId       tenant ID
 * @param newPeriodStart new billing period start date
 */
public record UsageResetRequested(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    LocalDateTime newPeriodStart
) implements DomainEvent {

  public UsageResetRequested {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.USAGE_RESET_REQUESTED;
  }
}
