package com.iqscaffold.billingservice.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when a payment is refunded.
 * This event is triggered after successful refund processing and persistence.
 *
 * @param eventId          unique identifier for this event
 * @param occurredAt       timestamp when the event occurred
 * @param aggregateId      payment ID
 * @param tenantId         tenant ID
 * @param invoiceId        invoice ID
 * @param subscriptionId   subscription ID
 * @param refundAmount     refund amount
 * @param currency         currency code
 * @param reason           refund reason
 * @param refundedBy       user ID who initiated the refund
 * @param providerRefundId payment provider's refund ID
 */
public record PaymentRefunded(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    Long invoiceId,
    Long subscriptionId,
    BigDecimal refundAmount,
    String currency,
    String reason,
    UUID refundedBy,
    String providerRefundId
) implements DomainEvent {

  public PaymentRefunded {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.PAYMENT_REFUNDED;
  }
}
