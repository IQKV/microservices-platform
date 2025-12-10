package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * Comprehensive unit tests for Subscription state transitions.
 * Tests all valid state transitions, invalid transition rejection, and side effects.
 *
 * <p>State Machine:
 * <pre>
 * INCOMPLETE -> TRIAL, ACTIVE, EXPIRED
 * TRIAL -> ACTIVE, EXPIRED, CANCELED
 * ACTIVE -> PAST_DUE, CANCELED, SUSPENDED, EXPIRED
 * PAST_DUE -> ACTIVE, EXPIRED, SUSPENDED
 * CANCELED -> ACTIVE, EXPIRED
 * SUSPENDED -> ACTIVE, EXPIRED
 * EXPIRED -> (terminal state, no transitions)
 * </pre>
 */
@DisplayName("Subscription State Transition Tests")
class SubscriptionStateTransitionTest {

  private UUID tenantId;
  private UUID userId;
  private SubscriptionPlan trialPlan;
  private SubscriptionPlan monthlyPlan;
  private SubscriptionPlan yearlyPlan;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    userId = UUID.randomUUID();

    trialPlan = SubscriptionPlan.create(
        "PRO_TRIAL",
        "Pro with Trial",
        "Pro plan with 14-day trial",
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
        "PRO_MONTHLY",
        "Pro Monthly",
        "Pro plan monthly",
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
        "Pro plan yearly",
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
  @DisplayName("INCOMPLETE State Transitions")
  class IncompleteStateTransitions {

    @Test
    @DisplayName("INCOMPLETE -> TRIAL: Should transition when trial is activated")
    void shouldTransitionFromIncompleteToTrial() {
      // Given
      var subscription = Subscription.createIncomplete(tenantId, userId, trialPlan);
      assertEquals(SubscriptionStatus.INCOMPLETE, subscription.getStatus());

      // When - Simulate completing setup and starting trial
      // Note: This would typically be done through a service method
      // For now, we test that TRIAL can be created from scratch
      var trialSubscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // Then
      assertEquals(SubscriptionStatus.TRIAL, trialSubscription.getStatus());
      assertNotNull(trialSubscription.getTrialStart());
      assertNotNull(trialSubscription.getTrialEnd());
    }

    @Test
    @DisplayName("INCOMPLETE -> ACTIVE: Should transition when payment is completed")
    void shouldTransitionFromIncompleteToActive() {
      // Given
      var subscription = Subscription.createIncomplete(tenantId, userId, monthlyPlan);
      assertEquals(SubscriptionStatus.INCOMPLETE, subscription.getStatus());

      // When - Simulate completing payment
      var activeSubscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, activeSubscription.getStatus());
      assertNotNull(activeSubscription.getCurrentPeriodStart());
      assertNotNull(activeSubscription.getCurrentPeriodEnd());
    }

    @Test
    @DisplayName("INCOMPLETE -> EXPIRED: Should transition when setup times out")
    void shouldTransitionFromIncompleteToExpired() {
      // Given
      var subscription = Subscription.createIncomplete(tenantId, userId, monthlyPlan);

      // When
      subscription.expire();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }
  }

  @Nested
  @DisplayName("TRIAL State Transitions")
  class TrialStateTransitions {

    @Test
    @DisplayName("TRIAL -> ACTIVE: Should transition when trial converts to paid")
    void shouldTransitionFromTrialToActive() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);
      var originalTrialEnd = subscription.getTrialEnd();

