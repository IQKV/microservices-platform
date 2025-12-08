package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event published when invoice generation is requested for a subscription renewal.
 * This event is triggered by the SubscriptionRenewalJob and processed asynchronously
 * to generate invoices with PDF creation.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId subscription ID
 * @param tenantId tenant ID
 * @param userId user ID
 * @param planId plan ID
 * @param periodStart billing period start date
 * @param periodEnd billing period end date
 */
public record InvoiceGenerationRequested(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  UUID userId,
  Long planId,
  LocalDateTime periodStart,
  LocalDateTime periodEnd
) implements DomainEvent {

  public InvoiceGenerationRequested {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.INVOICE_GENERATION_REQUESTED;
  }
}
