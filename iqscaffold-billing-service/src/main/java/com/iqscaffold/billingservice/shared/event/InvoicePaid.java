package com.iqscaffold.billingservice.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when an invoice is paid.
 * This event is triggered after successful payment processing and invoice update.
 *
 * @param eventId        unique identifier for this event
 * @param occurredAt     timestamp when the event occurred
 * @param aggregateId    invoice ID
 * @param tenantId       tenant ID
 * @param subscriptionId subscription ID
 * @param invoiceNumber  invoice number
 * @param paymentId      payment ID
 * @param amountPaid     amount paid
 * @param currency       currency code
 * @param paidAt         payment timestamp
 */
public record InvoicePaid(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    Long subscriptionId,
    String invoiceNumber,
    Long paymentId,
    BigDecimal amountPaid,
    String currency,
    Instant paidAt
) implements DomainEvent {

  public InvoicePaid {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.INVOICE_PAID;
  }
}
