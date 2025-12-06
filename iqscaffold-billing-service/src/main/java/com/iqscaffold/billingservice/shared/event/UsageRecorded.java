package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when usage is recorded.
 * This event is triggered after successful usage record persistence.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId usage record ID
 * @param tenantId tenant ID
 * @param subscriptionId subscription ID
 * @param metricType metric type (e.g., API_CALLS, STORAGE_GB)
 * @param quantity usage quantity
 * @param unit usage unit
 * @param recordedAt when the usage was recorded
 */
public record UsageRecorded(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  Long subscriptionId,
  String metricType,
  Long quantity,
  String unit,
  Instant recordedAt
) implements DomainEvent {

  public UsageRecorded {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.USAGE_RECORDED;
  }
}
