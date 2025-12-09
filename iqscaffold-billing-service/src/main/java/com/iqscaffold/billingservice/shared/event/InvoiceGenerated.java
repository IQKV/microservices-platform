package com.iqscaffold.billingservice.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when an invoice is generated.
 * This event is triggered after successful invoice creation and persistence.
 *
 * @param eventId        unique identifier for this event
 * @param occurredAt     timestamp when the event occurred
 * @param aggregateId    invoice ID
 * @param tenantId       tenant ID
 * @param subscriptionId subscription ID
 * @param invoiceNumber  invoice number
 * @param totalAmount    total invoice amount
 * @param currency       currency code
 * @param dueDate        invoice due date
 */
public record InvoiceGenerated(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    Long subscriptionId,
    String invoiceNumber,
    BigDecimal totalAmount,
    String currency,
    Instant dueDate
) implements DomainEvent {

  public InvoiceGenerated {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.INVOICE_GENERATED;
  }
}
