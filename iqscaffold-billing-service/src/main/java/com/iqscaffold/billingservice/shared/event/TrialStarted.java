package com.iqscaffold.billingservice.shared.event;

import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when a trial period starts for a subscription.
 * This event is triggered after successful trial subscription creation and persistence.
 *
 * @param eventId     unique identifier for this event
 * @param occurredAt  timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId    tenant ID
 * @param userId      user ID who started the trial
 * @param planId      plan ID of the trial subscription
 * @param trialStart  trial start date
 * @param trialEnd    trial end date
 * @param trialDays   number of trial days
 */
public record TrialStarted(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    UUID userId,
    Long planId,
    Instant trialStart,
    Instant trialEnd,
    int trialDays
) implements DomainEvent {

  public TrialStarted {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.TRIAL_STARTED;
  }
}
