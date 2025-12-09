package com.iqscaffold.billingservice.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iqscaffold.billingservice.TestEntityUtils;
import com.iqscaffold.billingservice.invoice.Invoice;
import com.iqscaffold.billingservice.invoice.InvoiceLineItem;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodType;
import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.Subscription;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Payment domain entity.
 * 
 * <p>Tests payment business logic including:
 * <ul>
 *   <li>Payment creation and validation</li>
 *   <li>Payment status transitions (PENDING → SUCCEEDED/FAILED)</li>
 *   <li>Refund processing (full and partial)</li>
 *   <li>Aggregate invariants enforcement</li>
 *   <li>Idempotency considerations</li>
 * </ul>
 */
@DisplayName("Payment Domain Entity Tests")
class PaymentTest {

  private UUID testTenantId;
  private UUID testUserId;
  private Invoice testInvoice;
  private PaymentMethod testPaymentMethod;

  @BeforeEach
  void setUp() {
    testTenantId = UUID.randomUUID();
    testUserId = UUID.randomUUID();

    // Create test payment method
    testPaymentMethod = new PaymentMethod(
        testTenantId,
        testUserId,
        PaymentMethodType.CARD,
        "pm_test123"
    );
    TestEntityUtils.setId(testPaymentMethod, 1L);
    testPaymentMethod.setCardDetails("4242", "Visa", 12, 2025);

    // Create test subscription and plan
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

    // Create test invoice
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
    testInvoice.addLineItem(InvoiceLineItem.subscriptionFee(
        "Pro Plan - Monthly",
        new BigDecimal("49.99")
    ));
  }

  @Nested
  @DisplayName("Payment Creation Tests")
  class PaymentCreationTests {

    @Test
    @DisplayName("Should create payment with valid parameters")
    void shouldCreatePaymentWithValidParameters() {
      // Act
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );

