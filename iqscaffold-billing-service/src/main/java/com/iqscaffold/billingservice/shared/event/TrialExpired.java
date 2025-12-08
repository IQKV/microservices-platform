package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a trial period expires without conversion.
 * This event is triggered by the TrialExpirationJob and processed asynchronously
 * to update subscription status and send notifications.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param userId user ID
 * @param planId plan ID
 * @param trialEnd trial end date
 */
public record TrialExpired(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  UUID userId,
  Long planId,
  Instant trialEnd
) implements DomainEvent {

  public TrialExpired {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.TRIAL_EXPIRED;
  }
}
