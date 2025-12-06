package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a quota is exceeded.
 * This event is triggered when usage exceeds the plan's quota limit.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param subscriptionId subscription ID
 * @param metricType metric type that exceeded quota
 * @param quotaLimit quota limit
 * @param currentUsage current usage amount
 * @param excessAmount amount over quota
 */
public record QuotaExceeded(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  Long subscriptionId,
  String metricType,
  Long quotaLimit,
  Long currentUsage,
  Long excessAmount
) implements DomainEvent {

  public QuotaExceeded {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.QUOTA_EXCEEDED;
  }
}
