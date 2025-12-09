package com.iqscaffold.billingservice.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.TestEntityUtils;
import com.iqscaffold.billingservice.invoice.Invoice;
import com.iqscaffold.billingservice.invoice.InvoiceLineItem;
import com.iqscaffold.billingservice.invoice.InvoiceRepository;
import com.iqscaffold.billingservice.payment.Payment;
import com.iqscaffold.billingservice.payment.PaymentProviderAdapter;
import com.iqscaffold.billingservice.payment.PaymentProviderFactory;
import com.iqscaffold.billingservice.payment.PaymentRepository;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodType;
import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Unit tests for WebhookApplicationService.
 * 
 * <p>Tests the orchestration logic of the webhook application service, verifying:
 * <ul>
 *   <li>Webhook signature verification</li>
 *   <li>Idempotency handling (duplicate event detection)</li>
 *   <li>Async processing pattern (queue publishing)</li>
 *   <li>Event handler delegation</li>
 *   <li>Domain event publishing</li>
 *   <li>Error handling and retry logic</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookApplicationService Unit Tests")
class WebhookApplicationServiceTest {

  @Mock
  private PaymentProviderFactory paymentProviderFactory;

  @Mock
  private WebhookEventRepository webhookEventRepository;

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private InvoiceRepository invoiceRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private DomainEventPublisher eventPublisher;

  @Mock
  private RabbitTemplate rabbitTemplate;

  @Mock
  private PaymentProviderAdapter paymentProviderAdapter;

  @InjectMocks
  private WebhookApplicationService webhookApplicationService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private UUID testTenantId;
  private Payment testPayment;
  private Invoice testInvoice;
  private PaymentMethod testPaymentMethod;

  @BeforeEach
  void setUp() {
    testTenantId = UUID.randomUUID();
    UUID testUserId = UUID.randomUUID();

    testPaymentMethod = new PaymentMethod(
        testTenantId,
        testUserId,
        PaymentMethodType.CARD,
        "pm_test123"
    );
    TestEntityUtils.setId(testPaymentMethod, 1L);

    // Create a subscription and plan for the invoice
    PlanQuotas testQuotas = new PlanQuotas(
        100L, 50L, 10000L, 5000L, 100L, 1000L, 5L, 10L
    );
    SubscriptionPlan testPlan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Plan",
        "Professional features",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("49.99"),
        "USD",
        Map.of("advanced_workflows", true),
        testQuotas,
        14,
        true
    );
    TestEntityUtils.setId(testPlan, 1L);

    Subscription testSubscription = Subscription.createActive(testTenantId, testUserId, testPlan);
    TestEntityUtils.setId(testSubscription, 1L);

    testInvoice = Invoice.createDraft(
        testSubscription,
        testTenantId,
        "INV-202412-001",
        "USD",
        LocalDateTime.now(),
        LocalDateTime.now().plusMonths(1),
        30
    );
    TestEntityUtils.setId(testInvoice, 1L);
    // Add a line item and finalize the invoice so it can be marked as paid
    testInvoice.addLineItem(InvoiceLineItem.subscriptionFee(
        "Pro Plan - Monthly",
        new BigDecimal("49.99")
    ));
    testInvoice.finalize();

    testPayment = new Payment(
        testInvoice,
        testTenantId,
        new BigDecimal("49.99"),
        "USD",
        testPaymentMethod
    );
    TestEntityUtils.setId(testPayment, 1L);
    TestEntityUtils.setField(testPayment, "providerPaymentId", "pi_test123");

