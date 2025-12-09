package com.iqscaffold.billingservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ProrationCalculator domain service.
 * Tests proration calculation logic for plan changes.
 */
@DisplayName("ProrationCalculator Domain Service Tests")
class ProrationCalculatorTest {

  private ProrationCalculator prorationCalculator;
  private UUID tenantId;
  private UUID userId;
  private SubscriptionPlan basicPlan;
  private SubscriptionPlan proPlan;
  private SubscriptionPlan enterprisePlan;

  @BeforeEach
  void setUp() {
    prorationCalculator = new ProrationCalculator();
    tenantId = UUID.randomUUID();
    userId = UUID.randomUUID();

    basicPlan = SubscriptionPlan.create(
        "BASIC_MONTHLY",
        "Basic Monthly",
        "Basic plan",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("9.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.freeTier(),
        0,
        true
    );

    proPlan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Monthly",
        "Pro plan",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("29.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.proTier(),
        0,
        true
    );

    enterprisePlan = SubscriptionPlan.create(
        "ENTERPRISE_MONTHLY",
        "Enterprise Monthly",
        "Enterprise plan",
        PlanTier.ENTERPRISE,
        BillingCycle.MONTHLY,
        new BigDecimal("99.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.unlimited(),
        0,
        true
    );
    
    // Set IDs on plans using reflection since they're not persisted
    // This is needed because ProrationCalculator compares plan IDs
    try {
      var idField = SubscriptionPlan.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(basicPlan, 1L);
      idField.set(proPlan, 2L);
      idField.set(enterprisePlan, 3L);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to set plan IDs", e);
    }
  }

  @Nested
  @DisplayName("Upgrade Calculation Tests")
  class UpgradeCalculationTests {

    @Test
    @DisplayName("Should calculate proration for upgrade")
    void shouldCalculateProrationForUpgrade() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertTrue(result.isUpgrade());
      assertFalse(result.isDowngrade());
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) > 0);
      assertTrue(result.creditAmount().compareTo(BigDecimal.ZERO) >= 0);
      assertTrue(result.chargeAmount().compareTo(BigDecimal.ZERO) > 0);
      assertTrue(result.daysRemaining() > 0);
      assertTrue(result.daysInPeriod() > 0);
    }

