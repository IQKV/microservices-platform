package com.iqscaffold.billingservice.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.payment.PaymentService;
import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookEventHandlerTest {

  @Mock
  private PaymentService paymentService;

  private PaymentWebhookEventHandler handler;

  @BeforeEach
  void setUp() {
    handler = new PaymentWebhookEventHandler(paymentService);
  }

  @Test
  void supports_shouldReturnTrueForPaymentEvents() {
    // Given
    WebhookEvent event = createPaymentEvent(WebhookEvent.EventType.PAYMENT_SUCCEEDED);

    // When
    final boolean supports = handler.supports(event);

    // Then
    assertTrue(supports);
  }

  @Test
  void supports_shouldReturnFalseForNonPaymentEvents() {
    // Given
    WebhookEvent event = createPayoutEvent();

    // When
    final boolean supports = handler.supports(event);

    // Then
    assertFalse(supports);
  }

  @Test
  void handleEvent_shouldUpdateStatusToSucceededForPaymentSucceeded() {
    // Given
    WebhookEvent event = createPaymentEvent(WebhookEvent.EventType.PAYMENT_SUCCEEDED);

    // When
    handler.handleEvent(event);

    // Then
    verify(paymentService).updateStatus(eq("pi_123"), eq(BillingConstants.PaymentStatus.SUCCEEDED));
  }

  @Test
  void handleEvent_shouldUpdateStatusToFailedForPaymentFailed() {
    // Given
    WebhookEvent event = createPaymentEvent(WebhookEvent.EventType.PAYMENT_FAILED);

    // When
    handler.handleEvent(event);

    // Then
    verify(paymentService).updateStatus(eq("pi_123"), eq(BillingConstants.PaymentStatus.FAILED));
  }

  @Test
  void handleEvent_shouldUpdateStatusToRefundedForFullRefund() {
    // Given
    WebhookEvent event = createRefundEvent(true);

    // When
    handler.handleEvent(event);

    // Then
    verify(paymentService).updateStatus(eq("pi_123"), eq(BillingConstants.PaymentStatus.REFUNDED));
  }

  @Test
  void handleEvent_shouldUpdateStatusToPartiallyRefundedForPartialRefund() {
    // Given
    WebhookEvent event = createRefundEvent(false);

    // When
    handler.handleEvent(event);

    // Then
    verify(paymentService).updateStatus(eq("pi_123"), eq(BillingConstants.PaymentStatus.PARTIALLY_REFUNDED));
  }

  @Test
  void handleEvent_shouldThrowExceptionForUnsupportedEvent() {
    // Given
    WebhookEvent event = createPayoutEvent();

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        handler.handleEvent(event)
    );
  }

  private WebhookEvent createPaymentEvent(final String eventType) {
    return new WebhookEvent(
        "evt_123",
        eventType,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        new Object()
    );
  }

  private WebhookEvent createRefundEvent(final boolean fullRefund) {
    return new WebhookEvent(
        "evt_456",
        WebhookEvent.EventType.PAYMENT_REFUNDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.CHARGE,
        Map.of("refunded", fullRefund),
        new Object()
    );
  }

  private WebhookEvent createPayoutEvent() {
    return new WebhookEvent(
        "evt_789",
        WebhookEvent.EventType.PAYOUT_PAID,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "po_123",
        WebhookEvent.ResourceType.PAYOUT,
        Map.of(),
        new Object()
    );
  }
}
