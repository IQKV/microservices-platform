package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionPlanTest {

  private SubscriptionPlan plan;

  @BeforeEach
  void setUp() {
    plan = new SubscriptionPlan();
  }

  @Test
  void constructor_shouldCreateEmptyPlan() {
    // When
    SubscriptionPlan newPlan = new SubscriptionPlan();

    // Then
    assertNull(newPlan.getId());
    assertNull(newPlan.getName());
    assertNull(newPlan.getDescription());
    assertNull(newPlan.getAmount());
    assertNull(newPlan.getCurrency());
    assertNull(newPlan.getInterval());
    assertEquals(Integer.valueOf(1), newPlan.getIntervalCount()); // Default value in entity
    assertEquals(Integer.valueOf(0), newPlan.getTrialPeriodDays()); // Default value in entity
    assertNull(newPlan.getStripeProductId());
    assertNull(newPlan.getStripePriceId());
    assertEquals(Boolean.TRUE, newPlan.getIsActive()); // Default value in entity
    assertNull(newPlan.getMaxUsers());
    assertNull(newPlan.getMaxStorageGb());
    assertNull(newPlan.getMaxApiCalls());
    assertNull(newPlan.getMetadata());
    assertNull(newPlan.getCreatedAt());
    assertNull(newPlan.getUpdatedAt());
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    String name = "Premium Plan";
    String description = "Premium subscription with advanced features";
    BigDecimal amount = new BigDecimal("29.99");
    String currency = "USD";
    SubscriptionInterval interval = SubscriptionInterval.MONTH;
    Integer intervalCount = 1;
    Integer trialPeriodDays = 14;
    String stripeProductId = "prod_123";
    String stripePriceId = "price_123";
    Boolean isActive = true;
    Integer maxUsers = 10;
    Integer maxStorageGb = 100;
    Long maxApiCalls = 10000L;
    String metadata = "{\"tier\":\"premium\"}";
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();

    // When
    plan.setId(id);
    plan.setName(name);
    plan.setDescription(description);
    plan.setAmount(amount);
    plan.setCurrency(currency);
    plan.setInterval(interval);
    plan.setIntervalCount(intervalCount);
    plan.setTrialPeriodDays(trialPeriodDays);
    plan.setStripeProductId(stripeProductId);
    plan.setStripePriceId(stripePriceId);
    plan.setIsActive(isActive);
    plan.setMaxUsers(maxUsers);
    plan.setMaxStorageGb(maxStorageGb);
    plan.setMaxApiCalls(maxApiCalls);
    plan.setMetadata(metadata);
    plan.setCreatedAt(createdAt);
    plan.setUpdatedAt(updatedAt);

    // Then
    assertEquals(id, plan.getId());
    assertEquals(name, plan.getName());
    assertEquals(description, plan.getDescription());
    assertEquals(amount, plan.getAmount());
    assertEquals(currency, plan.getCurrency());
    assertEquals(interval, plan.getInterval());
    assertEquals(intervalCount, plan.getIntervalCount());
    assertEquals(trialPeriodDays, plan.getTrialPeriodDays());
    assertEquals(stripeProductId, plan.getStripeProductId());
    assertEquals(stripePriceId, plan.getStripePriceId());
    assertEquals(isActive, plan.getIsActive());
    assertEquals(maxUsers, plan.getMaxUsers());
    assertEquals(maxStorageGb, plan.getMaxStorageGb());
    assertEquals(maxApiCalls, plan.getMaxApiCalls());
    assertEquals(metadata, plan.getMetadata());
    assertEquals(createdAt, plan.getCreatedAt());
    assertEquals(updatedAt, plan.getUpdatedAt());
  }

  @Test
  void onCreate_shouldGenerateIdAndTimestamps() {
    // Given
    plan.setName("Test Plan");
    plan.setAmount(new BigDecimal("19.99"));
    plan.setCurrency("USD");
    plan.setInterval(SubscriptionInterval.MONTH);

    // When
    plan.onCreate();

    // Then
    assertNotNull(plan.getId());
    assertNotNull(plan.getCreatedAt());
    assertNotNull(plan.getUpdatedAt());
    assertEquals(plan.getCreatedAt(), plan.getUpdatedAt());
  }

  @Test
  void onCreate_shouldNotOverrideExistingId() {
    // Given
    UUID existingId = UUID.randomUUID();
    plan.setId(existingId);

    // When
    plan.onCreate();

    // Then
    assertEquals(existingId, plan.getId());
  }

  @Test
  void onUpdate_shouldUpdateTimestamp() throws InterruptedException {
    // Given
    plan.onCreate();
    Instant originalUpdatedAt = plan.getUpdatedAt();
    Thread.sleep(1); // Ensure time difference

    // When
    plan.onUpdate();

    // Then
    assertTrue(plan.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void isActive_shouldReturnTrueWhenIsActiveIsTrue() {
    // Given
    plan.setIsActive(true);

    // When & Then
    assertTrue(plan.isActive());
  }

  @Test
  void isActive_shouldReturnFalseWhenIsActiveIsFalse() {
    // Given
    plan.setIsActive(false);

    // When & Then
    assertFalse(plan.isActive());
  }

  @Test
  void isActive_shouldReturnFalseWhenIsActiveIsNull() {
    // Given
    plan.setIsActive(null);

    // When & Then
    assertFalse(plan.isActive());
  }

  @Test
  void hasTrial_shouldReturnTrueWhenTrialPeriodDaysIsPositive() {
    // Given
    plan.setTrialPeriodDays(14);

    // When & Then
    assertTrue(plan.hasTrial());
  }

  @Test
  void hasTrial_shouldReturnFalseWhenTrialPeriodDaysIsZero() {
    // Given
    plan.setTrialPeriodDays(0);

    // When & Then
    assertFalse(plan.hasTrial());
  }

  @Test
  void hasTrial_shouldReturnFalseWhenTrialPeriodDaysIsNull() {
    // Given
    plan.setTrialPeriodDays(null);

    // When & Then
    assertFalse(plan.hasTrial());
  }

  @Test
  void hasTrial_shouldReturnFalseWhenTrialPeriodDaysIsNegative() {
    // Given
    plan.setTrialPeriodDays(-1);

    // When & Then
    assertFalse(plan.hasTrial());
  }

  @Test
  void plan_shouldSupportAllIntervals() {
    // Test all subscription intervals
    SubscriptionInterval[] intervals = SubscriptionInterval.values();

    for (final SubscriptionInterval interval : intervals) {
      // Given
      SubscriptionPlan testPlan = new SubscriptionPlan();

      // When
      testPlan.setInterval(interval);

      // Then
      assertEquals(interval, testPlan.getInterval());
    }
  }

  @Test
  void plan_shouldHandleComplexMetadata() {
    // Given
    String complexMetadata = "{\"billing_cycle\":\"monthly\",\"auto_renew\":true,\"discount_eligible\":false}";

    // When
    plan.setMetadata(complexMetadata);

    // Then
    assertEquals(complexMetadata, plan.getMetadata());
  }

  @Test
  void plan_shouldSupportDifferentCurrencies() {
    // Given
    String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD"};

    for (final String currency : currencies) {
      // When
      plan.setCurrency(currency);

      // Then
      assertEquals(currency, plan.getCurrency());
    }
  }

  @Test
  void plan_shouldSupportVariousAmounts() {
    // Given
    BigDecimal[] amounts = {
        new BigDecimal("0.99"),
        new BigDecimal("9.99"),
        new BigDecimal("99.99"),
        new BigDecimal("999.99"),
        new BigDecimal("9999.99")
    };

    for (final BigDecimal amount : amounts) {
      // When
      plan.setAmount(amount);

      // Then
      assertEquals(amount, plan.getAmount());
    }
  }
}