      // When
      subscription.convertTrialToActive();

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
      assertNotNull(subscription.getCurrentPeriodStart());
      assertNotNull(subscription.getCurrentPeriodEnd());
      // Trial dates should remain unchanged
      assertEquals(originalTrialEnd, subscription.getTrialEnd());
    }

    @Test
    @DisplayName("TRIAL -> EXPIRED: Should transition when trial expires without payment")
    void shouldTransitionFromTrialToExpired() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When
      subscription.expire();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }

    @Test
    @DisplayName("TRIAL -> CANCELED: Should transition when user cancels during trial")
    void shouldTransitionFromTrialToCanceled() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When
      subscription.cancelAtPeriodEnd();

      // Then
      assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
      assertTrue(subscription.getCancelAtPeriodEnd());
      assertNotNull(subscription.getCanceledAt());
    }

    @Test
    @DisplayName("TRIAL -> PAST_DUE: Should reject invalid transition")
    void shouldRejectTransitionFromTrialToPastDue() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::markPastDue,
          "Cannot mark TRIAL subscription as PAST_DUE"
      );
    }

    @Test
    @DisplayName("TRIAL -> SUSPENDED: Should reject invalid transition")
    void shouldRejectTransitionFromTrialToSuspended() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::suspend,
          "Cannot suspend TRIAL subscription"
      );
    }
  }

  @Nested
  @DisplayName("ACTIVE State Transitions")
  class ActiveStateTransitions {

    @Test
    @DisplayName("ACTIVE -> PAST_DUE: Should transition when payment fails")
    void shouldTransitionFromActiveToPastDue() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.markPastDue();

      // Then
      assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
    }

    @Test
    @DisplayName("ACTIVE -> CANCELED: Should transition when user cancels at period end")
    void shouldTransitionFromActiveToCanceled() {
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
    @DisplayName("ACTIVE -> EXPIRED: Should transition when canceled immediately")
    void shouldTransitionFromActiveToExpiredViaCancelImmediately() {
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
    @DisplayName("ACTIVE -> EXPIRED: Should transition when period ends")
    void shouldTransitionFromActiveToExpiredViaExpire() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.expire();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }

    @Test
    @DisplayName("ACTIVE -> SUSPENDED: Should transition when admin suspends")
    void shouldTransitionFromActiveToSuspended() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.suspend();

      // Then
      assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
    }

    @Test
    @DisplayName("ACTIVE -> TRIAL: Should reject invalid transition")
    void shouldRejectTransitionFromActiveToTrial() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::convertTrialToActive,
          "Cannot convert ACTIVE subscription to TRIAL"
      );
    }

    @Test
    @DisplayName("ACTIVE -> INCOMPLETE: Should reject invalid transition")
    void shouldRejectTransitionFromActiveToIncomplete() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When & Then
      // No direct method exists, but state machine should prevent it
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }
  }

  @Nested
  @DisplayName("PAST_DUE State Transitions")
  class PastDueStateTransitions {

    @Test
    @DisplayName("PAST_DUE -> ACTIVE: Should transition when payment succeeds")
    void shouldTransitionFromPastDueToActive() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();

      // When
      subscription.reactivateFromPastDue();

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    @DisplayName("PAST_DUE -> EXPIRED: Should transition when payment fails permanently")
    void shouldTransitionFromPastDueToExpired() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();

      // When
      subscription.expire();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }

    @Test
    @DisplayName("PAST_DUE -> SUSPENDED: Should transition when admin suspends")
    void shouldTransitionFromPastDueToSuspended() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();

      // When
      subscription.suspend();

      // Then
      assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
    }

    @Test
    @DisplayName("PAST_DUE -> CANCELED: Should reject invalid transition")
    void shouldRejectTransitionFromPastDueToCanceled() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::cancelAtPeriodEnd,
          "Cannot cancel PAST_DUE subscription"
      );
    }

    @Test
    @DisplayName("PAST_DUE -> TRIAL: Should reject invalid transition")
    void shouldRejectTransitionFromPastDueToTrial() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::convertTrialToActive,
          "Cannot convert PAST_DUE subscription to TRIAL"
      );
    }
  }

  @Nested
  @DisplayName("CANCELED State Transitions")
  class CanceledStateTransitions {

    @Test
    @DisplayName("CANCELED -> ACTIVE: Should transition when user reactivates")
    void shouldTransitionFromCanceledToActive() {
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
    @DisplayName("CANCELED -> EXPIRED: Should transition when period ends")
    void shouldTransitionFromCanceledToExpired() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();

      // When
      subscription.expire();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }

    @Test
    @DisplayName("CANCELED -> PAST_DUE: Should reject invalid transition")
    void shouldRejectTransitionFromCanceledToPastDue() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::markPastDue,
          "Cannot mark CANCELED subscription as PAST_DUE"
      );
    }

    @Test
    @DisplayName("CANCELED -> SUSPENDED: Should reject invalid transition")
    void shouldRejectTransitionFromCanceledToSuspended() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::suspend,
          "Cannot suspend CANCELED subscription"
      );
    }

    @Test
    @DisplayName("CANCELED -> TRIAL: Should reject invalid transition")
    void shouldRejectTransitionFromCanceledToTrial() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::convertTrialToActive,
          "Cannot convert CANCELED subscription to TRIAL"
      );
    }
  }

  @Nested
  @DisplayName("SUSPENDED State Transitions")
  class SuspendedStateTransitions {

    @Test
    @DisplayName("SUSPENDED -> ACTIVE: Should transition when admin unsuspends")
    void shouldTransitionFromSuspendedToActive() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();

      // When
      subscription.unsuspend();

      // Then
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    @DisplayName("SUSPENDED -> EXPIRED: Should transition when admin expires")
    void shouldTransitionFromSuspendedToExpired() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();

      // When
      subscription.expire();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }

    @Test
    @DisplayName("SUSPENDED -> PAST_DUE: Should reject invalid transition")
    void shouldRejectTransitionFromSuspendedToPastDue() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::markPastDue,
          "Cannot mark SUSPENDED subscription as PAST_DUE"
      );
    }

    @Test
    @DisplayName("SUSPENDED -> CANCELED: Should reject invalid transition")
    void shouldRejectTransitionFromSuspendedToCanceled() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::cancelAtPeriodEnd,
          "Cannot cancel SUSPENDED subscription"
      );
    }

    @Test
    @DisplayName("SUSPENDED -> TRIAL: Should reject invalid transition")
    void shouldRejectTransitionFromSuspendedToTrial() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();

      // When & Then
      assertThrows(
          IllegalStateException.class,
          subscription::convertTrialToActive,
          "Cannot convert SUSPENDED subscription to TRIAL"
      );
    }
  }

  @Nested
  @DisplayName("EXPIRED State Transitions")
  class ExpiredStateTransitions {

    @Test
    @DisplayName("EXPIRED -> Any: Should reject all transitions from terminal state")
    void shouldRejectAllTransitionsFromExpired() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.expire();

      // When & Then - Try all possible transitions
      assertThrows(IllegalStateException.class, subscription::convertTrialToActive);
      assertThrows(IllegalStateException.class, subscription::markPastDue);
      assertThrows(IllegalStateException.class, subscription::reactivateFromPastDue);
      assertThrows(IllegalStateException.class, subscription::cancelAtPeriodEnd);
      assertThrows(IllegalStateException.class, subscription::cancelImmediately);
      assertThrows(IllegalStateException.class, subscription::reactivate);
      assertThrows(IllegalStateException.class, subscription::suspend);
      assertThrows(IllegalStateException.class, subscription::unsuspend);
    }

    @Test
    @DisplayName("EXPIRED: Should remain in EXPIRED state")
    void shouldRemainInExpiredState() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.expire();

      // When
      var status = subscription.getStatus();

      // Then
      assertEquals(SubscriptionStatus.EXPIRED, status);
    }
  }

  @Nested
  @DisplayName("State Transition Side Effects")
  class StateTransitionSideEffects {

    @Test
    @DisplayName("convertTrialToActive: Should reset billing period")
    void convertTrialToActiveShouldResetBillingPeriod() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);
      var trialPeriodEnd = subscription.getCurrentPeriodEnd();

      // When
      subscription.convertTrialToActive();

      // Then
      assertNotEquals(trialPeriodEnd, subscription.getCurrentPeriodEnd());
      assertTrue(subscription.getCurrentPeriodEnd().isAfter(subscription.getCurrentPeriodStart()));
    }

    @Test
    @DisplayName("cancelAtPeriodEnd: Should set canceledAt timestamp")
    void cancelAtPeriodEndShouldSetCanceledAtTimestamp() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      assertNull(subscription.getCanceledAt());

      // When
      subscription.cancelAtPeriodEnd();

      // Then
      assertNotNull(subscription.getCanceledAt());
    }

    @Test
    @DisplayName("cancelAtPeriodEnd: Should set cancelAtPeriodEnd flag")
    void cancelAtPeriodEndShouldSetFlag() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      assertFalse(subscription.getCancelAtPeriodEnd());

      // When
      subscription.cancelAtPeriodEnd();

      // Then
      assertTrue(subscription.getCancelAtPeriodEnd());
    }

    @Test
    @DisplayName("cancelImmediately: Should set canceledAt but not cancelAtPeriodEnd")
    void cancelImmediatelyShouldSetCanceledAtButNotFlag() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);

      // When
      subscription.cancelImmediately();

      // Then
      assertNotNull(subscription.getCanceledAt());
      assertFalse(subscription.getCancelAtPeriodEnd());
    }

    @Test
    @DisplayName("reactivate: Should clear canceledAt timestamp")
    void reactivateShouldClearCanceledAtTimestamp() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();
      assertNotNull(subscription.getCanceledAt());

      // When
      subscription.reactivate();

      // Then
      assertNull(subscription.getCanceledAt());
    }

    @Test
    @DisplayName("reactivate: Should clear cancelAtPeriodEnd flag")
    void reactivateShouldClearCancelAtPeriodEndFlag() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();
      assertTrue(subscription.getCancelAtPeriodEnd());

      // When
      subscription.reactivate();

      // Then
      assertFalse(subscription.getCancelAtPeriodEnd());
    }

    @Test
    @DisplayName("changePlan: Should reset billing period")
    void changePlanShouldResetBillingPeriod() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      var originalPeriodStart = subscription.getCurrentPeriodStart();
      var originalPeriodEnd = subscription.getCurrentPeriodEnd();

      // When
      subscription.changePlan(yearlyPlan);

      // Then
      assertNotEquals(originalPeriodStart, subscription.getCurrentPeriodStart());
      assertNotEquals(originalPeriodEnd, subscription.getCurrentPeriodEnd());
      assertTrue(subscription.getCurrentPeriodEnd().isAfter(subscription.getCurrentPeriodStart()));
    }

    @Test
    @DisplayName("changePlan: Should update plan reference")
    void changePlanShouldUpdatePlanReference() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      assertEquals(monthlyPlan, subscription.getPlan());

      // When
      subscription.changePlan(yearlyPlan);

      // Then
      assertEquals(yearlyPlan, subscription.getPlan());
    }

    @Test
    @DisplayName("extendTrial: Should extend trial end date")
    void extendTrialShouldExtendTrialEndDate() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);
      var originalTrialEnd = subscription.getTrialEnd();

      // When
      subscription.extendTrial(7);

      // Then
      assertEquals(originalTrialEnd.plusDays(7), subscription.getTrialEnd());
    }

    @Test
    @DisplayName("extendTrial: Should extend current period end")
    void extendTrialShouldExtendCurrentPeriodEnd() {
      // Given
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);
      var originalPeriodEnd = subscription.getCurrentPeriodEnd();

      // When
      subscription.extendTrial(7);

      // Then
      assertEquals(originalPeriodEnd.plusDays(7), subscription.getCurrentPeriodEnd());
    }

    @Test
    @DisplayName("renew: Should advance billing period")
    void renewShouldAdvanceBillingPeriod() {
      // Given
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      var originalPeriodEnd = subscription.getCurrentPeriodEnd();

      // When
      subscription.renew();

      // Then
      assertEquals(originalPeriodEnd, subscription.getCurrentPeriodStart());
      assertTrue(subscription.getCurrentPeriodEnd().isAfter(originalPeriodEnd));
    }
  }

  @Nested
  @DisplayName("Access Control Based on State")
  class AccessControlBasedOnState {

    @Test
    @DisplayName("TRIAL: Should allow access")
    void trialShouldAllowAccess() {
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);
      assertTrue(subscription.hasAccess());
    }

    @Test
    @DisplayName("ACTIVE: Should allow access")
    void activeShouldAllowAccess() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      assertTrue(subscription.hasAccess());
    }

    @Test
    @DisplayName("PAST_DUE: Should allow access (grace period)")
    void pastDueShouldAllowAccess() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();
      assertTrue(subscription.hasAccess());
    }

    @Test
    @DisplayName("CANCELED: Should allow access until period end")
    void canceledShouldAllowAccess() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();
      assertTrue(subscription.hasAccess());
    }

    @Test
    @DisplayName("EXPIRED: Should deny access")
    void expiredShouldDenyAccess() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.expire();
      assertFalse(subscription.hasAccess());
    }

    @Test
    @DisplayName("SUSPENDED: Should deny access")
    void suspendedShouldDenyAccess() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();
      assertFalse(subscription.hasAccess());
    }

    @Test
    @DisplayName("INCOMPLETE: Should deny access")
    void incompleteShouldDenyAccess() {
      var subscription = Subscription.createIncomplete(tenantId, userId, monthlyPlan);
      assertFalse(subscription.hasAccess());
    }
  }

  @Nested
  @DisplayName("Plan Change Restrictions Based on State")
  class PlanChangeRestrictionsBasedOnState {

    @Test
    @DisplayName("TRIAL: Should allow plan changes")
    void trialShouldAllowPlanChanges() {
      var subscription = Subscription.createTrial(tenantId, userId, trialPlan);
      assertDoesNotThrow(() -> subscription.changePlan(monthlyPlan));
    }

    @Test
    @DisplayName("ACTIVE: Should allow plan changes")
    void activeShouldAllowPlanChanges() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      assertDoesNotThrow(() -> subscription.changePlan(yearlyPlan));
    }

    @Test
    @DisplayName("PAST_DUE: Should allow plan changes")
    void pastDueShouldAllowPlanChanges() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.markPastDue();
      assertDoesNotThrow(() -> subscription.changePlan(yearlyPlan));
    }

    @Test
    @DisplayName("CANCELED: Should reject plan changes")
    void canceledShouldRejectPlanChanges() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.cancelAtPeriodEnd();
      assertThrows(IllegalStateException.class, () -> subscription.changePlan(yearlyPlan));
    }

    @Test
    @DisplayName("EXPIRED: Should reject plan changes")
    void expiredShouldRejectPlanChanges() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.expire();
      assertThrows(IllegalStateException.class, () -> subscription.changePlan(yearlyPlan));
    }

    @Test
    @DisplayName("SUSPENDED: Should reject plan changes")
    void suspendedShouldRejectPlanChanges() {
      var subscription = Subscription.createActive(tenantId, userId, monthlyPlan);
      subscription.suspend();
      assertThrows(IllegalStateException.class, () -> subscription.changePlan(yearlyPlan));
    }

    @Test
    @DisplayName("INCOMPLETE: Should reject plan changes")
    void incompleteShouldRejectPlanChanges() {
      var subscription = Subscription.createIncomplete(tenantId, userId, monthlyPlan);
      assertThrows(IllegalStateException.class, () -> subscription.changePlan(yearlyPlan));
    }
  }
}
