package com.iqscaffold.billingservice.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PaymentResult record.
 * 
 * <p>Tests the immutable payment result record including:
 * <ul>
 *   <li>Factory methods for success and failure results</li>
 *   <li>Validation of payment result state</li>
 *   <li>Invariant enforcement</li>
 *   <li>Provider-agnostic result representation</li>
 * </ul>
 */
@DisplayName("PaymentResult Record Tests")
class PaymentResultTest {

  @Nested
  @DisplayName("Success Result Tests")
  class SuccessResultTests {

    @Test
    @DisplayName("Should create successful payment result")
    void shouldCreateSuccessfulPaymentResult() {
      // Arrange
      LocalDateTime processedAt = LocalDateTime.now();

      // Act
      PaymentResult result = PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          processedAt
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.failed()).isFalse();
      assertThat(result.providerPaymentId()).isEqualTo("pi_test123");
      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(result.currency()).isEqualTo("USD");
      assertThat(result.processedAt()).isEqualTo(processedAt);
      assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when provider payment ID is null")
    void shouldThrowExceptionWhenProviderPaymentIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.success(
          null,
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now()
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Successful payment must have a provider payment ID");
    }

    @Test
    @DisplayName("Should throw exception when provider payment ID is blank")
    void shouldThrowExceptionWhenProviderPaymentIdIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.success(
          "   ",
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now()
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Successful payment must have a provider payment ID");
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.success(
          "pi_test123",
          null,
          "USD",
          LocalDateTime.now()
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Payment amount must be non-negative");
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.success(
          "pi_test123",
          new BigDecimal("-10.00"),
          "USD",
          LocalDateTime.now()
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Payment amount must be non-negative");
    }

    @Test
    @DisplayName("Should allow zero amount for successful payment")
    void shouldAllowZeroAmountForSuccessfulPayment() {
      // Act
      PaymentResult result = PaymentResult.success(
          "pi_test123",
          BigDecimal.ZERO,
          "USD",
          LocalDateTime.now()
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowExceptionWhenCurrencyIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          null,
          LocalDateTime.now()
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Currency must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when currency is blank")
    void shouldThrowExceptionWhenCurrencyIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "   ",
          LocalDateTime.now()
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Currency must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when processed timestamp is null")
    void shouldThrowExceptionWhenProcessedTimestampIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Processed timestamp must not be null");
    }
  }

  @Nested
  @DisplayName("Failure Result Tests")
  class FailureResultTests {

    @Test
    @DisplayName("Should create failed payment result")
    void shouldCreateFailedPaymentResult() {
      // Act
      PaymentResult result = PaymentResult.failure(
          new BigDecimal("49.99"),
          "USD",
          "Insufficient funds"
      );

      // Assert
      assertThat(result.success()).isFalse();
      assertThat(result.failed()).isTrue();
      assertThat(result.providerPaymentId()).isNull();
      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(result.currency()).isEqualTo("USD");
      assertThat(result.processedAt()).isNotNull();
      assertThat(result.failureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("Should throw exception when failure reason is null")
    void shouldThrowExceptionWhenFailureReasonIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.failure(
          new BigDecimal("49.99"),
          "USD",
          null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Failed payment must have a failure reason");
    }

    @Test
    @DisplayName("Should throw exception when failure reason is blank")
    void shouldThrowExceptionWhenFailureReasonIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.failure(
          new BigDecimal("49.99"),
          "USD",
          "   "
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Failed payment must have a failure reason");
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.failure(
          null,
          "USD",
          "Card declined"
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Payment amount must be non-negative");
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.failure(
          new BigDecimal("-10.00"),
          "USD",
          "Card declined"
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Payment amount must be non-negative");
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowExceptionWhenCurrencyIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.failure(
          new BigDecimal("49.99"),
          null,
          "Card declined"
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Currency must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when currency is blank")
    void shouldThrowExceptionWhenCurrencyIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> PaymentResult.failure(
          new BigDecimal("49.99"),
          "   ",
          "Card declined"
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Currency must not be null or blank");
    }

    @Test
    @DisplayName("Should auto-generate processed timestamp for failure")
    void shouldAutoGenerateProcessedTimestampForFailure() {
      // Arrange
      LocalDateTime before = LocalDateTime.now();

      // Act
      PaymentResult result = PaymentResult.failure(
          new BigDecimal("49.99"),
          "USD",
          "Card declined"
      );

      LocalDateTime after = LocalDateTime.now();

      // Assert
      assertThat(result.processedAt()).isNotNull();
      assertThat(result.processedAt()).isAfterOrEqualTo(before);
      assertThat(result.processedAt()).isBeforeOrEqualTo(after);
    }
  }

  @Nested
  @DisplayName("Direct Constructor Tests")
  class DirectConstructorTests {

    @Test
    @DisplayName("Should create payment result with direct constructor")
    void shouldCreatePaymentResultWithDirectConstructor() {
      // Arrange
      LocalDateTime processedAt = LocalDateTime.now();

      // Act
      PaymentResult result = new PaymentResult(
          true,
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          processedAt,
          null
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.providerPaymentId()).isEqualTo("pi_test123");
      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(result.currency()).isEqualTo("USD");
      assertThat(result.processedAt()).isEqualTo(processedAt);
      assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("Should validate successful payment has provider ID")
    void shouldValidateSuccessfulPaymentHasProviderId() {
      // Act & Assert
      assertThatThrownBy(() -> new PaymentResult(
          true,
          null,
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now(),
          null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Successful payment must have a provider payment ID");
    }

    @Test
    @DisplayName("Should validate failed payment has failure reason")
    void shouldValidateFailedPaymentHasFailureReason() {
      // Act & Assert
      assertThatThrownBy(() -> new PaymentResult(
          false,
          null,
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now(),
          null
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Failed payment must have a failure reason");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("Should be immutable record")
    void shouldBeImmutableRecord() {
      // Arrange
      LocalDateTime processedAt = LocalDateTime.now();
      PaymentResult result = PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          processedAt
      );

      // Assert - verify all fields are accessible and match
      assertThat(result.success()).isTrue();
      assertThat(result.providerPaymentId()).isEqualTo("pi_test123");
      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("49.99"));
      assertThat(result.currency()).isEqualTo("USD");
      assertThat(result.processedAt()).isEqualTo(processedAt);
      assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("Should support equality comparison")
    void shouldSupportEqualityComparison() {
      // Arrange
      LocalDateTime processedAt = LocalDateTime.now();
      PaymentResult result1 = PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          processedAt
      );
      PaymentResult result2 = PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          processedAt
      );

      // Assert
      assertThat(result1).isEqualTo(result2);
      assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
      // Arrange
      PaymentResult result = PaymentResult.success(
          "pi_test123",
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now()
      );

      // Act
      String toString = result.toString();

      // Assert
      assertThat(toString).contains("pi_test123");
      assertThat(toString).contains("49.99");
      assertThat(toString).contains("USD");
      assertThat(toString).contains("true");
    }
  }

  @Nested
  @DisplayName("Common Payment Scenarios Tests")
  class CommonPaymentScenariosTests {

    @Test
    @DisplayName("Should handle card declined scenario")
    void shouldHandleCardDeclinedScenario() {
      // Act
      PaymentResult result = PaymentResult.failure(
          new BigDecimal("99.99"),
          "USD",
          "Card declined - insufficient funds"
      );

      // Assert
      assertThat(result.failed()).isTrue();
      assertThat(result.failureReason()).contains("Card declined");
      assertThat(result.providerPaymentId()).isNull();
    }

    @Test
    @DisplayName("Should handle expired card scenario")
    void shouldHandleExpiredCardScenario() {
      // Act
      PaymentResult result = PaymentResult.failure(
          new BigDecimal("49.99"),
          "USD",
          "Card expired"
      );

      // Assert
      assertThat(result.failed()).isTrue();
      assertThat(result.failureReason()).isEqualTo("Card expired");
    }

    @Test
    @DisplayName("Should handle fraud detection scenario")
    void shouldHandleFraudDetectionScenario() {
      // Act
      PaymentResult result = PaymentResult.failure(
          new BigDecimal("999.99"),
          "USD",
          "Payment blocked by fraud detection"
      );

      // Assert
      assertThat(result.failed()).isTrue();
      assertThat(result.failureReason()).contains("fraud detection");
    }

    @Test
    @DisplayName("Should handle successful payment with different currencies")
    void shouldHandleSuccessfulPaymentWithDifferentCurrencies() {
      // Act
      PaymentResult usdResult = PaymentResult.success(
          "pi_usd123",
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now()
      );
      PaymentResult eurResult = PaymentResult.success(
          "pi_eur123",
          new BigDecimal("44.99"),
          "EUR",
          LocalDateTime.now()
      );
      PaymentResult gbpResult = PaymentResult.success(
          "pi_gbp123",
          new BigDecimal("39.99"),
          "GBP",
          LocalDateTime.now()
      );

      // Assert
      assertThat(usdResult.success()).isTrue();
      assertThat(usdResult.currency()).isEqualTo("USD");
      assertThat(eurResult.success()).isTrue();
      assertThat(eurResult.currency()).isEqualTo("EUR");
      assertThat(gbpResult.success()).isTrue();
      assertThat(gbpResult.currency()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("Should handle large payment amounts")
    void shouldHandleLargePaymentAmounts() {
      // Act
      PaymentResult result = PaymentResult.success(
          "pi_large123",
          new BigDecimal("9999999.99"),
          "USD",
          LocalDateTime.now()
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("9999999.99"));
    }

    @Test
    @DisplayName("Should handle small payment amounts")
    void shouldHandleSmallPaymentAmounts() {
      // Act
      PaymentResult result = PaymentResult.success(
          "pi_small123",
          new BigDecimal("0.01"),
          "USD",
          LocalDateTime.now()
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("0.01"));
    }
  }

  @Nested
  @DisplayName("Provider-Agnostic Tests")
  class ProviderAgnosticTests {

    @Test
    @DisplayName("Should work with Stripe-style payment IDs")
    void shouldWorkWithStripeStylePaymentIds() {
      // Act
      PaymentResult result = PaymentResult.success(
          "pi_3MtwBwLkdIwHu7ix28a3tqPa",
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now()
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.providerPaymentId()).startsWith("pi_");
    }

    @Test
    @DisplayName("Should work with PayPal-style payment IDs")
    void shouldWorkWithPayPalStylePaymentIds() {
      // Act
      PaymentResult result = PaymentResult.success(
          "PAYID-MXYZ123-ABC456",
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now()
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.providerPaymentId()).contains("PAYID");
    }

    @Test
    @DisplayName("Should work with generic UUID payment IDs")
    void shouldWorkWithGenericUuidPaymentIds() {
      // Act
      PaymentResult result = PaymentResult.success(
          "550e8400-e29b-41d4-a716-446655440000",
          new BigDecimal("49.99"),
          "USD",
          LocalDateTime.now()
      );

      // Assert
      assertThat(result.success()).isTrue();
      assertThat(result.providerPaymentId()).matches(
          "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
      );
    }
  }
}
