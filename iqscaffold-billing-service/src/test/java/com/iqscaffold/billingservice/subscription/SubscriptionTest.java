package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Subscription aggregate root.
 * Tests business logic, invariants, and state transitions.
 */
@DisplayName("Subscription Aggregate Tests")
class SubscriptionTest {

  private UUID tenantId;
  private UUID userId;
  private SubscriptionPlan trialPlan;
  private SubscriptionPlan monthlyPlan;
  private SubscriptionPlan yearlyPlan;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    userId = UUID.randomUUID();

    // Create test plans
    trialPlan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Monthly",
        "Pro plan with monthly billing",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("29.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.proTier(),
        14,
        true
    );

    monthlyPlan = SubscriptionPlan.create(
        "PRO_MONTHLY_NO_TRIAL",
        "Pro Monthly",
        "Pro plan without trial",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("29.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.proTier(),
        0,
        true
    );

    yearlyPlan = SubscriptionPlan.create(
        "PRO_YEARLY",
        "Pro Yearly",
        "Pro plan with yearly billing",
        PlanTier.PRO,
        BillingCycle.YEARLY,
        new BigDecimal("299.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.proTier(),
        0,
        true
    );
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create trial subscription with valid parameters")
    void shouldCreateTrialSubscription() {
      // When
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.TRIAL, subscription.getStatus());
      assertEquals(tenantId, subscription.getTenantId());
      assertEquals(userId, subscription.getUserId());
      assertEquals(trialPlan, subscription.getPlan());
      assertNotNull(subscription.getTrialStart());
      assertNotNull(subscription.getTrialEnd());
      assertNotNull(subscription.getCurrentPeriodStart());
      assertNotNull(subscription.getCurrentPeriodEnd());
      assertEquals(subscription.getTrialEnd(), subscription.getCurrentPeriodEnd());
      assertFalse(subscription.getCancelAtPeriodEnd());
    }

    @Test
    @DisplayName("Should create active subscription with valid parameters")
    void shouldCreateActiveSubscription() {
      // When
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
      assertEquals(tenantId, subscription.getTenantId());
      assertEquals(userId, subscription.getUserId());
      assertEquals(monthlyPlan, subscription.getPlan());
      assertNull(subscription.getTrialStart());
      assertNull(subscription.getTrialEnd());
      assertNotNull(subscription.getCurrentPeriodStart());
      assertNotNull(subscription.getCurrentPeriodEnd());
      assertFalse(subscription.getCancelAtPeriodEnd());
    }

    @Test
    @DisplayName("Should create incomplete subscription with valid parameters")
    void shouldCreateIncompleteSubscription() {
      // When
      var subscription = Subscription.createIncomplete(tenantId, userId, monthlyPlan);

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.INCOMPLETE, subscription.getStatus());
      assertEquals(tenantId, subscription.getTenantId());
      assertEquals(userId, subscription.getUserId());
      assertEquals(monthlyPlan, subscription.getPlan());
      assertFalse(subscription.getCancelAtPeriodEnd());
    }

