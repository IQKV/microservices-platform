package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionItemTest {

  private SubscriptionItem subscriptionItem;
  private TenantSubscription tenantSubscription;
  private SubscriptionPlan plan;

  @BeforeEach
  void setUp() {
    subscriptionItem = new SubscriptionItem();

    tenantSubscription = new TenantSubscription();
    tenantSubscription.setId(UUID.randomUUID());

    plan = new SubscriptionPlan();
    plan.setId(UUID.randomUUID());
    plan.setName("Test Plan");
  }

  @Test
  void constructor_shouldCreateEmptySubscriptionItem() {
    // When
    SubscriptionItem newItem = new SubscriptionItem();

    // Then
    assertNull(newItem.getId());
    assertNull(newItem.getTenantSubscription());
    assertNull(newItem.getPlan());
    assertEquals(Integer.valueOf(1), newItem.getQuantity()); // Default value in entity
    assertNull(newItem.getUnitAmount());
    assertNull(newItem.getStripeSubscriptionItemId());
    assertNull(newItem.getCreatedAt());
    assertNull(newItem.getUpdatedAt());
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    Integer quantity = 3;
    BigDecimal unitAmount = new BigDecimal("29.99");
    String stripeSubscriptionItemId = "si_123";
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();

    // When
    subscriptionItem.setId(id);
    subscriptionItem.setTenantSubscription(tenantSubscription);
    subscriptionItem.setPlan(plan);
    subscriptionItem.setQuantity(quantity);
    subscriptionItem.setUnitAmount(unitAmount);
    subscriptionItem.setStripeSubscriptionItemId(stripeSubscriptionItemId);
    subscriptionItem.setCreatedAt(createdAt);
    subscriptionItem.setUpdatedAt(updatedAt);

    // Then
    assertEquals(id, subscriptionItem.getId());
    assertEquals(tenantSubscription, subscriptionItem.getTenantSubscription());
    assertEquals(plan, subscriptionItem.getPlan());
    assertEquals(quantity, subscriptionItem.getQuantity());
    assertEquals(unitAmount, subscriptionItem.getUnitAmount());
    assertEquals(stripeSubscriptionItemId, subscriptionItem.getStripeSubscriptionItemId());
    assertEquals(createdAt, subscriptionItem.getCreatedAt());
    assertEquals(updatedAt, subscriptionItem.getUpdatedAt());
  }

  @Test
  void onCreate_shouldGenerateIdAndTimestamps() {
    // Given
    subscriptionItem.setTenantSubscription(tenantSubscription);
    subscriptionItem.setPlan(plan);
    subscriptionItem.setQuantity(1);

    // When
    subscriptionItem.onCreate();

    // Then
    assertNotNull(subscriptionItem.getId());
    assertNotNull(subscriptionItem.getCreatedAt());
    assertNotNull(subscriptionItem.getUpdatedAt());
    assertEquals(subscriptionItem.getCreatedAt(), subscriptionItem.getUpdatedAt());
  }

  @Test
  void onCreate_shouldNotOverrideExistingId() {
    // Given
    UUID existingId = UUID.randomUUID();
    subscriptionItem.setId(existingId);

    // When
    subscriptionItem.onCreate();

    // Then
    assertEquals(existingId, subscriptionItem.getId());
  }

  @Test
  void onUpdate_shouldUpdateTimestamp() throws InterruptedException {
    // Given
    subscriptionItem.onCreate();
    Instant originalUpdatedAt = subscriptionItem.getUpdatedAt();
    Thread.sleep(1); // Ensure time difference

    // When
    subscriptionItem.onUpdate();

    // Then
    assertTrue(subscriptionItem.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void getTotalAmount_shouldCalculateCorrectly() {
    // Given
    subscriptionItem.setQuantity(3);
    subscriptionItem.setUnitAmount(new BigDecimal("29.99"));

    // When
    BigDecimal totalAmount = subscriptionItem.getTotalAmount();

    // Then
    assertEquals(new BigDecimal("89.97"), totalAmount);
  }

  @Test
  void getTotalAmount_shouldReturnZeroWhenUnitAmountIsNull() {
    // Given
    subscriptionItem.setQuantity(3);
    subscriptionItem.setUnitAmount(null);

    // When
    BigDecimal totalAmount = subscriptionItem.getTotalAmount();

    // Then
    assertEquals(BigDecimal.ZERO, totalAmount);
  }

  @Test
  void getTotalAmount_shouldHandleQuantityOfOne() {
    // Given
    subscriptionItem.setQuantity(1);
    subscriptionItem.setUnitAmount(new BigDecimal("99.99"));

    // When
    BigDecimal totalAmount = subscriptionItem.getTotalAmount();

    // Then
    assertEquals(new BigDecimal("99.99"), totalAmount);
  }

  @Test
  void getTotalAmount_shouldHandleZeroQuantity() {
    // Given
    subscriptionItem.setQuantity(0);
    subscriptionItem.setUnitAmount(new BigDecimal("29.99"));

    // When
    BigDecimal totalAmount = subscriptionItem.getTotalAmount();

    // Then
    assertEquals(new BigDecimal("0.00"), totalAmount);
  }

  @Test
  void getTotalAmount_shouldHandleLargeQuantities() {
    // Given
    subscriptionItem.setQuantity(100);
    subscriptionItem.setUnitAmount(new BigDecimal("1.99"));

    // When
    BigDecimal totalAmount = subscriptionItem.getTotalAmount();

    // Then
    assertEquals(new BigDecimal("199.00"), totalAmount);
  }

  @Test
  void getTotalAmount_shouldHandleDecimalPrecision() {
    // Given
    subscriptionItem.setQuantity(3);
    subscriptionItem.setUnitAmount(new BigDecimal("10.333"));

    // When
    BigDecimal totalAmount = subscriptionItem.getTotalAmount();

    // Then
    assertEquals(new BigDecimal("30.999"), totalAmount);
  }

  @Test
  void subscriptionItem_shouldSupportMultiplePlans() {
    // Given
    SubscriptionPlan plan1 = new SubscriptionPlan();
    plan1.setId(UUID.randomUUID());
    plan1.setName("Basic Plan");

    SubscriptionPlan plan2 = new SubscriptionPlan();
    plan2.setId(UUID.randomUUID());
    plan2.setName("Premium Plan");

    // When
    subscriptionItem.setPlan(plan1);
    assertEquals(plan1, subscriptionItem.getPlan());

    subscriptionItem.setPlan(plan2);
    assertEquals(plan2, subscriptionItem.getPlan());
  }

  @Test
  void subscriptionItem_shouldHandleStripeIntegration() {
    // Given
    String stripeItemId = "si_1234567890";

    // When
    subscriptionItem.setStripeSubscriptionItemId(stripeItemId);

    // Then
    assertEquals(stripeItemId, subscriptionItem.getStripeSubscriptionItemId());
  }
}
