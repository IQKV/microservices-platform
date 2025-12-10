package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodType;
import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for SubscriptionFactory.
 * Tests subscription creation logic and validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionFactory Tests")
class SubscriptionFactoryTest {

  @Mock
  private TrialEligibilitySpecification trialEligibilitySpecification;

  private SubscriptionFactory subscriptionFactory;
  private UUID tenantId;
  private UUID userId;
  private SubscriptionPlan trialPlan;
  private SubscriptionPlan paidPlan;
  private TenantTrialHistory eligibleHistory;
  private TenantTrialHistory ineligibleHistory;

  @BeforeEach
  void setUp() {
    subscriptionFactory = new SubscriptionFactory(trialEligibilitySpecification);
    tenantId = UUID.randomUUID();
    userId = UUID.randomUUID();

    trialPlan = SubscriptionPlan.create(
        "PRO_MONTHLY",
        "Pro Monthly",
        "Pro plan with trial",
        PlanTier.PRO,
        BillingCycle.MONTHLY,
        new BigDecimal("29.99"),
        "USD",
        new HashMap<>(),
        PlanQuotas.proTier(),
        14,
        true
    );

    paidPlan = SubscriptionPlan.create(
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

    eligibleHistory = TenantTrialHistory.create(tenantId);

    ineligibleHistory = TenantTrialHistory.create(tenantId);
    ineligibleHistory.markTrialUsed();
  }

  @Nested
  @DisplayName("Trial Subscription Creation Tests")
  class TrialSubscriptionCreationTests {

    @Test
    @DisplayName("Should create trial subscription when eligible")
    void shouldCreateTrialSubscriptionWhenEligible() {
      // Given
      when(trialEligibilitySpecification.isSatisfiedBy(eligibleHistory)).thenReturn(true);

      // When
      var subscription = subscriptionFactory.createTrialSubscription(
          tenantId,
          userId,
          trialPlan,
          eligibleHistory
      );

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.TRIAL, subscription.getStatus());
      assertEquals(tenantId, subscription.getTenantId());
      assertEquals(userId, subscription.getUserId());
      assertEquals(trialPlan, subscription.getPlan());
      assertNotNull(subscription.getTrialStart());
      assertNotNull(subscription.getTrialEnd());
      verify(trialEligibilitySpecification).isSatisfiedBy(eligibleHistory);
    }

    @Test
    @DisplayName("Should throw exception when tenant is not eligible for trial")
    void shouldThrowExceptionWhenTenantIsNotEligibleForTrial() {
      // Given
      when(trialEligibilitySpecification.isSatisfiedBy(ineligibleHistory)).thenReturn(false);

      // When & Then
      assertThrows(
          SubscriptionException.TrialNotEligibleException.class,
          () -> subscriptionFactory.createTrialSubscription(
              tenantId,
              userId,
              trialPlan,
              ineligibleHistory
          )
      );
      verify(trialEligibilitySpecification).isSatisfiedBy(ineligibleHistory);
    }

    @Test
    @DisplayName("Should throw exception when plan does not offer trial")
    void shouldThrowExceptionWhenPlanDoesNotOfferTrial() {
      // Given
      when(trialEligibilitySpecification.isSatisfiedBy(eligibleHistory)).thenReturn(true);

      // When & Then
      assertThrows(
          SubscriptionException.PlanDoesNotOfferTrialException.class,
          () -> subscriptionFactory.createTrialSubscription(
              tenantId,
              userId,
              paidPlan,
              eligibleHistory
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when tenant ID is null")
    void shouldThrowExceptionWhenTenantIdIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscriptionFactory.createTrialSubscription(
              null,
              userId,
              trialPlan,
              eligibleHistory
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when user ID is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscriptionFactory.createTrialSubscription(
              tenantId,
              null,
              trialPlan,
              eligibleHistory
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when plan is null")
    void shouldThrowExceptionWhenPlanIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscriptionFactory.createTrialSubscription(
              tenantId,
              userId,
              null,
              eligibleHistory
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when trial history is null")
    void shouldThrowExceptionWhenTrialHistoryIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscriptionFactory.createTrialSubscription(
              tenantId,
              userId,
              trialPlan,
              null
          )
      );
    }
  }

  @Nested
  @DisplayName("Paid Subscription Creation Tests")
  class PaidSubscriptionCreationTests {

    @Test
    @DisplayName("Should create paid subscription with payment method")
    void shouldCreatePaidSubscriptionWithPaymentMethod() {
      // Given
      var paymentMethod = createValidPaymentMethod();

      // When
      var subscription = subscriptionFactory.createPaidSubscription(
          tenantId,
          userId,
          paidPlan,
          paymentMethod
      );

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
      assertEquals(tenantId, subscription.getTenantId());
      assertEquals(userId, subscription.getUserId());
      assertEquals(paidPlan, subscription.getPlan());
      assertNull(subscription.getTrialStart());
      assertNull(subscription.getTrialEnd());
    }

    @Test
    @DisplayName("Should create paid subscription without payment method for FREE plan")
    void shouldCreatePaidSubscriptionWithoutPaymentMethodForFreePlan() {
      // Given
      var freePlan = SubscriptionPlan.create(
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

      // When
      var subscription = subscriptionFactory.createPaidSubscription(
          tenantId,
          userId,
          freePlan,
          null
      );

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when payment method belongs to different tenant")
    void shouldThrowExceptionWhenPaymentMethodBelongsToDifferentTenant() {
      // Given
      var paymentMethod = createValidPaymentMethod();
      var differentTenantId = UUID.randomUUID();

      // When & Then
      assertThrows(
          SubscriptionException.PaymentMethodMismatchException.class,
          () -> subscriptionFactory.createPaidSubscription(
              differentTenantId,
              userId,
              paidPlan,
              paymentMethod
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when payment method is expired")
    void shouldThrowExceptionWhenPaymentMethodIsExpired() {
      // Given
      var expiredPaymentMethod = createExpiredPaymentMethod();

      // When & Then
      assertThrows(
          SubscriptionException.InvalidPaymentMethodException.class,
          () -> subscriptionFactory.createPaidSubscription(
              tenantId,
              userId,
              paidPlan,
              expiredPaymentMethod
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when plan is inactive")
    void shouldThrowExceptionWhenPlanIsInactive() {
      // Given
      var inactivePlan = SubscriptionPlan.create(
          "INACTIVE_PLAN",
          "Inactive Plan",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          0,
          true
      );
      inactivePlan.deactivate();

      var paymentMethod = createValidPaymentMethod();

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscriptionFactory.createPaidSubscription(
              tenantId,
              userId,
              inactivePlan,
              paymentMethod
          )
      );
    }
  }

  @Nested
  @DisplayName("Incomplete Subscription Creation Tests")
  class IncompleteSubscriptionCreationTests {

    @Test
    @DisplayName("Should create incomplete subscription")
    void shouldCreateIncompleteSubscription() {
      // When
      var subscription = subscriptionFactory.createIncompleteSubscription(
          tenantId,
          userId,
          paidPlan
      );

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.INCOMPLETE, subscription.getStatus());
      assertEquals(tenantId, subscription.getTenantId());
      assertEquals(userId, subscription.getUserId());
      assertEquals(paidPlan, subscription.getPlan());
    }

    @Test
    @DisplayName("Should throw exception when tenant ID is null")
    void shouldThrowExceptionWhenTenantIdIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> subscriptionFactory.createIncompleteSubscription(
              null,
              userId,
              paidPlan
          )
      );
    }
  }

  @Nested
  @DisplayName("Trial With Payment Method Tests")
  class TrialWithPaymentMethodTests {

    @Test
    @DisplayName("Should create trial subscription with payment method")
    void shouldCreateTrialSubscriptionWithPaymentMethod() {
      // Given
      when(trialEligibilitySpecification.isSatisfiedBy(eligibleHistory)).thenReturn(true);
      var paymentMethod = createValidPaymentMethod();

      // When
      var subscription = subscriptionFactory.createTrialWithPaymentMethod(
          tenantId,
          userId,
          trialPlan,
          eligibleHistory,
          paymentMethod
      );

      // Then
      assertNotNull(subscription);
      assertEquals(SubscriptionStatus.TRIAL, subscription.getStatus());
      assertEquals(tenantId, subscription.getTenantId());
      assertEquals(userId, subscription.getUserId());
      assertEquals(trialPlan, subscription.getPlan());
      verify(trialEligibilitySpecification).isSatisfiedBy(eligibleHistory);
    }

    @Test
    @DisplayName("Should throw exception when not eligible for trial")
    void shouldThrowExceptionWhenNotEligibleForTrial() {
      // Given
      when(trialEligibilitySpecification.isSatisfiedBy(ineligibleHistory)).thenReturn(false);
      var paymentMethod = createValidPaymentMethod();

      // When & Then
      assertThrows(
          SubscriptionException.TrialNotEligibleException.class,
          () -> subscriptionFactory.createTrialWithPaymentMethod(
              tenantId,
              userId,
              trialPlan,
              ineligibleHistory,
              paymentMethod
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when payment method belongs to different tenant")
    void shouldThrowExceptionWhenPaymentMethodBelongsToDifferentTenant() {
      // Given
      when(trialEligibilitySpecification.isSatisfiedBy(eligibleHistory)).thenReturn(true);
      var paymentMethod = createValidPaymentMethod();
      var differentTenantId = UUID.randomUUID();

      // When & Then
      assertThrows(
          SubscriptionException.PaymentMethodMismatchException.class,
          () -> subscriptionFactory.createTrialWithPaymentMethod(
              differentTenantId,
              userId,
              trialPlan,
              eligibleHistory,
              paymentMethod
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when payment method is expired")
    void shouldThrowExceptionWhenPaymentMethodIsExpired() {
      // Given
      when(trialEligibilitySpecification.isSatisfiedBy(eligibleHistory)).thenReturn(true);
      var expiredPaymentMethod = createExpiredPaymentMethod();

      // When & Then
      assertThrows(
          SubscriptionException.InvalidPaymentMethodException.class,
          () -> subscriptionFactory.createTrialWithPaymentMethod(
              tenantId,
              userId,
              trialPlan,
              eligibleHistory,
              expiredPaymentMethod
          )
      );
    }
  }

  // Helper methods

  private PaymentMethod createValidPaymentMethod() {
    var paymentMethod = new PaymentMethod(
        tenantId,
        userId,
        PaymentMethodType.CARD,
        "pm_test_123"
    );
    paymentMethod.setCardDetails("4242", "Visa", 12, LocalDateTime.now().getYear() + 1);
    paymentMethod.markAsDefault();
    return paymentMethod;
  }

  private PaymentMethod createExpiredPaymentMethod() {
    // Create a payment method and use reflection to set expired dates
    // since setCardDetails validates and rejects expired dates
    var paymentMethod = new PaymentMethod(
        tenantId,
        userId,
        PaymentMethodType.CARD,
        "pm_test_expired"
    );

    // Use reflection to set expired card details
    try {
      var expiryMonthField = PaymentMethod.class.getDeclaredField("expiryMonth");
      expiryMonthField.setAccessible(true);
      expiryMonthField.set(paymentMethod, 1);

      var expiryYearField = PaymentMethod.class.getDeclaredField("expiryYear");
      expiryYearField.setAccessible(true);
      expiryYearField.set(paymentMethod, 2020);

      var last4Field = PaymentMethod.class.getDeclaredField("last4");
      last4Field.setAccessible(true);
      last4Field.set(paymentMethod, "4242");

      var brandField = PaymentMethod.class.getDeclaredField("brand");
      brandField.setAccessible(true);
      brandField.set(paymentMethod, "Visa");
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create expired payment method", e);
    }

    return paymentMethod;
  }
}