    // Inject ObjectMapper into the service
    webhookApplicationService = new WebhookApplicationService(
        paymentProviderFactory,
        webhookEventRepository,
        paymentRepository,
        invoiceRepository,
        subscriptionRepository,
        eventPublisher,
        rabbitTemplate,
        objectMapper
    );
  }

  @Nested
  @DisplayName("verifyWebhookSignature Tests")
  class VerifyWebhookSignatureTests {

    @Test
    @DisplayName("Should verify webhook signature successfully")
    void shouldVerifyWebhookSignatureSuccessfully() {
      // Arrange
      String payload = "{\"id\":\"evt_test123\"}";
      String signature = "valid_signature";

      when(paymentProviderFactory.getProvider("stripe")).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.verifyWebhookSignature(payload, signature)).thenReturn(true);

      // Act
      boolean result = webhookApplicationService.verifyWebhookSignature(
          "stripe", payload, signature);

      // Assert
      assertThat(result).isTrue();
      verify(paymentProviderFactory).getProvider("stripe");
      verify(paymentProviderAdapter).verifyWebhookSignature(payload, signature);
    }

    @Test
    @DisplayName("Should return false when signature is invalid")
    void shouldReturnFalseWhenSignatureInvalid() {
      // Arrange
      String payload = "{\"id\":\"evt_test123\"}";
      String signature = "invalid_signature";

      when(paymentProviderFactory.getProvider("stripe")).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.verifyWebhookSignature(payload, signature)).thenReturn(false);

      // Act
      boolean result = webhookApplicationService.verifyWebhookSignature(
          "stripe", payload, signature);

      // Assert
      assertThat(result).isFalse();
      verify(paymentProviderAdapter).verifyWebhookSignature(payload, signature);
    }

    @Test
    @DisplayName("Should return false when provider throws exception")
    void shouldReturnFalseWhenProviderThrowsException() {
      // Arrange
      String payload = "{\"id\":\"evt_test123\"}";
      String signature = "signature";

      when(paymentProviderFactory.getProvider("stripe"))
          .thenThrow(new RuntimeException("Provider error"));

      // Act
      boolean result = webhookApplicationService.verifyWebhookSignature(
          "stripe", payload, signature);

      // Assert
      assertThat(result).isFalse();
      verify(paymentProviderFactory).getProvider("stripe");
    }
  }

  @Nested
  @DisplayName("processStripeWebhook Tests")
  class ProcessStripeWebhookTests {

    @Test
    @DisplayName("Should process Stripe webhook successfully")
    void shouldProcessStripeWebhookSuccessfully() {
      // Arrange
      String payload = """
          {
            "id": "evt_test123",
            "type": "payment_intent.succeeded"
          }
          """;
      String signature = "valid_signature";

      when(paymentProviderFactory.getProvider("stripe")).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.verifyWebhookSignature(payload, signature)).thenReturn(true);
      when(webhookEventRepository.existsByProviderEventId("evt_test123")).thenReturn(false);
      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      String result = webhookApplicationService.processStripeWebhook(payload, signature);

      // Assert
      assertThat(result).isEqualTo("evt_test123");
      verify(paymentProviderAdapter).verifyWebhookSignature(payload, signature);
      verify(webhookEventRepository).existsByProviderEventId("evt_test123");
      verify(webhookEventRepository).save(any(WebhookEvent.class));
      verify(rabbitTemplate).convertAndSend(
          eq("billing.webhooks"),
          eq("stripe.payment_intent_succeeded"),
          any(Object.class)
      );
    }

    @Test
    @DisplayName("Should return event ID when webhook already processed (idempotency)")
    void shouldReturnEventIdWhenAlreadyProcessed() {
      // Arrange
      String payload = """
          {
            "id": "evt_test123",
            "type": "payment_intent.succeeded"
          }
          """;
      String signature = "valid_signature";

      when(paymentProviderFactory.getProvider("stripe")).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.verifyWebhookSignature(payload, signature)).thenReturn(true);
      when(webhookEventRepository.existsByProviderEventId("evt_test123")).thenReturn(true);

      // Act
      String result = webhookApplicationService.processStripeWebhook(payload, signature);

      // Assert
      assertThat(result).isEqualTo("evt_test123");
      verify(webhookEventRepository).existsByProviderEventId("evt_test123");
      verify(webhookEventRepository, never()).save(any());
      verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should throw exception when signature is invalid")
    void shouldThrowExceptionWhenSignatureInvalid() {
      // Arrange
      String payload = "{\"id\":\"evt_test123\"}";
      String signature = "invalid_signature";

      when(paymentProviderFactory.getProvider("stripe")).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.verifyWebhookSignature(payload, signature)).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> webhookApplicationService.processStripeWebhook(payload, signature))
          .isInstanceOf(PaymentException.class)
          .hasMessageContaining("Invalid webhook signature");

      verify(paymentProviderAdapter).verifyWebhookSignature(payload, signature);
      verify(webhookEventRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("processPayPalWebhook Tests")
  class ProcessPayPalWebhookTests {

    @Test
    @DisplayName("Should process PayPal webhook successfully")
    void shouldProcessPayPalWebhookSuccessfully() {
      // Arrange
      String payload = """
          {
            "id": "evt_paypal123",
            "event_type": "PAYMENT.CAPTURE.COMPLETED"
          }
          """;
      String signature = "valid_signature";

      when(paymentProviderFactory.getProvider("paypal")).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.verifyWebhookSignature(payload, signature)).thenReturn(true);
      when(webhookEventRepository.existsByProviderEventId("evt_paypal123")).thenReturn(false);
      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      String result = webhookApplicationService.processPayPalWebhook(payload, signature);

      // Assert
      assertThat(result).isEqualTo("evt_paypal123");
      verify(paymentProviderAdapter).verifyWebhookSignature(payload, signature);
      verify(webhookEventRepository).existsByProviderEventId("evt_paypal123");
      verify(webhookEventRepository).save(any(WebhookEvent.class));
      verify(rabbitTemplate).convertAndSend(
          eq("billing.webhooks"),
          eq("paypal.PAYMENT_CAPTURE_COMPLETED"),
          any(Object.class)
      );
    }

    @Test
    @DisplayName("Should throw exception when signature is invalid")
    void shouldThrowExceptionWhenSignatureInvalid() {
      // Arrange
      String payload = "{\"id\":\"evt_paypal123\"}";
      String signature = "invalid_signature";

      when(paymentProviderFactory.getProvider("paypal")).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.verifyWebhookSignature(payload, signature)).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> webhookApplicationService.processPayPalWebhook(payload, signature))
          .isInstanceOf(PaymentException.class)
          .hasMessageContaining("Invalid webhook signature");

      verify(paymentProviderAdapter).verifyWebhookSignature(payload, signature);
      verify(webhookEventRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("handlePaymentSucceeded Tests")
  class HandlePaymentSucceededTests {

    @Test
    @DisplayName("Should handle payment succeeded event successfully")
    void shouldHandlePaymentSucceededSuccessfully() {
      // Arrange
      String payload = """
          {
            "id": "evt_test123",
            "type": "payment_intent.succeeded",
            "data": {
              "object": {
                "id": "pi_test123",
                "amount": "4999",
                "currency": "usd"
              }
            }
          }
          """;

      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId("evt_test123")
          .provider("stripe")
          .eventType("payment_intent.succeeded")
          .payload(payload)
          .status(WebhookEventStatus.PENDING)
          .retryCount(0)
          .build();

      when(paymentRepository.findByProviderPaymentId("pi_test123"))
          .thenReturn(Optional.of(testPayment));
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      webhookApplicationService.handlePaymentSucceeded(webhookEvent);

      // Assert
      verify(paymentRepository).findByProviderPaymentId("pi_test123");
      verify(paymentRepository).save(testPayment);
      verify(invoiceRepository).save(testInvoice);
      verify(eventPublisher, times(2)).publish(any()); // InvoicePaid + PaymentSucceeded
      verify(webhookEventRepository).save(webhookEvent);
      assertThat(webhookEvent.getStatus()).isEqualTo(WebhookEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("Should mark webhook as failed when payment not found")
    void shouldMarkWebhookAsFailedWhenPaymentNotFound() {
      // Arrange
      String payload = """
          {
            "id": "evt_test123",
            "type": "payment_intent.succeeded",
            "data": {
              "object": {
                "id": "pi_unknown",
                "amount": "4999",
                "currency": "usd"
              }
            }
          }
          """;

      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId("evt_test123")
          .provider("stripe")
          .eventType("payment_intent.succeeded")
          .payload(payload)
          .status(WebhookEventStatus.PENDING)
          .retryCount(0)
          .build();

      when(paymentRepository.findByProviderPaymentId("pi_unknown"))
          .thenReturn(Optional.empty());
      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      webhookApplicationService.handlePaymentSucceeded(webhookEvent);

      // Assert
      verify(paymentRepository).findByProviderPaymentId("pi_unknown");
      verify(paymentRepository, never()).save(any());
      verify(eventPublisher, never()).publish(any());
      verify(webhookEventRepository).save(webhookEvent);
      assertThat(webhookEvent.getStatus()).isEqualTo(WebhookEventStatus.FAILED);
      assertThat(webhookEvent.getErrorMessage()).isEqualTo("Payment not found");
    }
  }

  @Nested
  @DisplayName("handlePaymentFailed Tests")
  class HandlePaymentFailedTests {

    @Test
    @DisplayName("Should handle payment failed event successfully")
    void shouldHandlePaymentFailedSuccessfully() {
      // Arrange
      String payload = """
          {
            "id": "evt_test123",
            "type": "payment_intent.failed",
            "data": {
              "object": {
                "id": "pi_test123",
                "last_payment_error": {
                  "message": "Insufficient funds"
                }
              }
            }
          }
          """;

      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId("evt_test123")
          .provider("stripe")
          .eventType("payment_intent.failed")
          .payload(payload)
          .status(WebhookEventStatus.PENDING)
          .retryCount(0)
          .build();

      when(paymentRepository.findByProviderPaymentId("pi_test123"))
          .thenReturn(Optional.of(testPayment));
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      webhookApplicationService.handlePaymentFailed(webhookEvent);

      // Assert
      verify(paymentRepository).findByProviderPaymentId("pi_test123");
      verify(paymentRepository).save(testPayment);
      verify(eventPublisher).publish(any()); // PaymentFailed event
      verify(webhookEventRepository).save(webhookEvent);
      assertThat(webhookEvent.getStatus()).isEqualTo(WebhookEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("Should mark webhook as failed when payment not found")
    void shouldMarkWebhookAsFailedWhenPaymentNotFound() {
      // Arrange
      String payload = """
          {
            "id": "evt_test123",
            "type": "payment_intent.failed",
            "data": {
              "object": {
                "id": "pi_unknown",
                "last_payment_error": {
                  "message": "Card declined"
                }
              }
            }
          }
          """;

      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId("evt_test123")
          .provider("stripe")
          .eventType("payment_intent.failed")
          .payload(payload)
          .status(WebhookEventStatus.PENDING)
          .retryCount(0)
          .build();

      when(paymentRepository.findByProviderPaymentId("pi_unknown"))
          .thenReturn(Optional.empty());
      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      webhookApplicationService.handlePaymentFailed(webhookEvent);

      // Assert
      verify(paymentRepository).findByProviderPaymentId("pi_unknown");
      verify(paymentRepository, never()).save(any());
      verify(eventPublisher, never()).publish(any());
      verify(webhookEventRepository).save(webhookEvent);
      assertThat(webhookEvent.getStatus()).isEqualTo(WebhookEventStatus.FAILED);
    }
  }

  @Nested
  @DisplayName("handleChargeRefunded Tests")
  class HandleChargeRefundedTests {

    @Test
    @DisplayName("Should handle charge refunded event successfully")
    void shouldHandleChargeRefundedSuccessfully() {
      // Arrange
      testPayment.markAsSucceeded("pi_test123");
      String payload = """
          {
            "id": "evt_test123",
            "type": "charge.refunded",
            "data": {
              "object": {
                "id": "ch_test123",
                "payment_intent": "pi_test123",
                "amount_refunded": "4999",
                "currency": "usd"
              }
            }
          }
          """;

      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId("evt_test123")
          .provider("stripe")
          .eventType("charge.refunded")
          .payload(payload)
          .status(WebhookEventStatus.PENDING)
          .retryCount(0)
          .build();

      when(paymentRepository.findByProviderPaymentId("pi_test123"))
          .thenReturn(Optional.of(testPayment));
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      webhookApplicationService.handleChargeRefunded(webhookEvent);

      // Assert
      verify(paymentRepository).findByProviderPaymentId("pi_test123");
      verify(paymentRepository).save(testPayment);
      verify(eventPublisher).publish(any()); // PaymentRefunded event
      verify(webhookEventRepository).save(webhookEvent);
      assertThat(webhookEvent.getStatus()).isEqualTo(WebhookEventStatus.PROCESSED);
    }
  }

  @Nested
  @DisplayName("handleSubscriptionUpdated Tests")
  class HandleSubscriptionUpdatedTests {

    @Test
    @DisplayName("Should handle subscription updated event successfully")
    void shouldHandleSubscriptionUpdatedSuccessfully() {
      // Arrange
      String payload = """
          {
            "id": "evt_test123",
            "type": "customer.subscription.updated",
            "data": {
              "object": {
                "id": "sub_test123",
                "status": "active"
              }
            }
          }
          """;

      WebhookEvent webhookEvent = WebhookEvent.builder()
          .providerEventId("evt_test123")
          .provider("stripe")
          .eventType("customer.subscription.updated")
          .payload(payload)
          .status(WebhookEventStatus.PENDING)
          .retryCount(0)
          .build();

      when(webhookEventRepository.save(any(WebhookEvent.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      webhookApplicationService.handleSubscriptionUpdated(webhookEvent);

      // Assert
      verify(webhookEventRepository).save(webhookEvent);
      assertThat(webhookEvent.getStatus()).isEqualTo(WebhookEventStatus.PROCESSED);
    }
  }
}
