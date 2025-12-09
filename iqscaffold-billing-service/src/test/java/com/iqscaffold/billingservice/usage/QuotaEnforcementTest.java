package com.iqscaffold.billingservice.usage;

import static org.junit.jupiter.api.Assertions.*;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.Subscription;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for quota enforcement logic.
 * Tests quota checking for various usage levels, exceeded scenarios, and grace period (5% overage).
 * 
 * <p>Test Coverage:
 * <ul>
 *   <li>Various usage levels (0%, 25%, 50%, 75%, 90%, 100%)</li>
 *   <li>Quota exceeded scenarios (hard limit, grace period)</li>
 *   <li>Grace period logic (5% overage allowed)</li>
 *   <li>Edge cases (exact limit, just over limit, just under limit)</li>
 *   <li>Multiple metric types</li>
 *   <li>Unlimited quotas</li>
 * </ul>
 */
@DisplayName("Quota Enforcement Comprehensive Tests")
class QuotaEnforcementTest {

  private QuotaEnforcer quotaEnforcer;
  private UUID tenantId;
  private UUID userId;
  private SubscriptionPlan freePlan;
  private SubscriptionPlan proPlan;
  private SubscriptionPlan unlimitedPlan;

  // Quota limits for FREE tier (from PlanQuotas.freeTier())
  private static final long FREE_API_CALLS_LIMIT = 1000L;
  private static final long FREE_STORAGE_GB_LIMIT = 1L;
  private static final long FREE_EMAIL_SENDS_LIMIT = 100L;
  private static final long FREE_MAX_USERS_LIMIT = 5L;

  // Grace period is 5%
  private static final double GRACE_PERIOD_PERCENTAGE = 0.05;

  @BeforeEach
  void setUp() {
    quotaEnforcer = new QuotaEnforcer();
    tenantId = UUID.randomUUID();
    userId = UUID.randomUUID();

    freePlan = SubscriptionPlan.create(
        "FREE",
        "Free Plan",
        "Free tier with limited quotas",
        PlanTier.FREE,
        BillingCycle.MONTHLY,
        BigDecimal.ZERO,
        "USD",
        new HashMap<>(),
        PlanQuotas.freeTier(),
        0,
        true
    );

    proPlan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Monthly",
        "Pro plan with higher quotas",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("29.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.proTier(),
        0,
        true
    );

    unlimitedPlan = SubscriptionPlan.create(
        "ENTERPRISE",
        "Enterprise",
        "Enterprise plan with unlimited quotas",
        PlanTier.ENTERPRISE,
        BillingCycle.MONTHLY,
        new BigDecimal("99.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.unlimited(),
        0,
        true
    );
  }

  @Nested
  @DisplayName("Various Usage Levels Tests")
  class VariousUsageLevelsTests {

    @Test
    @DisplayName("0% usage: Should allow operation with full quota remaining")
    void zeroPercentUsageShouldAllowOperation() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 0L;
      var requestedAmount = 100L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(0L, result.currentUsage());
      assertEquals(FREE_API_CALLS_LIMIT, result.limit());
      assertEquals(FREE_API_CALLS_LIMIT - requestedAmount, result.remainingQuota());
      assertEquals(0.0, quotaEnforcer.getQuotaUsagePercentage(subscription, MetricType.API_CALLS, currentUsage));
    }

    @Test
    @DisplayName("25% usage: Should allow operation with 75% quota remaining")
    void twentyFivePercentUsageShouldAllowOperation() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 250L; // 25% of 1000
      var requestedAmount = 100L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(250L, result.currentUsage());
      assertEquals(FREE_API_CALLS_LIMIT - currentUsage - requestedAmount, result.remainingQuota());
      assertEquals(25.0, quotaEnforcer.getQuotaUsagePercentage(subscription, MetricType.API_CALLS, currentUsage));
    }

