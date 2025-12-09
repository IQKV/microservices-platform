package com.iqscaffold.billingservice.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.BillingConstants;

/**
 * Domain event published when a payment fails.
 * This event is triggered after a failed payment attempt and persistence.
 *
 * @param eventId         unique identifier for this event
 * @param occurredAt      timestamp when the event occurred
 * @param aggregateId     payment ID
 * @param tenantId        tenant ID
 * @param invoiceId       invoice ID
 * @param subscriptionId  subscription ID
 * @param amount          attempted payment amount
 * @param currency        currency code
 * @param paymentMethodId payment method ID
 * @param failureReason   reason for payment failure
 * @param retryAttempt    retry attempt number
 */
public record PaymentFailed(
    UUID eventId,
    Instant occurredAt,
    Long aggregateId,
    UUID tenantId,
    Long invoiceId,
    Long subscriptionId,
    BigDecimal amount,
    String currency,
    Long paymentMethodId,
    String failureReason,
    int retryAttempt
) implements DomainEvent {

  public PaymentFailed {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.PAYMENT_FAILED;
  }
}
