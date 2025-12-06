package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a trial period ends.
 * This event is triggered when the trial period expires.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param userId user ID
 * @param planId plan ID
 * @param trialEnd trial end date
 * @param converted whether the trial was converted to a paid subscription
 */
public record TrialEnded(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  UUID userId,
  Long planId,
  Instant trialEnd,
  boolean converted
) implements DomainEvent {

  public TrialEnded {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.TRIAL_ENDED;
  }
}