    @Test
    @DisplayName("Should calculate correct net amount for upgrade")
    void shouldCalculateCorrectNetAmountForUpgrade() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      var expectedNetAmount = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(expectedNetAmount, result.netAmount());
    }

    @Test
    @DisplayName("Should calculate upgrade with immediate effect")
    void shouldCalculateUpgradeWithImmediateEffect() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);

      // When
      var result = prorationCalculator.calculateUpgrade(subscription, proPlan);

      // Then
      assertNotNull(result);
      assertTrue(result.isUpgrade());
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) > 0);
    }
  }

  @Nested
  @DisplayName("Downgrade Calculation Tests")
  class DowngradeCalculationTests {

    @Test
    @DisplayName("Should calculate proration for downgrade")
    void shouldCalculateProrationForDowngrade() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, proPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When
      var result = prorationCalculator.calculate(subscription, basicPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertFalse(result.isUpgrade());
      assertTrue(result.isDowngrade());
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) < 0);
      assertTrue(result.creditAmount().compareTo(BigDecimal.ZERO) >= 0);
      assertTrue(result.chargeAmount().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    @DisplayName("Should calculate correct net amount for downgrade")
    void shouldCalculateCorrectNetAmountForDowngrade() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, proPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When
      var result = prorationCalculator.calculate(subscription, basicPlan, effectiveDate);

      // Then
      var expectedNetAmount = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(expectedNetAmount, result.netAmount());
    }

    @Test
    @DisplayName("Should calculate downgrade with immediate effect")
    void shouldCalculateDowngradeWithImmediateEffect() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, proPlan);

      // When
      var result = prorationCalculator.calculateDowngrade(subscription, basicPlan);

      // Then
      assertNotNull(result);
      assertTrue(result.isDowngrade());
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) < 0);
    }
  }

  @Nested
  @DisplayName("Scheduled Change Tests")
  class ScheduledChangeTests {

    @Test
    @DisplayName("Should calculate scheduled change with no proration")
    void shouldCalculateScheduledChangeWithNoProration() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);

      // When
      var result = prorationCalculator.calculateScheduledChange(subscription, proPlan);

      // Then
      assertNotNull(result);
      assertFalse(result.requiresProration());
      assertEquals(proPlan.getBasePrice(), result.chargeAmount());
      assertEquals(BigDecimal.ZERO.setScale(2), result.creditAmount());
      assertEquals(proPlan.getBasePrice(), result.netAmount());
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle effective date at period end")
    void shouldHandleEffectiveDateAtPeriodEnd() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      // Use a date very close to period end (1 day before)
      var effectiveDate = subscription.getCurrentPeriodEnd().minusDays(1);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      // Should have minimal days remaining
      assertTrue(result.daysRemaining() <= 1);
      // With only 1 day remaining, proration should still be calculated
      assertTrue(result.requiresProration());
    }

    @Test
    @DisplayName("Should handle same day change (0 days remaining)")
    void shouldHandleSameDayChange() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodEnd();

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      // When effective date equals period end, days remaining should be 0
      // This is handled by the calculator's edge case logic
      assertFalse(result.requiresProration());
      assertEquals(BigDecimal.ZERO.setScale(2), result.creditAmount());
      assertEquals(proPlan.getBasePrice().setScale(2), result.chargeAmount());
      assertEquals(proPlan.getBasePrice().setScale(2), result.netAmount());
    }

    @Test
    @DisplayName("Should handle change at period start (full period)")
    void shouldHandleChangeAtPeriodStart() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart();
      var periodStart = subscription.getCurrentPeriodStart();
      var periodEnd = subscription.getCurrentPeriodEnd();
      var expectedDaysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertEquals(expectedDaysInPeriod, result.daysInPeriod());
      assertEquals(expectedDaysInPeriod, result.daysRemaining());
      // Full period remaining means proration is still calculated
      assertTrue(result.daysRemaining() > 0);
    }

    @Test
    @DisplayName("Should handle leap year February correctly")
    void shouldHandleLeapYearFebruaryCorrectly() {
      // Given - Create subscription starting in leap year February
      var leapYearStart = LocalDateTime.of(2024, 2, 1, 0, 0);
      var leapYearEnd = LocalDateTime.of(2024, 3, 1, 0, 0); // End is start of next month
      
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      // Set period dates using reflection
      try {
        var startField = Subscription.class.getDeclaredField("currentPeriodStart");
        startField.setAccessible(true);
        startField.set(subscription, leapYearStart);
        
        var endField = Subscription.class.getDeclaredField("currentPeriodEnd");
        endField.setAccessible(true);
        endField.set(subscription, leapYearEnd);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to set period dates", e);
      }
      
      var effectiveDate = leapYearStart.plusDays(14); // Mid-February
      var expectedDaysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(leapYearStart, leapYearEnd);
      var expectedDaysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(effectiveDate, leapYearEnd);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertEquals(expectedDaysInPeriod, result.daysInPeriod()); // Leap year February has 29 days
      assertEquals(expectedDaysRemaining, result.daysRemaining());
      assertTrue(result.requiresProration());
      // Verify it's actually a leap year period (29 days)
      assertEquals(29, expectedDaysInPeriod);
    }

    @Test
    @DisplayName("Should handle non-leap year February correctly")
    void shouldHandleNonLeapYearFebruaryCorrectly() {
      // Given - Create subscription starting in non-leap year February
      var nonLeapYearStart = LocalDateTime.of(2023, 2, 1, 0, 0);
      var nonLeapYearEnd = LocalDateTime.of(2023, 3, 1, 0, 0); // End is start of next month
      
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      // Set period dates using reflection
      try {
        var startField = Subscription.class.getDeclaredField("currentPeriodStart");
        startField.setAccessible(true);
        startField.set(subscription, nonLeapYearStart);
        
        var endField = Subscription.class.getDeclaredField("currentPeriodEnd");
        endField.setAccessible(true);
        endField.set(subscription, nonLeapYearEnd);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to set period dates", e);
      }
      
      var effectiveDate = nonLeapYearStart.plusDays(14); // Mid-February

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertEquals(28, result.daysInPeriod()); // Non-leap year February has 28 days
      assertEquals(14, result.daysRemaining()); // 28 - 14 = 14 days remaining
      assertTrue(result.requiresProration());
    }

    @Test
    @DisplayName("Should handle 31-day month correctly")
    void shouldHandle31DayMonthCorrectly() {
      // Given - Create subscription in January (31 days)
      var januaryStart = LocalDateTime.of(2024, 1, 1, 0, 0);
      var januaryEnd = LocalDateTime.of(2024, 2, 1, 0, 0); // End is start of next month
      
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      // Set period dates using reflection
      try {
        var startField = Subscription.class.getDeclaredField("currentPeriodStart");
        startField.setAccessible(true);
        startField.set(subscription, januaryStart);
        
        var endField = Subscription.class.getDeclaredField("currentPeriodEnd");
        endField.setAccessible(true);
        endField.set(subscription, januaryEnd);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to set period dates", e);
      }
      
      var effectiveDate = januaryStart.plusDays(15); // Mid-January

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertEquals(31, result.daysInPeriod());
      assertEquals(16, result.daysRemaining()); // 31 - 15 = 16 days remaining
      assertTrue(result.requiresProration());
    }

    @Test
    @DisplayName("Should handle 30-day month correctly")
    void shouldHandle30DayMonthCorrectly() {
      // Given - Create subscription in April (30 days)
      var aprilStart = LocalDateTime.of(2024, 4, 1, 0, 0);
      var aprilEnd = LocalDateTime.of(2024, 5, 1, 0, 0); // End is start of next month
      
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      // Set period dates using reflection
      try {
        var startField = Subscription.class.getDeclaredField("currentPeriodStart");
        startField.setAccessible(true);
        startField.set(subscription, aprilStart);
        
        var endField = Subscription.class.getDeclaredField("currentPeriodEnd");
        endField.setAccessible(true);
        endField.set(subscription, aprilEnd);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to set period dates", e);
      }
      
      var effectiveDate = aprilStart.plusDays(15); // Mid-April

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertEquals(30, result.daysInPeriod());
      assertEquals(15, result.daysRemaining()); // 30 - 15 = 15 days remaining
      assertTrue(result.requiresProration());
    }

    @Test
    @DisplayName("Should throw exception when effective date is before period start")
    void shouldThrowExceptionWhenEffectiveDateIsBeforePeriodStart() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().minusDays(1);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.calculate(subscription, proPlan, effectiveDate)
      );
    }

    @Test
    @DisplayName("Should throw exception when effective date is after period end")
    void shouldThrowExceptionWhenEffectiveDateIsAfterPeriodEnd() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodEnd().plusDays(1);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.calculate(subscription, proPlan, effectiveDate)
      );
    }

    @Test
    @DisplayName("Should throw exception when new plan is same as current plan")
    void shouldThrowExceptionWhenNewPlanIsSameAsCurrentPlan() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.calculate(subscription, basicPlan, effectiveDate)
      );
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should throw exception when subscription is null")
    void shouldThrowExceptionWhenSubscriptionIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.calculate(null, proPlan, LocalDateTime.now())
      );
    }

    @Test
    @DisplayName("Should throw exception when new plan is null")
    void shouldThrowExceptionWhenNewPlanIsNull() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.calculate(subscription, null, LocalDateTime.now())
      );
    }

    @Test
    @DisplayName("Should throw exception when effective date is null")
    void shouldThrowExceptionWhenEffectiveDateIsNull() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.calculate(subscription, proPlan, null)
      );
    }

    @Test
    @DisplayName("Should throw exception when subscription is not in active state")
    void shouldThrowExceptionWhenSubscriptionIsNotInActiveState() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      subscription.expire();
      var effectiveDate = LocalDateTime.now();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          () -> prorationCalculator.calculate(subscription, proPlan, effectiveDate)
      );
    }
  }

  @Nested
  @DisplayName("Annual Cost Difference Tests")
  class AnnualCostDifferenceTests {

    @Test
    @DisplayName("Should estimate annual cost difference for monthly plans")
    void shouldEstimateAnnualCostDifferenceForMonthlyPlans() {
      // When
      var difference = prorationCalculator.estimateAnnualCostDifference(basicPlan, proPlan);

      // Then
      var expectedDifference = proPlan.getBasePrice()
          .multiply(new BigDecimal("12"))
          .subtract(basicPlan.getBasePrice().multiply(new BigDecimal("12")));

      assertEquals(expectedDifference.setScale(2), difference);
    }

    @Test
    @DisplayName("Should throw exception when current plan is null")
    void shouldThrowExceptionWhenCurrentPlanIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.estimateAnnualCostDifference(null, proPlan)
      );
    }

    @Test
    @DisplayName("Should throw exception when new plan is null")
    void shouldThrowExceptionWhenNewPlanIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> prorationCalculator.estimateAnnualCostDifference(basicPlan, null)
      );
    }
  }

  @Nested
  @DisplayName("Financial Calculation Accuracy Tests")
  class FinancialCalculationAccuracyTests {

    @Test
    @DisplayName("Should calculate proration with exact 50% of period remaining")
    void shouldCalculateProrationWithExact50PercentRemaining() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var periodStart = subscription.getCurrentPeriodStart();
      var periodEnd = subscription.getCurrentPeriodEnd();
      var daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
      var effectiveDate = periodStart.plusDays(daysInPeriod / 2);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      // Calculate the actual proration factor based on days remaining
      var daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(effectiveDate, periodEnd);
      var prorationFactor = new BigDecimal(daysRemaining)
          .divide(new BigDecimal(daysInPeriod), 10, java.math.RoundingMode.HALF_UP);
      
      // Credit should be based on actual proration factor
      var expectedCredit = basicPlan.getBasePrice()
          .multiply(prorationFactor)
          .setScale(2, java.math.RoundingMode.HALF_UP);
      assertEquals(expectedCredit, result.creditAmount());
      
      // Charge should be based on actual proration factor
      var expectedCharge = proPlan.getBasePrice()
          .multiply(prorationFactor)
          .setScale(2, java.math.RoundingMode.HALF_UP);
      assertEquals(expectedCharge, result.chargeAmount());
      
      // Net should be charge - credit
      var expectedNet = expectedCharge.subtract(expectedCredit);
      assertEquals(expectedNet, result.netAmount());
    }

    @Test
    @DisplayName("Should calculate proration with 25% of period remaining")
    void shouldCalculateProrationWith25PercentRemaining() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var periodStart = subscription.getCurrentPeriodStart();
      var periodEnd = subscription.getCurrentPeriodEnd();
      var daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
      var effectiveDate = periodStart.plusDays((daysInPeriod * 3) / 4);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertTrue(result.getPercentageRemaining() >= 24.0 && result.getPercentageRemaining() <= 26.0);
      
      // Verify net amount calculation
      var calculatedNet = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(calculatedNet, result.netAmount());
    }

    @Test
    @DisplayName("Should calculate proration with 75% of period remaining")
    void shouldCalculateProrationWith75PercentRemaining() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var periodStart = subscription.getCurrentPeriodStart();
      var periodEnd = subscription.getCurrentPeriodEnd();
      var daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
      var effectiveDate = periodStart.plusDays(daysInPeriod / 4);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      // Verify percentage is within acceptable range (accounting for rounding and integer division)
      var percentageRemaining = result.getPercentageRemaining();
      assertTrue(percentageRemaining >= 73.0 && percentageRemaining <= 78.0,
          "Expected percentage between 73.0 and 78.0, but got: " + percentageRemaining);
      
      // Verify net amount calculation
      var calculatedNet = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(calculatedNet, result.netAmount());
    }

    @Test
    @DisplayName("Should handle very small price differences accurately")
    void shouldHandleVerySmallPriceDifferencesAccurately() {
      // Given - Create plans with very small price difference
      var plan1 = SubscriptionPlan.create(
          "PLAN1",
          "Plan 1",
          "Plan 1",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("10.00"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          0,
          true
      );
      
      var plan2 = SubscriptionPlan.create(
          "PLAN2",
          "Plan 2",
          "Plan 2",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("10.01"), // Only 1 cent difference
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          0,
          true
      );
      
      // Set IDs
      try {
        var idField = SubscriptionPlan.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(plan1, 10L);
        idField.set(plan2, 11L);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to set plan IDs", e);
      }
      
      var subscription = Subscription.createActive(tenantId, userId, plan1);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When
      var result = prorationCalculator.calculate(subscription, plan2, effectiveDate);

      // Then
      assertNotNull(result);
      assertTrue(result.isUpgrade());
      // Net amount should be very small but positive
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) > 0);
      assertTrue(result.netAmount().compareTo(new BigDecimal("1.00")) < 0);
      
      // Verify precision (2 decimal places)
      assertEquals(2, result.netAmount().scale());
      assertEquals(2, result.creditAmount().scale());
      assertEquals(2, result.chargeAmount().scale());
    }

    @Test
    @DisplayName("Should handle large price differences accurately")
    void shouldHandleLargePriceDifferencesAccurately() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When - Upgrade from $9.99 to $99.99
      var result = prorationCalculator.calculate(subscription, enterprisePlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertTrue(result.isUpgrade());
      // Net amount should be significant
      assertTrue(result.netAmount().compareTo(new BigDecimal("40.00")) > 0);
      
      // Verify all amounts are properly scaled
      assertEquals(2, result.netAmount().scale());
      assertEquals(2, result.creditAmount().scale());
      assertEquals(2, result.chargeAmount().scale());
      
      // Verify calculation integrity
      var calculatedNet = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(calculatedNet, result.netAmount());
    }

    @Test
    @DisplayName("Should maintain precision with repeating decimals")
    void shouldMaintainPrecisionWithRepeatingDecimals() {
      // Given - Create plan with price that creates repeating decimals
      var planWithRepeatingDecimal = SubscriptionPlan.create(
          "REPEATING",
          "Repeating Decimal Plan",
          "Plan with repeating decimal",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("33.33"), // Will create repeating decimals in calculations
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          0,
          true
      );
      
      try {
        var idField = SubscriptionPlan.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(planWithRepeatingDecimal, 20L);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to set plan ID", e);
      }
      
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(10);

      // When
      var result = prorationCalculator.calculate(subscription, planWithRepeatingDecimal, effectiveDate);

      // Then
      assertNotNull(result);
      // All amounts should be properly rounded to 2 decimal places
      assertEquals(2, result.netAmount().scale());
      assertEquals(2, result.creditAmount().scale());
      assertEquals(2, result.chargeAmount().scale());
      
      // Verify calculation integrity despite rounding
      var calculatedNet = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(calculatedNet, result.netAmount());
    }

    @Test
    @DisplayName("Should calculate proration for 1 day remaining accurately")
    void shouldCalculateProrationFor1DayRemainingAccurately() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodEnd().minusDays(1);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertEquals(1, result.daysRemaining());
      assertTrue(result.requiresProration());
      
      // Amounts should be very small (1 day worth)
      var periodStart = subscription.getCurrentPeriodStart();
      var periodEnd = subscription.getCurrentPeriodEnd();
      var daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
      var expectedFactor = new BigDecimal("1").divide(
          new BigDecimal(daysInPeriod), 10, java.math.RoundingMode.HALF_UP);
      
      var expectedCredit = basicPlan.getBasePrice()
          .multiply(expectedFactor)
          .setScale(2, java.math.RoundingMode.HALF_UP);
      assertEquals(expectedCredit, result.creditAmount());
      
      // Verify all amounts are properly scaled
      assertEquals(2, result.netAmount().scale());
      assertEquals(2, result.creditAmount().scale());
      assertEquals(2, result.chargeAmount().scale());
    }

    @Test
    @DisplayName("Should ensure net amount equals charge minus credit")
    void shouldEnsureNetAmountEqualsChargeMinusCredit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(7);

      // When
      var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);

      // Then
      var calculatedNet = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(calculatedNet.setScale(2, java.math.RoundingMode.HALF_UP), result.netAmount());
    }

    @Test
    @DisplayName("Should handle downgrade with credit calculation accuracy")
    void shouldHandleDowngradeWithCreditCalculationAccuracy() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, enterprisePlan);
      var effectiveDate = subscription.getCurrentPeriodStart().plusDays(15);

      // When - Downgrade from $99.99 to $9.99
      var result = prorationCalculator.calculate(subscription, basicPlan, effectiveDate);

      // Then
      assertNotNull(result);
      assertTrue(result.isDowngrade());
      // Net amount should be negative (customer gets credit)
      assertTrue(result.netAmount().compareTo(BigDecimal.ZERO) < 0);
      
      // Credit should be larger than charge
      assertTrue(result.creditAmount().compareTo(result.chargeAmount()) > 0);
      
      // Verify calculation integrity
      var calculatedNet = result.chargeAmount().subtract(result.creditAmount());
      assertEquals(calculatedNet, result.netAmount());
      
      // Verify all amounts are properly scaled
      assertEquals(2, result.netAmount().scale());
      assertEquals(2, result.creditAmount().scale());
      assertEquals(2, result.chargeAmount().scale());
    }

    @Test
    @DisplayName("Should calculate proration factor correctly for various scenarios")
    void shouldCalculateProrationFactorCorrectlyForVariousScenarios() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, basicPlan);
      var periodStart = subscription.getCurrentPeriodStart();
      var periodEnd = subscription.getCurrentPeriodEnd();
      var daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);

      // Test various points in the period
      for (final int daysElapsed = 0; daysElapsed < daysInPeriod; daysElapsed += 5) {
        var effectiveDate = periodStart.plusDays(daysElapsed);
        var result = prorationCalculator.calculate(subscription, proPlan, effectiveDate);
        
        var expectedFactor = new BigDecimal(result.daysRemaining())
            .divide(new BigDecimal(daysInPeriod), 10, java.math.RoundingMode.HALF_UP);
        
        assertEquals(expectedFactor.setScale(10, java.math.RoundingMode.HALF_UP), 
            result.getProrationFactor().setScale(10, java.math.RoundingMode.HALF_UP));
      }
    }
  }
}