    @Test
    @DisplayName("Should throw exception when creating trial with plan that has no trial")
    void shouldThrowExceptionWhenCreatingTrialWithoutTrialPlan() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Subscription.createTrial(tenantId, userId, monthlyPlan)
      );
    }

    @Test
    @DisplayName("Should throw exception when tenant ID is null")
    void shouldThrowExceptionWhenTenantIdIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Subscription.createTrial(null, userId, trialPlan)
      );
    }

    @Test
    @DisplayName("Should throw exception when user ID is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Subscription.createTrial(tenantId, null, trialPlan)
      );
    }

    @Test
    @DisplayName("Should throw exception when plan is null")
    void shouldThrowExceptionWhenPlanIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> Subscription.createTrial(tenantId, userId, null)
      );
    }
  }

  @Nested
  @DisplayName("Status Transition Tests")
  class StatusTransitionTests {

    @Test
    @DisplayName("Should convert trial to active")
    void shouldConvertTrialToActive() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When
      subscription.convertTrialToActive();

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
      assertNotNull(subscription.getCurrentPeriodStart());
      assertNotNull(subscription.getCurrentPeriodEnd());
    }

    @Test
    @DisplayName("Should throw exception when converting non-trial to active")
    void shouldThrowExceptionWhenConvertingNonTrialToActive() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::convertTrialToActive
      );
    }

    @Test
    @DisplayName("Should mark active subscription as past due")
    void shouldMarkActiveSubscriptionAsPastDue() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.markPastDue();

      // Then
      assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when marking non-active subscription as past due")
    void shouldThrowExceptionWhenMarkingNonActiveAsPastDue() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::markPastDue
      );
    }

    @Test
    @DisplayName("Should reactivate past due subscription")
    void shouldReactivatePastDueSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();

      // When
      subscription.reactivateFromPastDue();

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when reactivating non-past-due subscription")
    void shouldThrowExceptionWhenReactivatingNonPastDue() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::reactivateFromPastDue
      );
    }

    @Test
    @DisplayName("Should suspend active subscription")
    void shouldSuspendActiveSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.suspend();

      // Then
      assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
    }

    @Test
    @DisplayName("Should unsuspend suspended subscription")
    void shouldUnsuspendSuspendedSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();

      // When
      subscription.unsuspend();

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when unsuspending non-suspended subscription")
    void shouldThrowExceptionWhenUnsuspendingNonSuspended() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::unsuspend
      );
    }

    @Test
    @DisplayName("Should expire subscription")
    void shouldExpireSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.expire();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }
  }

  @Nested
  @DisplayName("Cancellation Tests")
  class CancellationTests {

    @Test
    @DisplayName("Should cancel subscription at period end")
    void shouldCancelSubscriptionAtPeriodEnd() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.cancelAtPeriodEnd();

      // Then
      assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
      assertTrue(subscription.getCancelAtPeriodEnd());
      assertNotNull(subscription.getCanceledAt());
    }

    @Test
    @DisplayName("Should cancel subscription immediately")
    void shouldCancelSubscriptionImmediately() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.cancelImmediately();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
      assertFalse(subscription.getCancelAtPeriodEnd());
      assertNotNull(subscription.getCanceledAt());
    }

    @Test
    @DisplayName("Should reactivate canceled subscription")
    void shouldReactivateCanceledSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();

      // When
      subscription.reactivate();

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
      assertFalse(subscription.getCancelAtPeriodEnd());
      assertNull(subscription.getCanceledAt());
    }

    @Test
    @DisplayName("Should throw exception when canceling expired subscription")
    void shouldThrowExceptionWhenCancelingExpiredSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.expire();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::cancelAtPeriodEnd
      );
    }
  }

  @Nested
  @DisplayName("Plan Change Tests")
  class PlanChangeTests {

    @Test
    @DisplayName("Should change plan for active subscription")
    void shouldChangePlanForActiveSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.changePlan(yearlyPlan);

      // Then
      assertEquals(yearlyPlan, subscription.getPlan());
      assertNotNull(subscription.getCurrentPeriodStart());
      assertNotNull(subscription.getCurrentPeriodEnd());
    }

    @Test
    @DisplayName("Should throw exception when changing plan for non-active subscription")
    void shouldThrowExceptionWhenChangingPlanForNonActiveSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.expire();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          () -> subscription.changePlan(yearlyPlan)
      );
    }

    @Test
    @DisplayName("Should throw exception when new plan is null")
    void shouldThrowExceptionWhenNewPlanIsNull() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscription.changePlan(null)
      );
    }
  }

  @Nested
  @DisplayName("Trial Management Tests")
  class TrialManagementTests {

    @Test
    @DisplayName("Should extend trial period")
    void shouldExtendTrialPeriod() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);
      var originalTrialEnd = subscription.getTrialEnd();

      // When
      subscription.extendTrial(7);

      // Then
      assertEquals(originalTrialEnd.plusDays(7), subscription.getTrialEnd());
      assertEquals(subscription.getTrialEnd(), subscription.getCurrentPeriodEnd());
    }

    @Test
    @DisplayName("Should throw exception when extending trial for non-trial subscription")
    void shouldThrowExceptionWhenExtendingTrialForNonTrialSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          () -> subscription.extendTrial(7)
      );
    }

    @Test
    @DisplayName("Should throw exception when extending trial with negative days")
    void shouldThrowExceptionWhenExtendingTrialWithNegativeDays() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscription.extendTrial(-1)
      );
    }

    @Test
    @DisplayName("Should check if subscription is in trial")
    void shouldCheckIfSubscriptionIsInTrial() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // Then
      assertTrue(subscription.isInTrial());
    }

    @Test
    @DisplayName("Should return false for isInTrial when not in trial status")
    void shouldReturnFalseForIsInTrialWhenNotInTrialStatus() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // Then
      assertFalse(subscription.isInTrial());
    }
  }

  @Nested
  @DisplayName("Renewal Tests")
  class RenewalTests {

    @Test
    @DisplayName("Should renew active subscription")
    void shouldRenewActiveSubscription() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      var originalPeriodEnd = subscription.getCurrentPeriodEnd();

      // When
      subscription.renew();

      // Then
      assertEquals(originalPeriodEnd, subscription.getCurrentPeriodStart());
      assertTrue(subscription.getCurrentPeriodEnd().isAfter(originalPeriodEnd));
    }

    @Test
    @DisplayName("Should throw exception when renewing non-active subscription")
    void shouldThrowExceptionWhenRenewingNonActiveSubscription() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::renew
      );
    }
  }

  @Nested
  @DisplayName("Metadata Tests")
  class MetadataTests {

    @Test
    @DisplayName("Should add metadata")
    void shouldAddMetadata() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.addMetadata("key1", "value1");
      subscription.addMetadata("key2", 123);

      // Then
      var metadata = subscription.getMetadata();
      assertEquals("value1", metadata.get("key1"));
      assertEquals(123, metadata.get("key2"));
    }

    @Test
    @DisplayName("Should remove metadata")
    void shouldRemoveMetadata() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.addMetadata("key1", "value1");

      // When
      subscription.removeMetadata("key1");

      // Then
      var metadata = subscription.getMetadata();
      assertFalse(metadata.containsKey("key1"));
    }

    @Test
    @DisplayName("Should return defensive copy of metadata")
    void shouldReturnDefensiveCopyOfMetadata() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.addMetadata("key1", "value1");

      // When
      var metadata = subscription.getMetadata();
      metadata.put("key2", "value2");

      // Then
      var actualMetadata = subscription.getMetadata();
      assertFalse(actualMetadata.containsKey("key2"));
    }
  }

  @Nested
  @DisplayName("Query Methods Tests")
  class QueryMethodsTests {

    @Test
    @DisplayName("Should check if subscription has access")
    void shouldCheckIfSubscriptionHasAccess() {
      // Given
      var activeSubscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      var expiredSubscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      expiredSubscription.expire();

      // Then
      assertTrue(activeSubscription.hasAccess());
      assertFalse(expiredSubscription.hasAccess());
    }

    @Test
    @DisplayName("Should check if subscription is active billing")
    void shouldCheckIfSubscriptionIsActiveBilling() {
      // Given
      var activeSubscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      var trialSubscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // Then
      assertTrue(activeSubscription.isActiveBilling());
      assertFalse(trialSubscription.isActiveBilling());
    }

    @Test
    @DisplayName("Should get days remaining in period")
    void shouldGetDaysRemainingInPeriod() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      var daysRemaining = subscription.getDaysRemainingInPeriod();

      // Then
      assertTrue(daysRemaining > 0);
    }
  }
}
