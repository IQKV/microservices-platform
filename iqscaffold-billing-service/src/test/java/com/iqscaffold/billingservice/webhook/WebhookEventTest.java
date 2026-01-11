package com.iqscaffold.billingservice.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import org.junit.jupiter.api.Test;

class WebhookEventTest {

  @Test
  void isPaymentEvent_shouldReturnTrueForPaymentEvents() {
    // Given
    WebhookEvent event = createEvent(WebhookEvent.EventType.PAYMENT_SUCCEEDED);

    // When
    final boolean isPaymentEvent = event.isPaymentEvent();

    // Then
    assertTrue(isPaymentEvent);
  }

  @Test
  void isPaymentEvent_shouldReturnFalseForNonPaymentEvents() {
    // Given
    WebhookEvent event = createEvent(WebhookEvent.EventType.PAYOUT_PAID);

    // When
    final boolean isPaymentEvent = event.isPaymentEvent();

    // Then
    assertFalse(isPaymentEvent);
  }

  @Test
  void isPayoutEvent_shouldReturnTrueForPayoutEvents() {
    // Given
    WebhookEvent event = createEvent(WebhookEvent.EventType.PAYOUT_PAID);

    // When
    final boolean isPayoutEvent = event.isPayoutEvent();

    // Then
    assertTrue(isPayoutEvent);
  }

  @Test
  void isPayoutEvent_shouldReturnFalseForNonPayoutEvents() {
    // Given
    WebhookEvent event = createEvent(WebhookEvent.EventType.PAYMENT_SUCCEEDED);

    // When
    final boolean isPayoutEvent = event.isPayoutEvent();

    // Then
    assertFalse(isPayoutEvent);
  }

  @Test
  void isAccountEvent_shouldReturnTrueForAccountEvents() {
    // Given
    WebhookEvent event = createEvent(WebhookEvent.EventType.ACCOUNT_UPDATED);

    // When
    final boolean isAccountEvent = event.isAccountEvent();

    // Then
    assertTrue(isAccountEvent);
  }

  @Test
  void isAccountEvent_shouldReturnFalseForNonAccountEvents() {
    // Given
    WebhookEvent event = createEvent(WebhookEvent.EventType.PAYMENT_SUCCEEDED);

    // When
    final boolean isAccountEvent = event.isAccountEvent();

    // Then
    assertFalse(isAccountEvent);
  }

  @Test
  void getMetadataString_shouldReturnValueWhenPresent() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.PAYMENT_SUCCEEDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of("order_id", "order-123", "count", 5),
        new Object()
    );

    // When
    Optional<String> orderId = event.getMetadataString("order_id");
    Optional<String> count = event.getMetadataString("count");

    // Then
    assertTrue(orderId.isPresent());
    assertEquals("order-123", orderId.get());
    assertTrue(count.isPresent());
    assertEquals("5", count.get());
  }

  @Test
  void getMetadataString_shouldReturnEmptyWhenNotPresent() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.PAYMENT_SUCCEEDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        new Object()
    );

    // When
    Optional<String> value = event.getMetadataString("nonexistent");

    // Then
    assertFalse(value.isPresent());
  }

  @Test
  void getMetadataBoolean_shouldReturnValueWhenPresent() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.PAYMENT_REFUNDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.CHARGE,
        Map.of("refunded", true, "captured", false),
        new Object()
    );

    // When
    Optional<Boolean> refunded = event.getMetadataBoolean("refunded");
    Optional<Boolean> captured = event.getMetadataBoolean("captured");

    // Then
    assertTrue(refunded.isPresent());
    assertTrue(refunded.get());
    assertTrue(captured.isPresent());
    assertFalse(captured.get());
  }

  @Test
  void getMetadataBoolean_shouldReturnEmptyWhenNotBoolean() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.PAYMENT_SUCCEEDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of("order_id", "order-123"),
        new Object()
    );

    // When
    Optional<Boolean> value = event.getMetadataBoolean("order_id");

    // Then
    assertFalse(value.isPresent());
  }

  @Test
  void getMetadataBoolean_shouldReturnEmptyWhenNotPresent() {
    // Given
    WebhookEvent event = new WebhookEvent(
        "evt_123",
        WebhookEvent.EventType.PAYMENT_SUCCEEDED,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "pi_123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        new Object()
    );

    // When
    Optional<Boolean> value = event.getMetadataBoolean("nonexistent");

    // Then
    assertFalse(value.isPresent());
  }

  private WebhookEvent createEvent(final String eventType) {
    return new WebhookEvent(
        "evt_123",
        eventType,
        PaymentGatewayProvider.STRIPE,
        Optional.of("tenant-1"),
        "resource-123",
        WebhookEvent.ResourceType.PAYMENT_INTENT,
        Map.of(),
        new Object()
    );
  }
}
