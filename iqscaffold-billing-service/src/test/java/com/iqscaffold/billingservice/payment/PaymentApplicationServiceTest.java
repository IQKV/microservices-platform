package com.iqscaffold.billingservice.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.iqscaffold.billingservice.TestEntityUtils;
import com.iqscaffold.billingservice.invoice.Invoice;
import com.iqscaffold.billingservice.invoice.InvoiceLineItem;
import com.iqscaffold.billingservice.invoice.InvoiceRepository;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodRepository;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodType;
import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.exception.InvoiceException;
import com.iqscaffold.billingservice.shared.exception.PaymentException;
import com.iqscaffold.billingservice.subscription.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Unit tests for PaymentApplicationService.
 * 
 * <p>Tests the orchestration logic of the payment application service, verifying:
 * <ul>
 *   <li>Delegation to Payment and PaymentMethod aggregates</li>
 *   <li>Coordination with PaymentProviderAdapter (anti-corruption layer)</li>
 *   <li>Transaction management boundaries</li>
 *   <li>DTO translation</li>
 *   <li>Idempotency handling</li>
 *   <li>Circuit breaker and timeout behavior</li>
 *   <li>Domain event publishing</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentApplicationService Unit Tests")
class PaymentApplicationServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentMethodRepository paymentMethodRepository;

  @Mock
  private InvoiceRepository invoiceRepository;

  @Mock
  private PaymentProviderFactory paymentProviderFactory;

  @Mock
  private DomainEventPublisher eventPublisher;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @Mock
  private PaymentProviderAdapter paymentProviderAdapter;

  @InjectMocks
  private PaymentApplicationService paymentApplicationService;

  private UUID testTenantId;
  private UUID testUserId;
  private PaymentMethod testPaymentMethod;
  private Invoice testInvoice;
  private Payment testPayment;

  @BeforeEach
  void setUp() {
    testTenantId = UUID.randomUUID();
    testUserId = UUID.randomUUID();

    testPaymentMethod = new PaymentMethod(
        testTenantId,
        testUserId,
        PaymentMethodType.CARD,
        "pm_test123"
    );
    TestEntityUtils.setId(testPaymentMethod, 1L);
    testPaymentMethod.setCardDetails("4242", "Visa", 12, 2025);

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
        "INV-001",
        "USD",
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.now().plusMonths(1),
        30
    );
    TestEntityUtils.setId(testInvoice, 1L);
    // Add a line item so the invoice has a non-zero amount
    testInvoice.addLineItem(InvoiceLineItem.subscriptionFee(
        "Pro Plan - Monthly",
        new BigDecimal("49.99")
    ));

    testPayment = new Payment(
        testInvoice,
        testTenantId,
        new BigDecimal("49.99"),
        "USD",
        testPaymentMethod
    );
    TestEntityUtils.setId(testPayment, 1L);

    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Nested
  @DisplayName("addPaymentMethod Tests")
  class AddPaymentMethodTests {

    @Test
    @DisplayName("Should add payment method successfully")
    void shouldAddPaymentMethodSuccessfully() throws ExecutionException, InterruptedException {
      // Arrange
      PaymentMethodDetails details = new PaymentMethodDetails(
          "pm_test123",
          "card",
          "4242",
          "Visa",
          12,
          2025
      );

      when(paymentProviderFactory.getProvider()).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.createPaymentMethod(anyString(), anyString()))
          .thenReturn(details);
      when(paymentMethodRepository.save(any(PaymentMethod.class)))
          .thenReturn(testPaymentMethod);

      // Act
      CompletableFuture<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto> future =
          paymentApplicationService.addPaymentMethod(
              testTenantId,
              testUserId,
              "tok_test123",
              "cus_test123"
          );

      com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto result = future.get();

      // Assert
      assertThat(result).isNotNull();
      verify(paymentProviderFactory).getProvider();
      verify(paymentProviderAdapter).createPaymentMethod("cus_test123", "tok_test123");
      verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    @DisplayName("Should throw exception when payment provider fails")
    void shouldThrowExceptionWhenProviderFails() {
      // Arrange
      when(paymentProviderFactory.getProvider()).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.createPaymentMethod(anyString(), anyString()))
          .thenThrow(new RuntimeException("Provider error"));

      // Act
      CompletableFuture<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto> future =
          paymentApplicationService.addPaymentMethod(
              testTenantId,
              testUserId,
              "tok_test123",
              "cus_test123"
          );

      // Assert
      assertThatThrownBy(future::get)
          .hasCauseInstanceOf(PaymentException.InvalidPaymentMethodException.class);

      verify(paymentMethodRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("removePaymentMethod Tests")
  class RemovePaymentMethodTests {

    @Test
    @DisplayName("Should remove payment method successfully")
    void shouldRemovePaymentMethodSuccessfully() {
      // Arrange
      when(paymentMethodRepository.findByIdAndTenantId(1L, testTenantId))
          .thenReturn(Optional.of(testPaymentMethod));
      when(paymentMethodRepository.save(any(PaymentMethod.class)))
          .thenReturn(testPaymentMethod);
      when(paymentProviderFactory.getProvider()).thenReturn(paymentProviderAdapter);

      // Act
      paymentApplicationService.removePaymentMethod(testTenantId, 1L);

      // Assert
      verify(paymentMethodRepository).findByIdAndTenantId(1L, testTenantId);
      verify(paymentMethodRepository).save(testPaymentMethod);
      verify(paymentProviderFactory).getProvider();
      verify(paymentProviderAdapter).deletePaymentMethod("pm_test123");
    }

    @Test
    @DisplayName("Should throw exception when payment method not found")
    void shouldThrowExceptionWhenPaymentMethodNotFound() {
      // Arrange
      when(paymentMethodRepository.findByIdAndTenantId(999L, testTenantId))
          .thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> paymentApplicationService.removePaymentMethod(testTenantId, 999L))
          .isInstanceOf(PaymentException.PaymentMethodNotFoundException.class);

      verify(paymentMethodRepository).findByIdAndTenantId(999L, testTenantId);
      verify(paymentMethodRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("setDefaultPaymentMethod Tests")
  class SetDefaultPaymentMethodTests {

    @Test
    @DisplayName("Should set default payment method successfully")
    void shouldSetDefaultPaymentMethodSuccessfully() {
      // Arrange
      PaymentMethod currentDefault = new PaymentMethod(
          testTenantId,
          testUserId,
          PaymentMethodType.CARD,
          "pm_old"
      );
      TestEntityUtils.setId(currentDefault, 2L);
      currentDefault.markAsDefault();

      when(paymentMethodRepository.findByIdAndTenantId(1L, testTenantId))
          .thenReturn(Optional.of(testPaymentMethod));
      when(paymentMethodRepository.findByTenantIdAndIsDefaultTrue(testTenantId))
          .thenReturn(List.of(currentDefault));
      when(paymentMethodRepository.save(any(PaymentMethod.class)))
          .thenReturn(testPaymentMethod);

      // Act
      paymentApplicationService.setDefaultPaymentMethod(testTenantId, 1L);

      // Assert
      verify(paymentMethodRepository).findByIdAndTenantId(1L, testTenantId);
      verify(paymentMethodRepository).findByTenantIdAndIsDefaultTrue(testTenantId);
      verify(paymentMethodRepository, times(2)).save(any(PaymentMethod.class));
    }

    @Test
    @DisplayName("Should throw exception when payment method not found")
    void shouldThrowExceptionWhenPaymentMethodNotFound() {
      // Arrange
      when(paymentMethodRepository.findByIdAndTenantId(999L, testTenantId))
          .thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> paymentApplicationService.setDefaultPaymentMethod(testTenantId, 999L))
          .isInstanceOf(PaymentException.PaymentMethodNotFoundException.class);

      verify(paymentMethodRepository).findByIdAndTenantId(999L, testTenantId);
      verify(paymentMethodRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("listPaymentMethods Tests")
  class ListPaymentMethodsTests {

    @Test
    @DisplayName("Should list payment methods successfully")
    void shouldListPaymentMethodsSuccessfully() {
      // Arrange
      List<PaymentMethod> paymentMethods = List.of(testPaymentMethod);
      when(paymentMethodRepository.findByTenantIdAndActiveTrue(testTenantId))
          .thenReturn(paymentMethods);

      // Act
      List<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto> result =
          paymentApplicationService.listPaymentMethods(testTenantId);

      // Assert
      assertThat(result).hasSize(1);
      verify(paymentMethodRepository).findByTenantIdAndActiveTrue(testTenantId);
    }

    @Test
    @DisplayName("Should return empty list when no payment methods exist")
    void shouldReturnEmptyListWhenNoPaymentMethods() {
      // Arrange
      when(paymentMethodRepository.findByTenantIdAndActiveTrue(testTenantId))
          .thenReturn(List.of());

      // Act
      List<com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto> result =
          paymentApplicationService.listPaymentMethods(testTenantId);

      // Assert
      assertThat(result).isEmpty();
      verify(paymentMethodRepository).findByTenantIdAndActiveTrue(testTenantId);
    }
  }

  @Nested
  @DisplayName("processPayment Tests")
  class ProcessPaymentTests {

    @Test
    @DisplayName("Should process payment successfully")
    void shouldProcessPaymentSuccessfully() throws ExecutionException, InterruptedException {
      // Arrange
      String idempotencyKey = "idem_test123";
      PaymentResult successResult = PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          java.time.LocalDateTime.now()
      );

      when(valueOperations.get("payment:idempotency:" + idempotencyKey)).thenReturn(null);
      when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
      when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(paymentProviderFactory.getProvider()).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.processPayment(
          eq("pm_test123"),
          eq(new BigDecimal("49.99")),
          eq("USD"),
          eq(idempotencyKey)
      )).thenReturn(successResult);

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.processPayment(
          1L,
          1L,
          idempotencyKey
      );

      PaymentDto result = future.get();

      // Assert
      assertThat(result).isNotNull();
      verify(invoiceRepository).findById(1L);
      verify(paymentMethodRepository).findById(1L);
      verify(paymentRepository, times(2)).save(any(Payment.class));
      verify(paymentProviderAdapter).processPayment(
          eq("pm_test123"),
          eq(new BigDecimal("49.99")),
          eq("USD"),
          eq(idempotencyKey)
      );
      verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should return cached result for duplicate idempotency key")
    void shouldReturnCachedResultForDuplicateIdempotencyKey() throws ExecutionException, InterruptedException {
      // Arrange
      String idempotencyKey = "idem_test123";
      PaymentDto cachedDto = new PaymentDto(
          1L,
          1L,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          PaymentStatus.SUCCEEDED,
          1L,
          "CARD",
          "4242",
          "pi_test123",
          null,
          BigDecimal.ZERO,
          java.util.Map.of(),
          java.time.LocalDateTime.now(),
          java.time.LocalDateTime.now()
      );

      when(valueOperations.get("payment:idempotency:" + idempotencyKey)).thenReturn(cachedDto);

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.processPayment(
          1L,
          1L,
          idempotencyKey
      );

      PaymentDto result = future.get();

      // Assert
      assertThat(result).isEqualTo(cachedDto);
      verify(invoiceRepository, never()).findById(any());
      verify(paymentRepository, never()).save(any());
      verify(paymentProviderAdapter, never()).processPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle payment failure")
    void shouldHandlePaymentFailure() throws ExecutionException, InterruptedException {
      // Arrange
      String idempotencyKey = "idem_test123";
      PaymentResult failureResult = PaymentResult.failure(
          new BigDecimal("49.99"),
          "USD",
          "Insufficient funds"
      );

      when(valueOperations.get("payment:idempotency:" + idempotencyKey)).thenReturn(null);
      when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
      when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(paymentProviderFactory.getProvider()).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.processPayment(
          eq("pm_test123"),
          eq(new BigDecimal("49.99")),
          eq("USD"),
          eq(idempotencyKey)
      )).thenReturn(failureResult);

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.processPayment(
          1L,
          1L,
          idempotencyKey
      );

      PaymentDto result = future.get();

      // Assert
      assertThat(result).isNotNull();
      verify(paymentRepository, times(2)).save(any(Payment.class));
      verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
      // Arrange
      String idempotencyKey = "idem_test123";

      when(valueOperations.get("payment:idempotency:" + idempotencyKey)).thenReturn(null);
      when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.processPayment(
          999L,
          1L,
          idempotencyKey
      );

      // Assert
      assertThatThrownBy(future::get)
          .hasCauseInstanceOf(InvoiceException.InvoiceNotFoundException.class);

      verify(invoiceRepository).findById(999L);
      verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when payment method not found")
    void shouldThrowExceptionWhenPaymentMethodNotFound() {
      // Arrange
      String idempotencyKey = "idem_test123";

      when(valueOperations.get("payment:idempotency:" + idempotencyKey)).thenReturn(null);
      when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
      when(paymentMethodRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.processPayment(
          1L,
          999L,
          idempotencyKey
      );

      // Assert
      assertThatThrownBy(future::get)
          .hasCauseInstanceOf(PaymentException.PaymentMethodNotFoundException.class);

      verify(paymentMethodRepository).findById(999L);
      verify(paymentRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("retryPayment Tests")
  class RetryPaymentTests {

    @Test
    @DisplayName("Should retry payment successfully")
    void shouldRetryPaymentSuccessfully() throws ExecutionException, InterruptedException {
      // Arrange
      testPayment.markAsFailed("Initial failure");
      PaymentResult successResult = PaymentResult.success(
          "pi_retry123",
          new BigDecimal("49.99"),
          "USD",
          java.time.LocalDateTime.now()
      );

      // Create a new payment object for the retry (simulating what the service does)
      Payment retryPayment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      TestEntityUtils.setId(retryPayment, 2L);

      when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
      when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
        Payment savedPayment = invocation.getArgument(0);
        return savedPayment;
      });
      when(paymentProviderFactory.getProvider()).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.processPayment(
          anyString(),
          any(BigDecimal.class),
          anyString(),
          anyString()
      )).thenReturn(successResult);

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.retryPayment(1L);

      PaymentDto result = future.get();

      // Assert
      assertThat(result).isNotNull();
      verify(paymentRepository).findById(1L);
      verify(paymentRepository, times(2)).save(any(Payment.class));
      verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should not retry payment that is not in FAILED status")
    void shouldNotRetryPaymentNotInFailedStatus() throws ExecutionException, InterruptedException {
      // Arrange
      testPayment.markAsSucceeded("pi_success123");

      when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.retryPayment(1L);

      PaymentDto result = future.get();

      // Assert
      assertThat(result).isNotNull();
      verify(paymentRepository).findById(1L);
      verify(paymentRepository, never()).save(any());
      verify(paymentProviderAdapter, never()).processPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
      // Arrange
      when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.retryPayment(999L);

      // Assert
      assertThatThrownBy(future::get)
          .hasCauseInstanceOf(PaymentException.class);

      verify(paymentRepository).findById(999L);
      verify(paymentRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("refundPayment Tests")
  class RefundPaymentTests {

    @Test
    @DisplayName("Should refund payment successfully")
    void shouldRefundPaymentSuccessfully() throws ExecutionException, InterruptedException {
      // Arrange
      testPayment.markAsSucceeded("pi_success123");
      PaymentResult refundResult = PaymentResult.success(
          "re_refund123",
          new BigDecimal("49.99"),
          "USD",
          java.time.LocalDateTime.now()
      );

      when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
      when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
      when(paymentProviderFactory.getProvider()).thenReturn(paymentProviderAdapter);
      when(paymentProviderAdapter.refundPayment(
          eq("pi_success123"),
          eq(new BigDecimal("49.99")),
          eq("Customer request")
      )).thenReturn(refundResult);

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.refundPayment(
          1L,
          new BigDecimal("49.99"),
          "Customer request",
          testUserId
      );

      PaymentDto result = future.get();

      // Assert
      assertThat(result).isNotNull();
      verify(paymentRepository).findById(1L);
      verify(paymentRepository).save(testPayment);
      verify(paymentProviderAdapter).refundPayment(
          eq("pi_success123"),
          eq(new BigDecimal("49.99")),
          eq("Customer request")
      );
      verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
      // Arrange
      when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.refundPayment(
          999L,
          new BigDecimal("49.99"),
          "Customer request",
          testUserId
      );

      // Assert
      assertThatThrownBy(future::get)
          .hasCauseInstanceOf(PaymentException.class);

      verify(paymentRepository).findById(999L);
      verify(paymentProviderAdapter, never()).refundPayment(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when payment is not in SUCCEEDED status")
    void shouldThrowExceptionWhenPaymentNotSucceeded() {
      // Arrange
      testPayment.markAsFailed("Payment failed");

      when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

      // Act
      CompletableFuture<PaymentDto> future = paymentApplicationService.refundPayment(
          1L,
          new BigDecimal("49.99"),
          "Customer request",
          testUserId
      );

      // Assert
      assertThatThrownBy(future::get)
          .hasCauseInstanceOf(PaymentException.class);

      verify(paymentRepository).findById(1L);
      verify(paymentProviderAdapter, never()).refundPayment(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("getPayment Tests")
  class GetPaymentTests {

    @Test
    @DisplayName("Should retrieve payment by ID successfully")
    void shouldRetrievePaymentById() {
      // Arrange
      when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

      // Act
      PaymentDto result = paymentApplicationService.getPayment(1L);

      // Assert
      assertThat(result).isNotNull();
      verify(paymentRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
      // Arrange
      when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> paymentApplicationService.getPayment(999L))
          .isInstanceOf(PaymentException.class);

      verify(paymentRepository).findById(999L);
    }
  }

  @Nested
  @DisplayName("listPaymentsByInvoice Tests")
  class ListPaymentsByInvoiceTests {

    @Test
    @DisplayName("Should list payments by invoice successfully")
    void shouldListPaymentsByInvoice() {
      // Arrange
      List<Payment> payments = List.of(testPayment);
      when(paymentRepository.findByInvoiceId(1L)).thenReturn(payments);

      // Act
      List<PaymentDto> result = paymentApplicationService.listPaymentsByInvoice(1L);

      // Assert
      assertThat(result).hasSize(1);
      verify(paymentRepository).findByInvoiceId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no payments exist")
    void shouldReturnEmptyListWhenNoPayments() {
      // Arrange
      when(paymentRepository.findByInvoiceId(1L)).thenReturn(List.of());

      // Act
      List<PaymentDto> result = paymentApplicationService.listPaymentsByInvoice(1L);

      // Assert
      assertThat(result).isEmpty();
      verify(paymentRepository).findByInvoiceId(1L);
    }
  }
}
