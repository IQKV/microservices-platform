package com.iqscaffold.billingservice.shared.event;

import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when a payment retry is scheduled.
 * This event is triggered by the PaymentRetryJob and processed asynchronously
 * to attempt payment processing again.
 *
 * @param eventId      unique identifier for this event
 * @param occurredAt   timestamp when the event occurred
 * @param aggregateId  payment ID
 * @param tenantId     tenant ID
 * @param invoiceId    invoice ID
 * @param retryAttempt current retry attempt number (1-4)
 * @param amount       payment amount
 */
public record PaymentRetryRequested(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    Long invoiceId,
    int retryAttempt,
    java.math.BigDecimal amount
) implements DomainEvent {

  public PaymentRetryRequested {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.PAYMENT_RETRY_REQUESTED;
  }
}
