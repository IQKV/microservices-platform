package com.iqscaffold.billingservice.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.iqscaffold.billingservice.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

  @Mock
  private RabbitTemplate rabbitTemplate;

  private MessagingService messagingService;

  @BeforeEach
  void setUp() {
    messagingService = new MessagingService(rabbitTemplate);
  }

  @Test
  void publishBillingEvent_shouldPublishEventWithCorrectRoutingKey() {
    // Given
    BillingEvent event = BillingEvent.paymentSuccessful("pay-123", "tenant-123", "customer@example.com");
    String routingKey = "billing.payment.successful";

    // When
    messagingService.publishBillingEvent(event, routingKey);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(routingKey),
        eq(event)
    );
  }

  @Test
  void publishBillingEvent_shouldThrowMessagingExceptionOnFailure() {
    // Given
    BillingEvent event = BillingEvent.paymentSuccessful("pay-123", "tenant-123", "customer@example.com");
    String routingKey = "billing.payment.successful";
    doThrow(new RuntimeException("RabbitMQ error"))
        .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(BillingEvent.class));

    // When & Then
    MessagingException exception = assertThrows(MessagingException.class, () ->
        messagingService.publishBillingEvent(event, routingKey)
    );
    assertEquals("Failed to publish billing event", exception.getMessage());
  }

  @Test
  void publishNotificationEvent_shouldPublishEventToNotificationQueue() {
    // Given
    NotificationEvent event = NotificationEvent.emailNotification(
        "recipient@example.com",
        "Recipient Name",
        "Test Subject",
        "test-template",
        null,
        "tenant-123",
        "customer-123"
    );

    // When
    messagingService.publishNotificationEvent(event);

    // Then
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.NOTIFICATION_EMAIL_KEY),
        eq(event)
    );
  }

  @Test
  void publishNotificationEvent_shouldThrowMessagingExceptionOnFailure() {
    // Given
    NotificationEvent event = NotificationEvent.emailNotification(
        "recipient@example.com",
        "Recipient Name",
        "Test Subject",
        "test-template",
        null,
        "tenant-123",
        "customer-123"
    );
    doThrow(new RuntimeException("RabbitMQ error"))
        .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(NotificationEvent.class));

    // When & Then
    MessagingException exception = assertThrows(MessagingException.class, () ->
        messagingService.publishNotificationEvent(event)
    );
    assertEquals("Failed to publish notification event", exception.getMessage());
  }

  @Test
  void publishPaymentSuccessful_shouldCreateAndPublishPaymentSuccessfulEvent() {
    // Given
    String paymentId = "pay-123";
    String tenantId = "tenant-123";
    String customerEmail = "customer@example.com";

    // When
    messagingService.publishPaymentSuccessful(paymentId, tenantId, customerEmail);

    // Then
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.PAYMENT_SUCCESSFUL_KEY),
        eventCaptor.capture()
    );

    BillingEvent capturedEvent = eventCaptor.getValue();
    assertEquals("PAYMENT_SUCCESSFUL", capturedEvent.getEventType());
    assertEquals(paymentId, capturedEvent.getPaymentId());
    assertEquals(tenantId, capturedEvent.getTenantId());
    assertEquals(customerEmail, capturedEvent.getCustomerEmail());
  }

  @Test
  void publishPaymentFailed_shouldCreateAndPublishPaymentFailedEvent() {
    // Given
    String paymentId = "pay-456";
    String tenantId = "tenant-456";
    String customerEmail = "customer@example.com";

    // When
    messagingService.publishPaymentFailed(paymentId, tenantId, customerEmail);

    // Then
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.PAYMENT_FAILED_KEY),
        eventCaptor.capture()
    );

    BillingEvent capturedEvent = eventCaptor.getValue();
    assertEquals("PAYMENT_FAILED", capturedEvent.getEventType());
    assertEquals(paymentId, capturedEvent.getPaymentId());
    assertEquals(tenantId, capturedEvent.getTenantId());
    assertEquals(customerEmail, capturedEvent.getCustomerEmail());
  }

  @Test
  void publishPaymentRefunded_shouldCreateAndPublishPaymentRefundedEvent() {
    // Given
    String paymentId = "pay-789";
    String tenantId = "tenant-789";
    String customerEmail = "customer@example.com";

    // When
    messagingService.publishPaymentRefunded(paymentId, tenantId, customerEmail);

    // Then
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.PAYMENT_REFUNDED_KEY),
        eventCaptor.capture()
    );

    BillingEvent capturedEvent = eventCaptor.getValue();
    assertEquals("PAYMENT_REFUNDED", capturedEvent.getEventType());
    assertEquals(paymentId, capturedEvent.getPaymentId());
    assertEquals(tenantId, capturedEvent.getTenantId());
    assertEquals(customerEmail, capturedEvent.getCustomerEmail());
  }

  @Test
  void publishMerchantOnboarding_shouldCreateAndPublishMerchantOnboardingEvent() {
    // Given
    String merchantId = "merchant-101";
    String tenantId = "tenant-101";
    String merchantEmail = "merchant@example.com";

    // When
    messagingService.publishMerchantOnboarding(merchantId, tenantId, merchantEmail);

    // Then
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.MERCHANT_ONBOARDING_KEY),
        eventCaptor.capture()
    );

    BillingEvent capturedEvent = eventCaptor.getValue();
    assertEquals("MERCHANT_ONBOARDING", capturedEvent.getEventType());
    assertEquals(merchantId, capturedEvent.getMerchantId());
    assertEquals(tenantId, capturedEvent.getTenantId());
    assertEquals(merchantEmail, capturedEvent.getCustomerEmail());
  }

  @Test
  void publishInvoiceGenerated_shouldCreateAndPublishInvoiceGeneratedEvent() {
    // Given
    String invoiceId = "inv-202";
    String tenantId = "tenant-202";
    String customerEmail = "customer@example.com";

    // When
    messagingService.publishInvoiceGenerated(invoiceId, tenantId, customerEmail);

    // Then
    ArgumentCaptor<BillingEvent> eventCaptor = ArgumentCaptor.forClass(BillingEvent.class);
    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.EVENTS_EXCHANGE),
        eq(RabbitMQConfig.INVOICE_GENERATED_KEY),
        eventCaptor.capture()
    );

    BillingEvent capturedEvent = eventCaptor.getValue();
    assertEquals("INVOICE_GENERATED", capturedEvent.getEventType());
    assertEquals(invoiceId, capturedEvent.getInvoiceId());
    assertEquals(tenantId, capturedEvent.getTenantId());
    assertEquals(customerEmail, capturedEvent.getCustomerEmail());
  }
}