    @Test
    @DisplayName("50% usage: Should allow operation with 50% quota remaining")
    void fiftyPercentUsageShouldAllowOperation() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 500L; // 50% of 1000
      var requestedAmount = 100L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(500L, result.currentUsage());
      assertEquals(400L, result.remainingQuota());
      assertEquals(50.0, quotaEnforcer.getQuotaUsagePercentage(subscription, MetricType.API_CALLS, currentUsage));
      assertFalse(quotaEnforcer.isApproachingLimit(subscription, MetricType.API_CALLS, currentUsage));
    }

    @Test
    @DisplayName("75% usage: Should allow operation with 25% quota remaining")
    void seventyFivePercentUsageShouldAllowOperation() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 750L; // 75% of 1000
      var requestedAmount = 100L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(750L, result.currentUsage());
      assertEquals(150L, result.remainingQuota());
      assertEquals(75.0, quotaEnforcer.getQuotaUsagePercentage(subscription, MetricType.API_CALLS, currentUsage));
      assertFalse(quotaEnforcer.isApproachingLimit(subscription, MetricType.API_CALLS, currentUsage));
    }

    @Test
    @DisplayName("90% usage: Should allow operation but flag as approaching limit")
    void ninetyPercentUsageShouldAllowButFlagApproachingLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 900L; // 90% of 1000
      var requestedAmount = 50L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(900L, result.currentUsage());
      assertEquals(50L, result.remainingQuota());
      assertEquals(90.0, quotaEnforcer.getQuotaUsagePercentage(subscription, MetricType.API_CALLS, currentUsage));
      assertTrue(quotaEnforcer.isApproachingLimit(subscription, MetricType.API_CALLS, currentUsage));
      assertTrue(result.isApproachingLimit());
    }

    @Test
    @DisplayName("95% usage: Should allow operation within grace period")
    void ninetyFivePercentUsageShouldAllowOperation() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 950L; // 95% of 1000
      var requestedAmount = 30L; // Total 980, still under limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(950L, result.currentUsage());
      assertEquals(95.0, quotaEnforcer.getQuotaUsagePercentage(subscription, MetricType.API_CALLS, currentUsage));
      assertTrue(quotaEnforcer.isApproachingLimit(subscription, MetricType.API_CALLS, currentUsage));
    }

    @Test
    @DisplayName("100% usage: Should allow small operation within grace period")
    void oneHundredPercentUsageShouldAllowWithinGracePeriod() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L; // 100% of 1000
      var requestedAmount = 30L; // Within 5% grace period (1050 total allowed)

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(1000L, result.currentUsage());
      assertEquals(100.0, quotaEnforcer.getQuotaUsagePercentage(subscription, MetricType.API_CALLS, currentUsage));
      assertTrue(result.isExceeded()); // Exceeded base limit but within grace
    }
  }

  @Nested
  @DisplayName("Grace Period Logic Tests (5% Overage)")
  class GracePeriodLogicTests {

    @Test
    @DisplayName("Should allow operation exactly at grace limit")
    void shouldAllowOperationExactlyAtGraceLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var graceLimit = (long) (FREE_API_CALLS_LIMIT * (1.0 + GRACE_PERIOD_PERCENTAGE)); // 1050
      var currentUsage = 1000L;
      var requestedAmount = 50L; // Exactly at grace limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(graceLimit, currentUsage + requestedAmount);
    }

    @Test
    @DisplayName("Should deny operation just over grace limit")
    void shouldDenyOperationJustOverGraceLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L;
      var requestedAmount = 51L; // Just over 5% grace limit (1051 > 1050)

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(0L, result.remainingQuota());
      assertNotNull(result.message());
      assertTrue(result.message().contains("Quota exceeded"));
    }

    @Test
    @DisplayName("Should allow operation just under grace limit")
    void shouldAllowOperationJustUnderGraceLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L;
      var requestedAmount = 49L; // Just under 5% grace limit (1049 < 1050)

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertTrue(result.remainingQuota() > 0);
    }

    @Test
    @DisplayName("Should calculate grace limit correctly for different quotas")
    void shouldCalculateGraceLimitCorrectlyForDifferentQuotas() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // Test API_CALLS (1000 limit, 1050 with grace)
      var apiCallsGraceLimit = (long) (FREE_API_CALLS_LIMIT * (1.0 + GRACE_PERIOD_PERCENTAGE));
      assertEquals(1050L, apiCallsGraceLimit);

      // Test EMAIL_SENDS (100 limit, 105 with grace)
      var emailGraceLimit = (long) (FREE_EMAIL_SENDS_LIMIT * (1.0 + GRACE_PERIOD_PERCENTAGE));
      assertEquals(105L, emailGraceLimit);

      // Verify grace period works for EMAIL_SENDS
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.EMAIL_SENDS,
          100L,
          5L // Exactly at grace limit
      );
      assertTrue(result.allowed());
    }

    @Test
    @DisplayName("Should apply grace period to all metric types")
    void shouldApplyGracePeriodToAllMetricTypes() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // Test STORAGE_GB (1 GB limit, 1.05 GB with grace)
      var storageResult = quotaEnforcer.checkQuota(
          subscription,
          MetricType.STORAGE_GB,
          1L,
          0L // At limit, should still allow within grace
      );
      assertTrue(storageResult.allowed());

      // Test ACTIVE_USERS (5 users limit, 5.25 with grace = 5 due to rounding)
      var usersResult = quotaEnforcer.checkQuota(
          subscription,
          MetricType.ACTIVE_USERS,
          5L,
          0L // At limit
      );
      assertTrue(usersResult.allowed());
    }

    @Test
    @DisplayName("Should not apply grace period to unlimited quotas")
    void shouldNotApplyGracePeriodToUnlimitedQuotas() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, unlimitedPlan);
      var currentUsage = 1000000L;
      var requestedAmount = 1000000L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertFalse(result.hasLimit());
      assertEquals(Long.MAX_VALUE, result.remainingQuota());
    }
  }

  @Nested
  @DisplayName("Quota Exceeded Scenarios")
  class QuotaExceededScenariosTests {

    @Test
    @DisplayName("Should deny operation when significantly over limit")
    void shouldDenyOperationWhenSignificantlyOverLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L;
      var requestedAmount = 200L; // Way over grace limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(0L, result.remainingQuota());
      assertTrue(result.message().contains("Quota exceeded"));
      assertTrue(result.message().contains("API_CALLS"));
    }

    @Test
    @DisplayName("Should deny operation when already over grace limit")
    void shouldDenyOperationWhenAlreadyOverGraceLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1100L; // Already over grace limit
      var requestedAmount = 1L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(0L, result.remainingQuota());
    }

    @Test
    @DisplayName("Should throw exception when enforcing exceeded quota")
    void shouldThrowExceptionWhenEnforcingExceededQuota() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L;
      var requestedAmount = 100L; // Over grace limit

      // When & Then
      var exception = assertThrows(
          QuotaEnforcer.QuotaExceededException.class,
          () -> quotaEnforcer.enforceQuota(
              subscription,
              MetricType.API_CALLS,
              currentUsage,
              requestedAmount
          )
      );

      assertEquals("API_CALLS", exception.getMetricType());
      assertEquals(FREE_API_CALLS_LIMIT, exception.getLimit());
      assertEquals(currentUsage, exception.getCurrentUsage());
    }

    @Test
    @DisplayName("Should provide detailed message when quota exceeded")
    void shouldProvideDetailedMessageWhenQuotaExceeded() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 950L;
      var requestedAmount = 150L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertNotNull(result.message());
      assertTrue(result.message().contains("Quota exceeded"));
      assertTrue(result.message().contains("Current: 950"));
      assertTrue(result.message().contains("Requested: 150"));
      assertTrue(result.message().contains("Limit: 1000"));
    }

    @Test
    @DisplayName("Should deny when projected usage exceeds grace limit")
    void shouldDenyWhenProjectedUsageExceedsGraceLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 500L;
      var requestedAmount = 600L; // Projected: 1100, exceeds grace limit of 1050

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(1100L, result.projectedUsage());
    }

    @Test
    @DisplayName("Should check hasQuotaAvailable returns false when exceeded")
    void shouldCheckHasQuotaAvailableReturnsFalseWhenExceeded() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1100L; // Over grace limit

      // When
      var hasQuota = quotaEnforcer.hasQuotaAvailable(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertFalse(hasQuota);
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle zero requested amount")
    void shouldHandleZeroRequestedAmount() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 500L;
      var requestedAmount = 0L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(500L, result.remainingQuota());
    }

    @Test
    @DisplayName("Should handle exactly at limit")
    void shouldHandleExactlyAtLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 999L;
      var requestedAmount = 1L; // Exactly at limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(0L, result.remainingQuota());
      assertEquals(FREE_API_CALLS_LIMIT, result.projectedUsage());
    }

    @Test
    @DisplayName("Should handle one unit over limit")
    void shouldHandleOneUnitOverLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L;
      var requestedAmount = 1L; // One over limit, but within grace

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed()); // Within grace period
    }

    @Test
    @DisplayName("Should handle large requested amount")
    void shouldHandleLargeRequestedAmount() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 0L;
      var requestedAmount = 10000L; // Much larger than limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
    }

    @Test
    @DisplayName("Should handle very small quotas")
    void shouldHandleVerySmallQuotas() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 0L;
      var requestedAmount = 1L;

      // When - STORAGE_GB has limit of 1
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.STORAGE_GB,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(1L, result.limit());
      assertEquals(0L, result.remainingQuota());
    }

    @Test
    @DisplayName("Should handle quota with fractional grace period")
    void shouldHandleQuotaWithFractionalGracePeriod() {
      // Given - ACTIVE_USERS has limit of 5, grace = 5.25 (rounds to 5)
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 5L;
      var requestedAmount = 1L; // Would be 6, over grace limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.ACTIVE_USERS,
          currentUsage,
          requestedAmount
      );

      // Then
      // Grace limit is 5 * 1.05 = 5.25, cast to long = 5
      // So 6 > 5, should be denied
      assertFalse(result.allowed());
    }
  }

  @Nested
  @DisplayName("Multiple Metric Types Tests")
  class MultipleMetricTypesTests {

    @Test
    @DisplayName("Should enforce API_CALLS quota correctly")
    void shouldEnforceApiCallsQuotaCorrectly() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L;
      var requestedAmount = 100L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(MetricType.API_CALLS, result.metricType());
      assertEquals(FREE_API_CALLS_LIMIT, result.limit());
    }

    @Test
    @DisplayName("Should enforce STORAGE_GB quota correctly")
    void shouldEnforceStorageGbQuotaCorrectly() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1L; // At limit
      var requestedAmount = 1L; // Over limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.STORAGE_GB,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(MetricType.STORAGE_GB, result.metricType());
      assertEquals(FREE_STORAGE_GB_LIMIT, result.limit());
    }

    @Test
    @DisplayName("Should enforce EMAIL_SENDS quota correctly")
    void shouldEnforceEmailSendsQuotaCorrectly() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 100L; // At limit
      var requestedAmount = 10L; // Over grace limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.EMAIL_SENDS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(MetricType.EMAIL_SENDS, result.metricType());
      assertEquals(FREE_EMAIL_SENDS_LIMIT, result.limit());
    }

    @Test
    @DisplayName("Should enforce ACTIVE_USERS quota correctly")
    void shouldEnforceActiveUsersQuotaCorrectly() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 5L; // At limit
      var requestedAmount = 1L; // Over limit

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.ACTIVE_USERS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertFalse(result.allowed());
      assertEquals(MetricType.ACTIVE_USERS, result.metricType());
      assertEquals(FREE_MAX_USERS_LIMIT, result.limit());
    }

    @Test
    @DisplayName("Should handle different quotas independently")
    void shouldHandleDifferentQuotasIndependently() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When - Check multiple metrics
      var apiResult = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 900L, 50L);
      var storageResult = quotaEnforcer.checkQuota(
          subscription, MetricType.STORAGE_GB, 0L, 1L);
      var emailResult = quotaEnforcer.checkQuota(
          subscription, MetricType.EMAIL_SENDS, 90L, 5L);

      // Then - Each should be evaluated independently
      assertTrue(apiResult.allowed());
      assertTrue(storageResult.allowed());
      assertTrue(emailResult.allowed());

      // Verify different limits
      assertEquals(FREE_API_CALLS_LIMIT, apiResult.limit());
      assertEquals(FREE_STORAGE_GB_LIMIT, storageResult.limit());
      assertEquals(FREE_EMAIL_SENDS_LIMIT, emailResult.limit());
    }
  }

  @Nested
  @DisplayName("Unlimited Quota Tests")
  class UnlimitedQuotaTests {

    @Test
    @DisplayName("Should always allow unlimited quota regardless of usage")
    void shouldAlwaysAllowUnlimitedQuotaRegardlessOfUsage() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, unlimitedPlan);
      var currentUsage = 1000000L;
      var requestedAmount = 1000000L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertFalse(result.hasLimit());
      assertNull(result.limit());
      assertEquals(Long.MAX_VALUE, result.remainingQuota());
      assertEquals("Unlimited quota", result.message());
    }

    @Test
    @DisplayName("Should return zero percentage for unlimited quota")
    void shouldReturnZeroPercentageForUnlimitedQuota() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, unlimitedPlan);
      var currentUsage = 1000000L;

      // When
      var percentage = quotaEnforcer.getQuotaUsagePercentage(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertEquals(0.0, percentage);
    }

    @Test
    @DisplayName("Should never approach limit for unlimited quota")
    void shouldNeverApproachLimitForUnlimitedQuota() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, unlimitedPlan);
      var currentUsage = 1000000L;

      // When
      var isApproaching = quotaEnforcer.isApproachingLimit(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertFalse(isApproaching);
    }

    @Test
    @DisplayName("Should always have quota available for unlimited")
    void shouldAlwaysHaveQuotaAvailableForUnlimited() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, unlimitedPlan);
      var currentUsage = 1000000L;

      // When
      var hasQuota = quotaEnforcer.hasQuotaAvailable(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertTrue(hasQuota);
    }

    @Test
    @DisplayName("Should return max value for remaining quota on unlimited")
    void shouldReturnMaxValueForRemainingQuotaOnUnlimited() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, unlimitedPlan);
      var currentUsage = 1000000L;

      // When
      var remaining = quotaEnforcer.getRemainingQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertEquals(Long.MAX_VALUE, remaining);
    }
  }

  @Nested
  @DisplayName("Quota Status and Monitoring Tests")
  class QuotaStatusAndMonitoringTests {

    @Test
    @DisplayName("Should correctly identify when approaching limit at 90%")
    void shouldCorrectlyIdentifyWhenApproachingLimitAt90Percent() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 900L; // Exactly 90%

      // When
      var isApproaching = quotaEnforcer.isApproachingLimit(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertTrue(isApproaching);
    }

    @Test
    @DisplayName("Should not flag as approaching limit below 90%")
    void shouldNotFlagAsApproachingLimitBelow90Percent() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 899L; // Just below 90%

      // When
      var isApproaching = quotaEnforcer.isApproachingLimit(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertFalse(isApproaching);
    }

    @Test
    @DisplayName("Should calculate usage percentage accurately")
    void shouldCalculateUsagePercentageAccurately() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // Test various percentages
      assertEquals(0.0, quotaEnforcer.getQuotaUsagePercentage(
          subscription, MetricType.API_CALLS, 0L));
      assertEquals(25.0, quotaEnforcer.getQuotaUsagePercentage(
          subscription, MetricType.API_CALLS, 250L));
      assertEquals(50.0, quotaEnforcer.getQuotaUsagePercentage(
          subscription, MetricType.API_CALLS, 500L));
      assertEquals(75.0, quotaEnforcer.getQuotaUsagePercentage(
          subscription, MetricType.API_CALLS, 750L));
      assertEquals(100.0, quotaEnforcer.getQuotaUsagePercentage(
          subscription, MetricType.API_CALLS, 1000L));
      assertEquals(110.0, quotaEnforcer.getQuotaUsagePercentage(
          subscription, MetricType.API_CALLS, 1100L));
    }

    @Test
    @DisplayName("Should calculate remaining quota correctly at various levels")
    void shouldCalculateRemainingQuotaCorrectlyAtVariousLevels() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // Test various usage levels
      assertEquals(1000L, quotaEnforcer.getRemainingQuota(
          subscription, MetricType.API_CALLS, 0L));
      assertEquals(750L, quotaEnforcer.getRemainingQuota(
          subscription, MetricType.API_CALLS, 250L));
      assertEquals(500L, quotaEnforcer.getRemainingQuota(
          subscription, MetricType.API_CALLS, 500L));
      assertEquals(250L, quotaEnforcer.getRemainingQuota(
          subscription, MetricType.API_CALLS, 750L));
      assertEquals(0L, quotaEnforcer.getRemainingQuota(
          subscription, MetricType.API_CALLS, 1000L));
      assertEquals(0L, quotaEnforcer.getRemainingQuota(
          subscription, MetricType.API_CALLS, 1100L)); // Over limit returns 0
    }

    @Test
    @DisplayName("Should check quota availability correctly")
    void shouldCheckQuotaAvailabilityCorrectly() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When & Then
      assertTrue(quotaEnforcer.hasQuotaAvailable(
          subscription, MetricType.API_CALLS, 0L));
      assertTrue(quotaEnforcer.hasQuotaAvailable(
          subscription, MetricType.API_CALLS, 500L));
      assertTrue(quotaEnforcer.hasQuotaAvailable(
          subscription, MetricType.API_CALLS, 999L));
      assertTrue(quotaEnforcer.hasQuotaAvailable(
          subscription, MetricType.API_CALLS, 1000L)); // At limit, within grace
      assertTrue(quotaEnforcer.hasQuotaAvailable(
          subscription, MetricType.API_CALLS, 1049L)); // Within grace
      assertFalse(quotaEnforcer.hasQuotaAvailable(
          subscription, MetricType.API_CALLS, 1050L)); // At grace limit
    }
  }

  @Nested
  @DisplayName("Different Plan Tiers Tests")
  class DifferentPlanTiersTests {

    @Test
    @DisplayName("Should enforce different limits for FREE tier")
    void shouldEnforceDifferentLimitsForFreeTier() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When
      var apiResult = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 1000L, 1L);
      var storageResult = quotaEnforcer.checkQuota(
          subscription, MetricType.STORAGE_GB, 1L, 1L);
      var emailResult = quotaEnforcer.checkQuota(
          subscription, MetricType.EMAIL_SENDS, 100L, 1L);

      // Then
      assertTrue(apiResult.allowed()); // 1001 <= 1050 (within grace)
      assertFalse(storageResult.allowed()); // 2 > 1.05 (over grace)
      assertTrue(emailResult.allowed()); // 101 <= 105 (within grace)
    }

    @Test
    @DisplayName("Should enforce different limits for PRO tier")
    void shouldEnforceDifferentLimitsForProTier() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, proPlan);

      // When - PRO tier has higher limits
      var apiResult = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 10000L, 1000L);

      // Then - Should allow more usage than FREE tier
      assertTrue(apiResult.allowed());
      assertTrue(apiResult.limit() > FREE_API_CALLS_LIMIT);
    }

    @Test
    @DisplayName("Should handle ENTERPRISE tier with unlimited quotas")
    void shouldHandleEnterpriseTierWithUnlimitedQuotas() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, unlimitedPlan);

      // When
      var result = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 1000000L, 1000000L);

      // Then
      assertTrue(result.allowed());
      assertFalse(result.hasLimit());
    }
  }

  @Nested
  @DisplayName("Boundary Condition Tests")
  class BoundaryConditionTests {

    @Test
    @DisplayName("Should handle minimum values (zero usage, zero request)")
    void shouldHandleMinimumValues() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When
      var result = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 0L, 0L);

      // Then
      assertTrue(result.allowed());
      assertEquals(0L, result.currentUsage());
      assertEquals(0L, result.requestedAmount());
      assertEquals(FREE_API_CALLS_LIMIT, result.remainingQuota());
    }

    @Test
    @DisplayName("Should handle maximum single request within limit")
    void shouldHandleMaximumSingleRequestWithinLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When - Request entire quota at once
      var result = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 0L, FREE_API_CALLS_LIMIT);

      // Then
      assertTrue(result.allowed());
      assertEquals(0L, result.remainingQuota());
    }

    @Test
    @DisplayName("Should handle request exactly at grace boundary")
    void shouldHandleRequestExactlyAtGraceBoundary() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var graceLimit = (long) (FREE_API_CALLS_LIMIT * 1.05);

      // When - Request exactly to grace limit
      var result = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 0L, graceLimit);

      // Then
      assertTrue(result.allowed());
    }

    @Test
    @DisplayName("Should handle request one over grace boundary")
    void shouldHandleRequestOneOverGraceBoundary() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var graceLimit = (long) (FREE_API_CALLS_LIMIT * 1.05);

      // When - Request one over grace limit
      var result = quotaEnforcer.checkQuota(
          subscription, MetricType.API_CALLS, 0L, graceLimit + 1);

      // Then
      assertFalse(result.allowed());
    }
  }
}
