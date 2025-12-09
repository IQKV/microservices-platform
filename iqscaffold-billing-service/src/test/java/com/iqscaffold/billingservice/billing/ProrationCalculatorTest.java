package com.iqscaffold.billingservice.billing;

import static org.junit.jupiter.api.Assertions.*;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.Subscription;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;
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
    } catch (Exception e) {
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
}
