package com.iqscaffold.billingservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ProrationResult value object.
 * Tests immutability, validation, and factory methods.
 */
@DisplayName("ProrationResult Value Object Tests")
class ProrationResultTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create proration result for upgrade")
    void shouldCreateProrationResultForUpgrade() {
      // Given
      var oldPrice = new BigDecimal("9.99");
      var newPrice = new BigDecimal("29.99");
      var daysRemaining = 15;
      var daysInPeriod = 30;

      // When
      var result = ProrationResult.forUpgrade(
          oldPrice,
          newPrice,
          daysRemaining,
          daysInPeriod,
          "Basic",
          "Pro"
      );

      // Then
      assertNotNull(result);
      assertTrue(result.isUpgrade());
      assertFalse(result.isDowngrade());
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) > 0);
      assertEquals(daysRemaining, result.daysRemaining());
      assertEquals(daysInPeriod, result.daysInPeriod());
      assertTrue(result.requiresProration());
    }

    @Test
    @DisplayName("Should create proration result for downgrade")
    void shouldCreateProrationResultForDowngrade() {
      // Given
      var oldPrice = new BigDecimal("29.99");
      var newPrice = new BigDecimal("9.99");
      var daysRemaining = 15;
      var daysInPeriod = 30;

      // When
      var result = ProrationResult.forDowngrade(
          oldPrice,
          newPrice,
          daysRemaining,
          daysInPeriod,
          "Pro",
          "Basic"
      );

      // Then
      assertNotNull(result);
      assertFalse(result.isUpgrade());
      assertTrue(result.isDowngrade());
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) < 0);
      assertEquals(daysRemaining, result.daysRemaining());
      assertEquals(daysInPeriod, result.daysInPeriod());
    }

    @Test
    @DisplayName("Should create proration result with no proration")
    void shouldCreateProrationResultWithNoProration() {
      // Given
      var planPrice = new BigDecimal("29.99");
      var daysInPeriod = 30;

      // When
      var result = ProrationResult.noProration(
          planPrice,
          daysInPeriod,
          "Pro"
      );

      // Then
      assertNotNull(result);
      assertFalse(result.requiresProration());
      assertEquals(BigDecimal.ZERO.setScale(2), result.creditAmount());
      assertEquals(planPrice.setScale(2), result.chargeAmount());
      assertEquals(planPrice.setScale(2), result.netAmount());
      assertEquals(daysInPeriod, result.daysRemaining());
      assertEquals(daysInPeriod, result.daysInPeriod());
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should throw exception when credit amount is null")
    void shouldThrowExceptionWhenCreditAmountIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              null,
              new BigDecimal("10.00"),
              new BigDecimal("5.00"),
              15,
              30,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when credit amount is negative")
    void shouldThrowExceptionWhenCreditAmountIsNegative() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("-5.00"),
              new BigDecimal("10.00"),
              new BigDecimal("5.00"),
              15,
              30,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when charge amount is null")
    void shouldThrowExceptionWhenChargeAmountIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              null,
              new BigDecimal("5.00"),
              15,
              30,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when charge amount is negative")
    void shouldThrowExceptionWhenChargeAmountIsNegative() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("-10.00"),
              new BigDecimal("5.00"),
              15,
              30,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when net amount is null")
    void shouldThrowExceptionWhenNetAmountIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("10.00"),
              null,
              15,
              30,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when days remaining is negative")
    void shouldThrowExceptionWhenDaysRemainingIsNegative() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("10.00"),
              new BigDecimal("5.00"),
              -1,
              30,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when days in period is zero or negative")
    void shouldThrowExceptionWhenDaysInPeriodIsZeroOrNegative() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("10.00"),
              new BigDecimal("5.00"),
              15,
              0,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when days remaining exceeds days in period")
    void shouldThrowExceptionWhenDaysRemainingExceedsDaysInPeriod() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("10.00"),
              new BigDecimal("5.00"),
              35,
              30,
              "Description"
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when description is null")
    void shouldThrowExceptionWhenDescriptionIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("10.00"),
              new BigDecimal("5.00"),
              15,
              30,
              null
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when description is blank")
    void shouldThrowExceptionWhenDescriptionIsBlank() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("10.00"),
              new BigDecimal("5.00"),
              15,
              30,
              "   "
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when net amount doesn't match calculation")
    void shouldThrowExceptionWhenNetAmountDoesntMatchCalculation() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProrationResult(
              new BigDecimal("5.00"),
              new BigDecimal("10.00"),
              new BigDecimal("10.00"), // Should be 5.00
              15,
              30,
              "Description"
          )
      );
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("Should identify upgrade")
    void shouldIdentifyUpgrade() {
      // Given
      var result = ProrationResult.forUpgrade(
          new BigDecimal("9.99"),
          new BigDecimal("29.99"),
          15,
          30,
          "Basic",
          "Pro"
      );

      // Then
      assertTrue(result.isUpgrade());
      assertFalse(result.isDowngrade());
      assertFalse(result.isNeutral());
    }

    @Test
    @DisplayName("Should identify downgrade")
    void shouldIdentifyDowngrade() {
      // Given
      var result = ProrationResult.forDowngrade(
          new BigDecimal("29.99"),
          new BigDecimal("9.99"),
          15,
          30,
          "Pro",
          "Basic"
      );

      // Then
      assertFalse(result.isUpgrade());
      assertTrue(result.isDowngrade());
      assertFalse(result.isNeutral());
    }

    @Test
    @DisplayName("Should identify neutral change")
    void shouldIdentifyNeutralChange() {
      // Given
      var result = new ProrationResult(
          new BigDecimal("10.00"),
          new BigDecimal("10.00"),
          BigDecimal.ZERO.setScale(2),
          15,
          30,
          "Same price"
      );

      // Then
      assertFalse(result.isUpgrade());
      assertFalse(result.isDowngrade());
      assertTrue(result.isNeutral());
    }

    @Test
    @DisplayName("Should get absolute net amount")
    void shouldGetAbsoluteNetAmount() {
      // Given
      var upgradeResult = ProrationResult.forUpgrade(
          new BigDecimal("9.99"),
          new BigDecimal("29.99"),
          15,
          30,
          "Basic",
          "Pro"
      );

      var downgradeResult = ProrationResult.forDowngrade(
          new BigDecimal("29.99"),
          new BigDecimal("9.99"),
          15,
          30,
          "Pro",
          "Basic"
      );

      // Then
      assertTrue(upgradeResult.getAbsoluteNetAmount().compareTo(BigDecimal.ZERO) > 0);
      assertTrue(downgradeResult.getAbsoluteNetAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should calculate proration factor")
    void shouldCalculateProrationFactor() {
      // Given
      var result = ProrationResult.forUpgrade(
          new BigDecimal("9.99"),
          new BigDecimal("29.99"),
          15,
          30,
          "Basic",
          "Pro"
      );

      // When
      var factor = result.getProrationFactor();

      // Then
      assertEquals(0.5, factor.doubleValue(), 0.01);
    }

    @Test
    @DisplayName("Should calculate percentage remaining")
    void shouldCalculatePercentageRemaining() {
      // Given
      var result = ProrationResult.forUpgrade(
          new BigDecimal("9.99"),
          new BigDecimal("29.99"),
          15,
          30,
          "Basic",
          "Pro"
      );

      // When
      var percentage = result.getPercentageRemaining();

      // Then
      assertEquals(50.0, percentage, 0.01);
    }

    @Test
    @DisplayName("Should check if proration is required")
    void shouldCheckIfProrationIsRequired() {
      // Given
      var prorationRequired = ProrationResult.forUpgrade(
          new BigDecimal("9.99"),
          new BigDecimal("29.99"),
          15,
          30,
          "Basic",
          "Pro"
      );

      var noProration = ProrationResult.noProration(
          new BigDecimal("29.99"),
          30,
          "Pro"
      );

      // Then
      assertTrue(prorationRequired.requiresProration());
      assertFalse(noProration.requiresProration());
    }

    @Test
    @DisplayName("Should get detailed description")
    void shouldGetDetailedDescription() {
      // Given
      var result = ProrationResult.forUpgrade(
          new BigDecimal("9.99"),
          new BigDecimal("29.99"),
          15,
          30,
          "Basic",
          "Pro"
      );

      // When
      var detailedDescription = result.getDetailedDescription();

      // Then
      assertNotNull(detailedDescription);
      assertTrue(detailedDescription.contains("Credit"));
      assertTrue(detailedDescription.contains("Charge"));
      assertTrue(detailedDescription.contains("Net"));
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("Should be immutable record")
    void shouldBeImmutableRecord() {
      // Given
      var result = ProrationResult.forUpgrade(
          new BigDecimal("9.99"),
          new BigDecimal("29.99"),
          15,
          30,
          "Basic",
          "Pro"
      );

      // When - Try to create a new instance with different values
      var newResult = new ProrationResult(
          new BigDecimal("10.00"),
          result.chargeAmount(),
          result.chargeAmount().subtract(new BigDecimal("10.00")),
          result.daysRemaining(),
          result.daysInPeriod(),
          result.description()
      );

      // Then - Original should be unchanged
      assertNotEquals(result.creditAmount(), newResult.creditAmount());
    }
  }
}
