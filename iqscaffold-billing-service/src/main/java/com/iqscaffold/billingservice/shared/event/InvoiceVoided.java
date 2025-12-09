package com.iqscaffold.billingservice.shared.event;

import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when an invoice is voided.
 * This event is triggered after successful invoice voiding.
 *
 * @param eventId        unique identifier for this event
 * @param occurredAt     timestamp when the event occurred
 * @param aggregateId    invoice ID
 * @param tenantId       tenant ID
 * @param subscriptionId subscription ID
 * @param invoiceNumber  invoice number
 * @param reason         reason for voiding
 * @param voidedBy       user ID who voided the invoice
 */
public record InvoiceVoided(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    Long subscriptionId,
    String invoiceNumber,
    String reason,
    UUID voidedBy
) implements DomainEvent {

  public InvoiceVoided {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.INVOICE_VOIDED;
  }
}