      // Assert
      assertThat(payment).isNotNull();
      assertThat(payment.getInvoice()).isEqualTo(testInvoice);
      assertThat(payment.getTenantId()).isEqualTo(testTenantId);
      assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(payment.getCurrency()).isEqualTo("USD");
      assertThat(payment.getPaymentMethod()).isEqualTo(testPaymentMethod);
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
      assertThat(payment.getRefundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(payment.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when invoice is null")
    void shouldThrowExceptionWhenInvoiceIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> new Payment(
          null,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invoice cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when tenant ID is null")
    void shouldThrowExceptionWhenTenantIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> new Payment(
          testInvoice,
          null,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Tenant ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> new Payment(
          testInvoice,
          testTenantId,
          null,
          "USD",
          testPaymentMethod
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when amount is zero")
    void shouldThrowExceptionWhenAmountIsZero() {
      // Act & Assert
      assertThatThrownBy(() -> new Payment(
          testInvoice,
          testTenantId,
          BigDecimal.ZERO,
          "USD",
          testPaymentMethod
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount must be positive");
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
      // Act & Assert
      assertThatThrownBy(() -> new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("-10.00"),
          "USD",
          testPaymentMethod
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount must be positive");
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowExceptionWhenCurrencyIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          null,
          testPaymentMethod
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Currency cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when currency is blank")
    void shouldThrowExceptionWhenCurrencyIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "   ",
          testPaymentMethod
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Currency cannot be null or empty");
    }
  }

  @Nested
  @DisplayName("Payment Success Tests")
  class PaymentSuccessTests {

    @Test
    @DisplayName("Should mark payment as succeeded")
    void shouldMarkPaymentAsSucceeded() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );

      // Act
      payment.markAsSucceeded("pi_test123");

      // Assert
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
      assertThat(payment.getProviderPaymentId()).isEqualTo("pi_test123");
      assertThat(payment.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when marking non-pending payment as succeeded")
    void shouldThrowExceptionWhenMarkingNonPendingAsSucceeded() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsFailed("Initial failure");

      // Act & Assert
      assertThatThrownBy(() -> payment.markAsSucceeded("pi_test123"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Can only mark PENDING payments as succeeded");
    }

    @Test
    @DisplayName("Should not allow marking succeeded payment as succeeded again")
    void shouldNotAllowMarkingSucceededPaymentAsSucceededAgain() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act & Assert
      assertThatThrownBy(() -> payment.markAsSucceeded("pi_test456"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Can only mark PENDING payments as succeeded");
    }
  }

  @Nested
  @DisplayName("Payment Failure Tests")
  class PaymentFailureTests {

    @Test
    @DisplayName("Should mark payment as failed")
    void shouldMarkPaymentAsFailed() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );

      // Act
      payment.markAsFailed("Insufficient funds");

      // Assert
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
      assertThat(payment.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("Should throw exception when marking non-pending payment as failed")
    void shouldThrowExceptionWhenMarkingNonPendingAsFailed() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act & Assert
      assertThatThrownBy(() -> payment.markAsFailed("Some reason"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Can only mark PENDING payments as failed");
    }

    @Test
    @DisplayName("Should not allow marking failed payment as failed again")
    void shouldNotAllowMarkingFailedPaymentAsFailedAgain() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsFailed("Initial failure");

      // Act & Assert
      assertThatThrownBy(() -> payment.markAsFailed("Another failure"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Can only mark PENDING payments as failed");
    }
  }

  @Nested
  @DisplayName("Refund Processing Tests")
  class RefundProcessingTests {

    @Test
    @DisplayName("Should process full refund successfully")
    void shouldProcessFullRefundSuccessfully() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act
      payment.processRefund(new BigDecimal("49.99"));

      // Assert
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
      assertThat(payment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(payment.isFullyRefunded()).isTrue();
      assertThat(payment.isPartiallyRefunded()).isFalse();
      assertThat(payment.getRefundableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should process partial refund successfully")
    void shouldProcessPartialRefundSuccessfully() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act
      payment.processRefund(new BigDecimal("20.00"));

      // Assert
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
      assertThat(payment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
      assertThat(payment.isFullyRefunded()).isFalse();
      assertThat(payment.isPartiallyRefunded()).isTrue();
      assertThat(payment.getRefundableAmount()).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    @DisplayName("Should process multiple partial refunds")
    void shouldProcessMultiplePartialRefunds() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("100.00"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act
      payment.processRefund(new BigDecimal("30.00"));
      payment.processRefund(new BigDecimal("20.00"));
      payment.processRefund(new BigDecimal("50.00"));

      // Assert
      assertThat(payment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
      assertThat(payment.isFullyRefunded()).isTrue();
      assertThat(payment.getRefundableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw exception when refunding non-succeeded payment")
    void shouldThrowExceptionWhenRefundingNonSucceededPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      // Payment is still PENDING

      // Act & Assert
      assertThatThrownBy(() -> payment.processRefund(new BigDecimal("49.99")))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Can only refund SUCCEEDED or partially REFUNDED payments");
    }

    @Test
    @DisplayName("Should throw exception when refunding failed payment")
    void shouldThrowExceptionWhenRefundingFailedPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsFailed("Card declined");

      // Act & Assert
      assertThatThrownBy(() -> payment.processRefund(new BigDecimal("49.99")))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Can only refund SUCCEEDED or partially REFUNDED payments");
    }

    @Test
    @DisplayName("Should throw exception when refund amount exceeds payment amount")
    void shouldThrowExceptionWhenRefundAmountExceedsPaymentAmount() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act & Assert
      assertThatThrownBy(() -> payment.processRefund(new BigDecimal("50.00")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Refund amount")
          .hasMessageContaining("would exceed original payment amount");
    }

    @Test
    @DisplayName("Should throw exception when total refunds exceed payment amount")
    void shouldThrowExceptionWhenTotalRefundsExceedPaymentAmount() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");
      payment.processRefund(new BigDecimal("30.00"));

      // Act & Assert
      assertThatThrownBy(() -> payment.processRefund(new BigDecimal("20.00")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("would exceed original payment amount");
    }

    @Test
    @DisplayName("Should throw exception when refund amount is null")
    void shouldThrowExceptionWhenRefundAmountIsNull() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act & Assert
      assertThatThrownBy(() -> payment.processRefund(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when refund amount is zero")
    void shouldThrowExceptionWhenRefundAmountIsZero() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act & Assert
      assertThatThrownBy(() -> payment.processRefund(BigDecimal.ZERO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount must be positive");
    }

    @Test
    @DisplayName("Should throw exception when refund amount is negative")
    void shouldThrowExceptionWhenRefundAmountIsNegative() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act & Assert
      assertThatThrownBy(() -> payment.processRefund(new BigDecimal("-10.00")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount must be positive");
    }
  }

  @Nested
  @DisplayName("Refundable Amount Tests")
  class RefundableAmountTests {

    @Test
    @DisplayName("Should return full amount as refundable for succeeded payment")
    void shouldReturnFullAmountAsRefundableForSucceededPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act
      BigDecimal refundable = payment.getRefundableAmount();

      // Assert
      assertThat(refundable).isEqualByComparingTo(new BigDecimal("49.99"));
    }

    @Test
    @DisplayName("Should return remaining amount as refundable after partial refund")
    void shouldReturnRemainingAmountAsRefundableAfterPartialRefund() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");
      payment.processRefund(new BigDecimal("20.00"));

      // Act
      BigDecimal refundable = payment.getRefundableAmount();

      // Assert
      assertThat(refundable).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    @DisplayName("Should return zero as refundable for fully refunded payment")
    void shouldReturnZeroAsRefundableForFullyRefundedPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");
      payment.processRefund(new BigDecimal("49.99"));

      // Act
      BigDecimal refundable = payment.getRefundableAmount();

      // Assert
      assertThat(refundable).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero as refundable for pending payment")
    void shouldReturnZeroAsRefundableForPendingPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );

      // Act
      BigDecimal refundable = payment.getRefundableAmount();

      // Assert
      assertThat(refundable).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero as refundable for failed payment")
    void shouldReturnZeroAsRefundableForFailedPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsFailed("Card declined");

      // Act
      BigDecimal refundable = payment.getRefundableAmount();

      // Assert
      assertThat(refundable).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }

  @Nested
  @DisplayName("Status Message Tests")
  class StatusMessageTests {

    @Test
    @DisplayName("Should return correct message for pending payment")
    void shouldReturnCorrectMessageForPendingPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );

      // Act
      String message = payment.getStatusMessage();

      // Assert
      assertThat(message).isEqualTo("Payment is being processed");
    }

    @Test
    @DisplayName("Should return correct message for succeeded payment")
    void shouldReturnCorrectMessageForSucceededPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");

      // Act
      String message = payment.getStatusMessage();

      // Assert
      assertThat(message).isEqualTo("Payment completed successfully");
    }

    @Test
    @DisplayName("Should return correct message for failed payment")
    void shouldReturnCorrectMessageForFailedPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsFailed("Insufficient funds");

      // Act
      String message = payment.getStatusMessage();

      // Assert
      assertThat(message).isEqualTo("Payment failed: Insufficient funds");
    }

    @Test
    @DisplayName("Should return correct message for fully refunded payment")
    void shouldReturnCorrectMessageForFullyRefundedPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");
      payment.processRefund(new BigDecimal("49.99"));

      // Act
      String message = payment.getStatusMessage();

      // Assert
      assertThat(message).isEqualTo("Payment fully refunded");
    }

    @Test
    @DisplayName("Should return correct message for partially refunded payment")
    void shouldReturnCorrectMessageForPartiallyRefundedPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.markAsSucceeded("pi_test123");
      payment.processRefund(new BigDecimal("20.00"));

      // Act
      String message = payment.getStatusMessage();

      // Assert
      assertThat(message).contains("Payment partially refunded");
      assertThat(message).contains("20.00");
      assertThat(message).contains("49.99");
    }
  }

  @Nested
  @DisplayName("Metadata Tests")
  class MetadataTests {

    @Test
    @DisplayName("Should add metadata to payment")
    void shouldAddMetadataToPayment() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );

      // Act
      payment.addMetadata("customer_note", "Urgent payment");
      payment.addMetadata("invoice_number", "INV-001");

      // Assert
      Map<String, Object> metadata = payment.getMetadata();
      assertThat(metadata).hasSize(2);
      assertThat(metadata.get("customer_note")).isEqualTo("Urgent payment");
      assertThat(metadata.get("invoice_number")).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("Should return defensive copy of metadata")
    void shouldReturnDefensiveCopyOfMetadata() {
      // Arrange
      Payment payment = new Payment(
          testInvoice,
          testTenantId,
          new BigDecimal("49.99"),
          "USD",
          testPaymentMethod
      );
      payment.addMetadata("key", "value");

      // Act
      Map<String, Object> metadata1 = payment.getMetadata();
      metadata1.put("external_key", "external_value");
      Map<String, Object> metadata2 = payment.getMetadata();

      // Assert
      assertThat(metadata2).doesNotContainKey("external_key");
      assertThat(metadata2).hasSize(1);
    }
  }
}
