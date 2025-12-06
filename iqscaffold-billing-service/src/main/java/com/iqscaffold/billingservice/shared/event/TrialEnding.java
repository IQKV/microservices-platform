package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a trial period is ending soon (typically 3 days before expiration).
 * This event is used to trigger reminder notifications to users.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param userId user ID
 * @param planId plan ID
 * @param trialEnd trial end date
 * @param daysRemaining number of days remaining in trial
 */
public record TrialEnding(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  UUID userId,
  Long planId,
  Instant trialEnd,
  int daysRemaining
) implements DomainEvent {

  public TrialEnding {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.TRIAL_ENDING;
  }
}
