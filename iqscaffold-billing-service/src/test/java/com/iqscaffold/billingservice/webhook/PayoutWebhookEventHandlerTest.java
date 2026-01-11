package com.iqscaffold.billingservice.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.payout.PayoutService;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayoutWebhookEventHandlerTest {

  @Mock
  private PayoutService payoutService;

  private PayoutWebhookEventHandler handler;

  @BeforeEach
  void setUp() {
    handler = new PayoutWebhookEventHandler(payoutService);
  }

  @Test
  void supports_shouldReturnTrueForPayoutEvents() {
    // Given
    WebhookEvent event = createPayoutEvent(WebhookEvent.EventType.PAYOUT_PAID);

    // When
    final boolean supports = handler.supports(event);

    // Then
    assertTrue(supports);
  }

  @Test
  void supports_shouldReturnFalseForNonPayoutEvents() {
    // Given
    WebhookEvent event = createPaymentEvent();

    // When
    final boolean supports = handler.supports(event);

    // Then
    assertFalse(supports);
  }

  @Test
  void handleEvent_shouldProcessStripePayout() {
    // Given
    com.stripe.model.Payout stripePayout = new com.stripe.model.Payout();
    stripePayout.setId("po_123");
    stripePayout.setAmount(100000L);
    stripePayout.setCurrency("usd");

    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.PAYOUT_PAID,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "po_123",
        WebhookEvent.ResourceType.PAYOUT,
        Map.of(),
        stripePayout
    );

    // When
    handler.handleEvent(event);

    // Then
    verify(payoutService).processPayout(any(com.stripe.model.Payout.class));
  }

  @Test
  void handleEvent_shouldHandlePayoutFailed() {
    // Given
    WebhookEvent event = createPayoutEvent(WebhookEvent.EventType.PAYOUT_FAILED);

    // When
    handler.handleEvent(event);

    // Then - should not throw exception
  }

  @Test
  void handleEvent_shouldThrowExceptionForUnsupportedEvent() {
    // Given
    WebhookEvent event = createPaymentEvent();

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        handler.handleEvent(event)
    );
  }

  private WebhookEvent createPayoutEvent(final String eventType) {
    return new WebhookEvent(
        "evt_123",
        eventType,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "po_123",
        WebhookEvent.ResourceType.PAYOUT,
        Map.of(),
        new Object()
    );
  }

  private WebhookEvent createPaymentEvent() {
    return new WebhookEvent(
        "evt_456",
        WebhookEvent.EventType.PAYMENT_SUCCEEDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        new Object()
    );
  }
}
