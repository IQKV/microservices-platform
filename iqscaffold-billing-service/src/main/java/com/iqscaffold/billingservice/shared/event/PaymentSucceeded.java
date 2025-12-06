package com.iqscaffold.billingservice.shared.event;

import com.iqscaffold.billingservice.shared.BillingConstants;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a payment succeeds.
 * This event is triggered after successful payment processing and persistence.
 *
 * @param eventId unique identifier for this event
 * @param occurredAt timestamp when the event occurred
 * @param aggregateId payment ID
 * @param tenantId tenant ID
 * @param invoiceId invoice ID
 * @param subscriptionId subscription ID
 * @param amount payment amount
 * @param currency currency code
 * @param paymentMethodId payment method ID
 * @param providerPaymentId payment provider's payment ID
 */
public record PaymentSucceeded(
  UUID eventId,
  Instant occurredAt,
  Long aggregateId,
  UUID tenantId,
  Long invoiceId,
  Long subscriptionId,
  BigDecimal amount,
  String currency,
  Long paymentMethodId,
  String providerPaymentId
) implements DomainEvent {

  public PaymentSucceeded {
    if (eventId == null) {
      eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  @Override
  public String eventType() {
    return BillingConstants.BillingEvents.PAYMENT_SUCCEEDED;
  }
}
