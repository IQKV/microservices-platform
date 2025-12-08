package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event published when a trial ending reminder should be sent.
 * This event is triggered by the TrialReminderJob and processed asynchronously
 * to send email notifications to users about their expiring trial.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param userId user ID
 * @param planId plan ID
 * @param trialEnd trial end date
 * @param daysRemaining days remaining until trial ends
 */
public record TrialReminderRequested(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  UUID userId,
  Long planId,
  LocalDateTime trialEnd,
  int daysRemaining
) implements DomainEvent {

  public TrialReminderRequested {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.TRIAL_REMINDER_REQUESTED;
  }
}
