package com.iqscaffold.billingservice.plan;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SubscriptionPlan aggregate root.
 * Tests business logic, invariants, and validation rules.
 */
@DisplayName("SubscriptionPlan Aggregate Tests")
class SubscriptionPlanTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create plan with valid parameters")
    void shouldCreatePlanWithValidParameters() {
      // Given
      Map<String, Object> features = new HashMap<>();
      features.put("feature1", true);
      features.put("feature2", false);
      var quotas = PlanQuotas.proTier();

      // When
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Pro plan with monthly billing",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          features,
          quotas,
          14,
          true
      );

      // Then
      assertNotNull(plan);
      assertEquals("PRO_MONTHLY", plan.getPlanCode());
      assertEquals("Pro Monthly", plan.getName());
      assertEquals("Pro plan with monthly billing", plan.getDescription());
      assertEquals(PlanTier.PRO, plan.getTier());
      assertEquals(BillingCycle.MONTHLY, plan.getBillingCycle());
      assertEquals(new BigDecimal("29.99"), plan.getBasePrice());
      assertEquals("USD", plan.getCurrency());
      assertEquals(14, plan.getTrialDays());
      assertTrue(plan.getActive());
      assertTrue(plan.getPublicPlan());
      assertEquals(quotas, plan.getQuotas());
    }

    @Test
    @DisplayName("Should create plan with null features and quotas")
    void shouldCreatePlanWithNullFeaturesAndQuotas() {
      // When
      var plan = SubscriptionPlan.create(
          "FREE",
          "Free Plan",
          "Free tier",
          PlanTier.FREE,
          BillingCycle.MONTHLY,
          BigDecimal.ZERO,
          "USD",
          null,
          null,
          0,
          true
      );

      // Then
      assertNotNull(plan);
      assertNotNull(plan.getFeatures());
      assertTrue(plan.getFeatures().isEmpty());
      assertNotNull(plan.getQuotas());
    }

    @Test
    @DisplayName("Should throw exception when plan code is null")
    void shouldThrowExceptionWhenPlanCodeIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              null,
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when plan code is blank")
    void shouldThrowExceptionWhenPlanCodeIsBlank() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "   ",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when name is null")
    void shouldThrowExceptionWhenNameIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              null,
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when tier is null")
    void shouldThrowExceptionWhenTierIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              null,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when billing cycle is null")
    void shouldThrowExceptionWhenBillingCycleIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              null,
              new BigDecimal("29.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when base price is null")
    void shouldThrowExceptionWhenBasePriceIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              null,
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when base price is negative")
    void shouldThrowExceptionWhenBasePriceIsNegative() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("-10.00"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowExceptionWhenCurrencyIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              null,
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when currency is not 3 letters")
    void shouldThrowExceptionWhenCurrencyIsNot3Letters() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              "US",
              new HashMap<>(),
              PlanQuotas.proTier(),
              14,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when trial days is null")
    void shouldThrowExceptionWhenTrialDaysIsNull() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              null,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when trial days is negative")
    void shouldThrowExceptionWhenTrialDaysIsNegative() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "PRO_MONTHLY",
              "Pro Monthly",
              "Description",
              PlanTier.PRO,
              BillingCycle.MONTHLY,
              new BigDecimal("29.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.proTier(),
              -1,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when FREE tier has non-zero price")
    void shouldThrowExceptionWhenFreeTierHasNonZeroPrice() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "FREE",
              "Free Plan",
              "Description",
              PlanTier.FREE,
              BillingCycle.MONTHLY,
              new BigDecimal("9.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.freeTier(),
              0,
              true
          )
      );
    }

    @Test
    @DisplayName("Should throw exception when LIFETIME plan has trial")
    void shouldThrowExceptionWhenLifetimePlanHasTrial() {
      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> SubscriptionPlan.create(
              "LIFETIME",
              "Lifetime Plan",
              "Description",
              PlanTier.ENTERPRISE,
              BillingCycle.LIFETIME,
              new BigDecimal("999.99"),
              "USD",
              new HashMap<>(),
              PlanQuotas.unlimited(),
              14,
              true
          )
      );
    }
  }

  @Nested
  @DisplayName("Update Methods Tests")
  class UpdateMethodsTests {

    @Test
    @DisplayName("Should update plan details")
    void shouldUpdatePlanDetails() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Old description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      // When
      plan.updateDetails("New Name", "New description");

      // Then
      assertEquals("New Name", plan.getName());
      assertEquals("New description", plan.getDescription());
    }

    @Test
    @DisplayName("Should update plan features")
    void shouldUpdatePlanFeatures() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      Map<String, Object> newFeatures = new HashMap<>();
      newFeatures.put("newFeature", true);

      // When
      plan.updateFeatures(newFeatures);

      // Then
      assertEquals(newFeatures, plan.getFeatures());
    }

    @Test
    @DisplayName("Should update plan quotas")
    void shouldUpdatePlanQuotas() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      var newQuotas = PlanQuotas.unlimited();

      // When
      plan.updateQuotas(newQuotas);

      // Then
      assertEquals(newQuotas, plan.getQuotas());
    }

    @Test
    @DisplayName("Should update plan pricing")
    void shouldUpdatePlanPricing() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      // When
      plan.updatePricing(new BigDecimal("39.99"), "EUR");

      // Then
      assertEquals(new BigDecimal("39.99"), plan.getBasePrice());
      assertEquals("EUR", plan.getCurrency());
    }

    @Test
    @DisplayName("Should throw exception when updating FREE tier with non-zero price")
    void shouldThrowExceptionWhenUpdatingFreeTierWithNonZeroPrice() {
      // Given
      var plan = SubscriptionPlan.create(
          "FREE",
          "Free Plan",
          "Description",
          PlanTier.FREE,
          BillingCycle.MONTHLY,
          BigDecimal.ZERO,
          "USD",
          new HashMap<>(),
          PlanQuotas.freeTier(),
          0,
          true
      );

      // When & Then
      assertThrows(
          IllegalArgumentException.class,
          () -> plan.updatePricing(new BigDecimal("9.99"), "USD")
      );
    }
  }

  @Nested
  @DisplayName("Activation Tests")
  class ActivationTests {

    @Test
    @DisplayName("Should activate plan")
    void shouldActivatePlan() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );
      plan.deactivate();

      // When
      plan.activate();

      // Then
      assertTrue(plan.getActive());
    }

    @Test
    @DisplayName("Should deactivate plan")
    void shouldDeactivatePlan() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      // When
      plan.deactivate();

      // Then
      assertFalse(plan.getActive());
    }
  }

  @Nested
  @DisplayName("Visibility Tests")
  class VisibilityTests {

    @Test
    @DisplayName("Should make plan public")
    void shouldMakePlanPublic() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          false
      );

      // When
      plan.makePublic();

      // Then
      assertTrue(plan.getPublicPlan());
    }

    @Test
    @DisplayName("Should make plan private")
    void shouldMakePlanPrivate() {
      // Given
      var plan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      // When
      plan.makePrivate();

      // Then
      assertFalse(plan.getPublicPlan());
    }
  }

  @Nested
  @DisplayName("Query Methods Tests")
  class QueryMethodsTests {

    @Test
    @DisplayName("Should check if plan has trial")
    void shouldCheckIfPlanHasTrial() {
      // Given
      var planWithTrial = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      var planWithoutTrial = SubscriptionPlan.create(
          "PRO_MONTHLY_NO_TRIAL",
          "Pro Monthly",
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

      // Then
      assertTrue(planWithTrial.hasTrial());
      assertFalse(planWithoutTrial.hasTrial());
    }

    @Test
    @DisplayName("Should check if plan is free")
    void shouldCheckIfPlanIsFree() {
      // Given
      var freePlan = SubscriptionPlan.create(
          "FREE",
          "Free Plan",
          "Description",
          PlanTier.FREE,
          BillingCycle.MONTHLY,
          BigDecimal.ZERO,
          "USD",
          new HashMap<>(),
          PlanQuotas.freeTier(),
          0,
          true
      );

      var paidPlan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      // Then
      assertTrue(freePlan.isFree());
      assertFalse(paidPlan.isFree());
    }

    @Test
    @DisplayName("Should check if plan requires payment")
    void shouldCheckIfPlanRequiresPayment() {
      // Given
      var freePlan = SubscriptionPlan.create(
          "FREE",
          "Free Plan",
          "Description",
          PlanTier.FREE,
          BillingCycle.MONTHLY,
          BigDecimal.ZERO,
          "USD",
          new HashMap<>(),
          PlanQuotas.freeTier(),
          0,
          true
      );

      var paidPlan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      // Then
      assertFalse(freePlan.requiresPayment());
      assertTrue(paidPlan.requiresPayment());
    }

    @Test
    @DisplayName("Should check if plan is lifetime")
    void shouldCheckIfPlanIsLifetime() {
      // Given
      var lifetimePlan = SubscriptionPlan.create(
          "LIFETIME",
          "Lifetime Plan",
          "Description",
          PlanTier.ENTERPRISE,
          BillingCycle.LIFETIME,
          new BigDecimal("999.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.unlimited(),
          0,
          true
      );

      var monthlyPlan = SubscriptionPlan.create(
          "PRO_MONTHLY",
          "Pro Monthly",
          "Description",
          PlanTier.PRO,
          BillingCycle.MONTHLY,
          new BigDecimal("29.99"),
          "USD",
          new HashMap<>(),
          PlanQuotas.proTier(),
          14,
          true
      );

      // Then
      assertTrue(lifetimePlan.isLifetime());
      assertFalse(monthlyPlan.isLifetime());
    }
  }
}
