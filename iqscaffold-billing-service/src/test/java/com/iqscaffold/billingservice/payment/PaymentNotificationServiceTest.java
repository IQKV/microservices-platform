package com.iqscaffold.billingservice.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iqscaffold.billingservice.infrastructure.messaging.MessagingService;
import com.iqscaffold.billingservice.shared.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private MessagingService messagingService;

  private PaymentNotificationService paymentNotificationService;

  @BeforeEach
  void setUp() {
    paymentNotificationService = new PaymentNotificationService(
        notificationService,
        messagingService
    );
  }

  @Test
  void handlePaymentSuccessful_shouldSendNotifications() {
    // Given
    PaymentNotificationService.PaymentSuccessfulEvent event =
        new PaymentNotificationService.PaymentSuccessfulEvent(
            "pay_123",
            "customer@example.com",
            "John Doe",
            new BigDecimal("100.00"),
            "usd",
            "Test payment",
            LocalDateTime.now(),
            "card",
            "https://receipt.url",
            "tenant_123"
        );

    doNothing().when(notificationService).sendPaymentSuccessfulNotification(
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
    );
    doNothing().when(messagingService).publishPaymentSuccessful(any(), any(), any());

    // When
    paymentNotificationService.handlePaymentSuccessful(event);

    // Then
    verify(notificationService).sendPaymentSuccessfulNotification(
        eq("customer@example.com"),
        eq("John Doe"),
        eq("pay_123"),
        eq(new BigDecimal("100.00")),
        eq("usd"),
        eq("Test payment"),
        any(LocalDateTime.class),
        eq("card"),
        eq("https://receipt.url"),
        eq("tenant_123")
    );
    verify(messagingService).publishPaymentSuccessful("pay_123", "tenant_123", "customer@example.com");
  }

  @Test
  void handlePaymentSuccessful_shouldNotThrowOnNotificationFailure() {
    // Given
    PaymentNotificationService.PaymentSuccessfulEvent event =
        new PaymentNotificationService.PaymentSuccessfulEvent(
            "pay_123",
            "customer@example.com",
            "John Doe",
            new BigDecimal("100.00"),
            "usd",
            "Test payment",
            LocalDateTime.now(),
            "card",
            "https://receipt.url",
            "tenant_123"
        );

    doThrow(new RuntimeException("Notification failed"))
        .when(notificationService).sendPaymentSuccessfulNotification(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );

    // When & Then - should not throw
    assertDoesNotThrow(() -> paymentNotificationService.handlePaymentSuccessful(event));
  }

  @Test
  void handlePaymentFailed_shouldSendNotifications() {
    // Given
    PaymentNotificationService.PaymentFailedEvent event =
        new PaymentNotificationService.PaymentFailedEvent(
            "pay_456",
            "customer@example.com",
            "Jane Doe",
            new BigDecimal("50.00"),
            "eur",
            "Failed payment",
            LocalDateTime.now(),
            "Card declined",
            "https://retry.url",
            "tenant_456"
        );

    doNothing().when(notificationService).sendPaymentFailedNotification(
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
    );
    doNothing().when(messagingService).publishPaymentFailed(any(), any(), any());

    // When
    paymentNotificationService.handlePaymentFailed(event);

    // Then
    verify(notificationService).sendPaymentFailedNotification(
        eq("customer@example.com"),
        eq("Jane Doe"),
        eq("pay_456"),
        eq(new BigDecimal("50.00")),
        eq("eur"),
        eq("Failed payment"),
        any(LocalDateTime.class),
        eq("Card declined"),
        eq("https://retry.url"),
        eq("tenant_456")
    );
    verify(messagingService).publishPaymentFailed("pay_456", "tenant_456", "customer@example.com");
  }

  @Test
  void handlePaymentFailed_shouldNotThrowOnNotificationFailure() {
    // Given
    PaymentNotificationService.PaymentFailedEvent event =
        new PaymentNotificationService.PaymentFailedEvent(
            "pay_456",
            "customer@example.com",
            "Jane Doe",
            new BigDecimal("50.00"),
            "eur",
            "Failed payment",
            LocalDateTime.now(),
            "Card declined",
            "https://retry.url",
            "tenant_456"
        );

    doThrow(new RuntimeException("Notification failed"))
        .when(notificationService).sendPaymentFailedNotification(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );

    // When & Then - should not throw
    assertDoesNotThrow(() -> paymentNotificationService.handlePaymentFailed(event));
  }

  @Test
  void handlePaymentRefunded_shouldSendNotifications() {
    // Given
    PaymentNotificationService.PaymentRefundedEvent event =
        new PaymentNotificationService.PaymentRefundedEvent(
            "pay_789",
            "customer@example.com",
            "Bob Smith",
            new BigDecimal("75.00"),
            "gbp",
            "ref_123",
            LocalDateTime.now(),
            "tenant_789"
        );

    doNothing().when(notificationService).sendPaymentRefundedNotification(
        any(), any(), any(), any(), any(), any(), any(), any()
    );
    doNothing().when(messagingService).publishPaymentRefunded(any(), any(), any());

    // When
    paymentNotificationService.handlePaymentRefunded(event);

    // Then
    verify(notificationService).sendPaymentRefundedNotification(
        eq("customer@example.com"),
        eq("Bob Smith"),
        eq("pay_789"),
        eq(new BigDecimal("75.00")),
        eq("gbp"),
        eq("ref_123"),
        any(LocalDateTime.class),
        eq("tenant_789")
    );
    verify(messagingService).publishPaymentRefunded("pay_789", "tenant_789", "customer@example.com");
  }

  @Test
  void handlePaymentRefunded_shouldNotThrowOnNotificationFailure() {
    // Given
    PaymentNotificationService.PaymentRefundedEvent event =
        new PaymentNotificationService.PaymentRefundedEvent(
            "pay_789",
            "customer@example.com",
            "Bob Smith",
            new BigDecimal("75.00"),
            "gbp",
            "ref_123",
            LocalDateTime.now(),
            "tenant_789"
        );

    doThrow(new RuntimeException("Notification failed"))
        .when(notificationService).sendPaymentRefundedNotification(
            any(), any(), any(), any(), any(), any(), any(), any()
        );

    // When & Then - should not throw
    assertDoesNotThrow(() -> paymentNotificationService.handlePaymentRefunded(event));
  }

  @Test
  void handlePaymentSuccessful_shouldHandleMessagingServiceFailure() {
    // Given
    PaymentNotificationService.PaymentSuccessfulEvent event =
        new PaymentNotificationService.PaymentSuccessfulEvent(
            "pay_123",
            "customer@example.com",
            "John Doe",
            new BigDecimal("100.00"),
            "usd",
            "Test payment",
            LocalDateTime.now(),
            "card",
            "https://receipt.url",
            "tenant_123"
        );

    doNothing().when(notificationService).sendPaymentSuccessfulNotification(
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
    );
    doThrow(new RuntimeException("Messaging failed"))
        .when(messagingService).publishPaymentSuccessful(any(), any(), any());

    // When & Then - should not throw
    assertDoesNotThrow(() -> paymentNotificationService.handlePaymentSuccessful(event));
  }

  @Test
  void paymentSuccessfulEvent_shouldSetAllFields() {
    // Given
    PaymentNotificationService.PaymentSuccessfulEvent event =
        new PaymentNotificationService.PaymentSuccessfulEvent();

    // When
    event.setPaymentId("pay_123");
    event.setCustomerEmail("test@example.com");
    event.setCustomerName("Test User");
    event.setAmount(new BigDecimal("100.00"));
    event.setCurrency("usd");
    event.setDescription("Test");
    event.setPaymentDate(LocalDateTime.now());
    event.setPaymentMethod("card");
    event.setReceiptUrl("https://receipt.url");
    event.setTenantId("tenant_123");

    // Then
    assert event.getPaymentId().equals("pay_123");
    assert event.getCustomerEmail().equals("test@example.com");
    assert event.getCustomerName().equals("Test User");
    assert event.getAmount().equals(new BigDecimal("100.00"));
    assert event.getCurrency().equals("usd");
    assert event.getDescription().equals("Test");
    assert event.getPaymentDate() != null;
    assert event.getPaymentMethod().equals("card");
    assert event.getReceiptUrl().equals("https://receipt.url");
    assert event.getTenantId().equals("tenant_123");
  }

  @Test
  void paymentFailedEvent_shouldSetAllFields() {
    // Given
    PaymentNotificationService.PaymentFailedEvent event =
        new PaymentNotificationService.PaymentFailedEvent();

    // When
    event.setPaymentId("pay_456");
    event.setCustomerEmail("test@example.com");
    event.setCustomerName("Test User");
    event.setAmount(new BigDecimal("50.00"));
    event.setCurrency("eur");
    event.setDescription("Failed");
    event.setAttemptDate(LocalDateTime.now());
    event.setErrorMessage("Card declined");
    event.setRetryUrl("https://retry.url");
    event.setTenantId("tenant_456");

    // Then
    assert event.getPaymentId().equals("pay_456");
    assert event.getCustomerEmail().equals("test@example.com");
    assert event.getCustomerName().equals("Test User");
    assert event.getAmount().equals(new BigDecimal("50.00"));
    assert event.getCurrency().equals("eur");
    assert event.getDescription().equals("Failed");
    assert event.getAttemptDate() != null;
    assert event.getErrorMessage().equals("Card declined");
    assert event.getRetryUrl().equals("https://retry.url");
    assert event.getTenantId().equals("tenant_456");
  }

  @Test
  void paymentRefundedEvent_shouldSetAllFields() {
    // Given
    PaymentNotificationService.PaymentRefundedEvent event =
        new PaymentNotificationService.PaymentRefundedEvent();

    // When
    event.setPaymentId("pay_789");
    event.setCustomerEmail("test@example.com");
    event.setCustomerName("Test User");
    event.setAmount(new BigDecimal("75.00"));
    event.setCurrency("gbp");
    event.setRefundId("ref_123");
    event.setRefundDate(LocalDateTime.now());
    event.setTenantId("tenant_789");

    // Then
    assert event.getPaymentId().equals("pay_789");
    assert event.getCustomerEmail().equals("test@example.com");
    assert event.getCustomerName().equals("Test User");
    assert event.getAmount().equals(new BigDecimal("75.00"));
    assert event.getCurrency().equals("gbp");
    assert event.getRefundId().equals("ref_123");
    assert event.getRefundDate() != null;
    assert event.getTenantId().equals("tenant_789");
  }
}
