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
 * Unit tests for QuotaEnforcer domain service.
 * Tests quota checking and enforcement logic.
 */
@DisplayName("QuotaEnforcer Domain Service Tests")
class QuotaEnforcerTest {

  private QuotaEnforcer quotaEnforcer;
  private UUID tenantId;
  private UUID userId;
  private SubscriptionPlan freePlan;
  private SubscriptionPlan proPlan;
  private SubscriptionPlan unlimitedPlan;

  @BeforeEach
  void setUp() {
    quotaEnforcer = new QuotaEnforcer();
    tenantId = UUID.randomUUID();
    userId = UUID.randomUUID();

    freePlan = SubscriptionPlan.create(
        "FREE",
        "Free Plan",
        "Free tier",
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

    unlimitedPlan = SubscriptionPlan.create(
        "ENTERPRISE",
        "Enterprise",
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
  }

  @Nested
  @DisplayName("Quota Check Tests")
  class QuotaCheckTests {

    @Test
    @DisplayName("Should allow operation when within quota")
    void shouldAllowOperationWhenWithinQuota() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 500L;
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
      assertFalse(result.isExceeded());
      assertEquals(MetricType.API_CALLS, result.metricType());
      assertEquals(currentUsage, result.currentUsage());
      assertEquals(requestedAmount, result.requestedAmount());
      assertTrue(result.remainingQuota() > 0);
    }

    @Test
    @DisplayName("Should deny operation when quota exceeded")
    void shouldDenyOperationWhenQuotaExceeded() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      // FREE tier has 1000 API calls, with 5% grace = 1050
      // Use 950 + 150 = 1100 to exceed grace limit
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
      assertEquals(MetricType.API_CALLS, result.metricType());
      assertEquals(currentUsage, result.currentUsage());
      assertEquals(requestedAmount, result.requestedAmount());
    }

    @Test
    @DisplayName("Should allow operation within grace period")
    void shouldAllowOperationWithinGracePeriod() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1000L; // At limit
      var requestedAmount = 20L; // Within 5% grace period

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
    }

    @Test
    @DisplayName("Should always allow unlimited quota")
    void shouldAlwaysAllowUnlimitedQuota() {
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
  @DisplayName("Quota Enforcement Tests")
  class QuotaEnforcementTests {

    @Test
    @DisplayName("Should not throw exception when quota is available")
    void shouldNotThrowExceptionWhenQuotaIsAvailable() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 500L;
      var requestedAmount = 100L;

      // When & Then
      assertDoesNotThrow(() ->
          quotaEnforcer.enforceQuota(
              subscription,
              MetricType.API_CALLS,
              currentUsage,
              requestedAmount
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when quota is exceeded")
    void shouldThrowExceptionWhenQuotaIsExceeded() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      // FREE tier has 1000 API calls, with 5% grace = 1050
      // Use 950 + 150 = 1100 to exceed grace limit
      var currentUsage = 950L;
      var requestedAmount = 150L;

      // When & Then
      assertThrows(
          QuotaEnforcer.QuotaExceededException.class,
          () -> quotaEnforcer.enforceQuota(
              subscription,
              MetricType.API_CALLS,
              currentUsage,
              requestedAmount
          )
      );
    }
  }

  @Nested
  @DisplayName("Remaining Quota Tests")
  class RemainingQuotaTests {

    @Test
    @DisplayName("Should calculate remaining quota correctly")
    void shouldCalculateRemainingQuotaCorrectly() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 600L;

      // When
      var remaining = quotaEnforcer.getRemainingQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertEquals(400L, remaining);
    }

    @Test
    @DisplayName("Should return zero when quota is exceeded")
    void shouldReturnZeroWhenQuotaIsExceeded() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 1500L;

      // When
      var remaining = quotaEnforcer.getRemainingQuota(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertEquals(0L, remaining);
    }

    @Test
    @DisplayName("Should return max value for unlimited quota")
    void shouldReturnMaxValueForUnlimitedQuota() {
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
  @DisplayName("Quota Status Tests")
  class QuotaStatusTests {

    @Test
    @DisplayName("Should check if quota is available")
    void shouldCheckIfQuotaIsAvailable() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 500L;

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
    @DisplayName("Should check if approaching limit")
    void shouldCheckIfApproachingLimit() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsageNearLimit = 920L; // 92% of 1000
      var currentUsageBelowThreshold = 800L; // 80% of 1000

      // When
      var isApproachingLimit = quotaEnforcer.isApproachingLimit(
          subscription,
          MetricType.API_CALLS,
          currentUsageNearLimit
      );

      var isNotApproachingLimit = quotaEnforcer.isApproachingLimit(
          subscription,
          MetricType.API_CALLS,
          currentUsageBelowThreshold
      );

      // Then
      assertTrue(isApproachingLimit);
      assertFalse(isNotApproachingLimit);
    }

    @Test
    @DisplayName("Should calculate quota usage percentage")
    void shouldCalculateQuotaUsagePercentage() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 750L;

      // When
      var percentage = quotaEnforcer.getQuotaUsagePercentage(
          subscription,
          MetricType.API_CALLS,
          currentUsage
      );

      // Then
      assertEquals(75.0, percentage, 0.01);
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
      assertEquals(0.0, percentage, 0.01);
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
          () -> quotaEnforcer.checkQuota(
              null,
              MetricType.API_CALLS,
              100L,
              10L
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when metric type is null")
    void shouldThrowExceptionWhenMetricTypeIsNull() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> quotaEnforcer.checkQuota(
              subscription,
              null,
              100L,
              10L
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when current usage is negative")
    void shouldThrowExceptionWhenCurrentUsageIsNegative() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> quotaEnforcer.checkQuota(
              subscription,
              MetricType.API_CALLS,
              -100L,
              10L
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when requested quantity is negative")
    void shouldThrowExceptionWhenRequestedQuantityIsNegative() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> quotaEnforcer.checkQuota(
              subscription,
              MetricType.API_CALLS,
              100L,
              -10L
          )
      );
    }
  }

  @Nested
  @DisplayName("Different Metric Types Tests")
  class DifferentMetricTypesTests {

    @Test
    @DisplayName("Should check storage quota")
    void shouldCheckStorageQuota() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 0L;
      var requestedAmount = 1L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.STORAGE_GB,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(MetricType.STORAGE_GB, result.metricType());
    }

    @Test
    @DisplayName("Should check email sends quota")
    void shouldCheckEmailSendsQuota() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 50L;
      var requestedAmount = 10L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.EMAIL_SENDS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(MetricType.EMAIL_SENDS, result.metricType());
    }

    @Test
    @DisplayName("Should check active users quota")
    void shouldCheckActiveUsersQuota() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, freePlan);
      var currentUsage = 3L;
      var requestedAmount = 1L;

      // When
      var result = quotaEnforcer.checkQuota(
          subscription,
          MetricType.ACTIVE_USERS,
          currentUsage,
          requestedAmount
      );

      // Then
      assertTrue(result.allowed());
      assertEquals(MetricType.ACTIVE_USERS, result.metricType());
    }
  }
}
