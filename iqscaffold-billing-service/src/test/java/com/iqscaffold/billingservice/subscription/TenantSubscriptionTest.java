package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantSubscriptionTest {

  private TenantSubscription subscription;
  private SubscriptionPlan plan;

  @BeforeEach
  void setUp() {
    subscription = new TenantSubscription();

    plan = new SubscriptionPlan();
    plan.setId(UUID.randomUUID());
    plan.setName("Test Plan");
  }

  @Test
  void constructor_shouldCreateEmptySubscription() {
    // When
    TenantSubscription newSubscription = new TenantSubscription();

    // Then
    assertNull(newSubscription.getId());
    assertNull(newSubscription.getPlan());
    assertNull(newSubscription.getOrganizationId());
    assertNull(newSubscription.getStatus());
    assertNull(newSubscription.getStripeSubscriptionId());
    assertNull(newSubscription.getStripeCustomerId());
    assertNull(newSubscription.getCurrentPeriodStart());
    assertNull(newSubscription.getCurrentPeriodEnd());
    assertNull(newSubscription.getTrialEnd());
    assertNull(newSubscription.getCancelAt());
    assertNull(newSubscription.getCanceledAt());
    assertNull(newSubscription.getCancellationReason());
    assertNull(newSubscription.getMetadata());
    assertNull(newSubscription.getCreatedAt());
    assertNull(newSubscription.getUpdatedAt());
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    Long organizationId = 12345L;
    SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    String stripeSubscriptionId = "sub_123";
    String stripeCustomerId = "cus_123";
    Instant currentPeriodStart = Instant.now();
    Instant currentPeriodEnd = Instant.now().plusSeconds(2592000); // 30 days
    Instant trialEnd = Instant.now().plusSeconds(1209600); // 14 days
    Instant cancelAt = Instant.now().plusSeconds(2592000);
    Instant canceledAt = Instant.now();
    String cancellationReason = "User requested cancellation";
    String metadata = "{\"source\":\"web\"}";
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();

    // When
    subscription.setId(id);
    subscription.setPlan(plan);
    subscription.setOrganizationId(organizationId);
    subscription.setStatus(status);
    subscription.setStripeSubscriptionId(stripeSubscriptionId);
    subscription.setStripeCustomerId(stripeCustomerId);
    subscription.setCurrentPeriodStart(currentPeriodStart);
    subscription.setCurrentPeriodEnd(currentPeriodEnd);
    subscription.setTrialEnd(trialEnd);
    subscription.setCancelAt(cancelAt);
    subscription.setCanceledAt(canceledAt);
    subscription.setCancellationReason(cancellationReason);
    subscription.setMetadata(metadata);
    subscription.setCreatedAt(createdAt);
    subscription.setUpdatedAt(updatedAt);

    // Then
    assertEquals(id, subscription.getId());
    assertEquals(plan, subscription.getPlan());
    assertEquals(organizationId, subscription.getOrganizationId());
    assertEquals(status, subscription.getStatus());
    assertEquals(stripeSubscriptionId, subscription.getStripeSubscriptionId());
    assertEquals(stripeCustomerId, subscription.getStripeCustomerId());
    assertEquals(currentPeriodStart, subscription.getCurrentPeriodStart());
    assertEquals(currentPeriodEnd, subscription.getCurrentPeriodEnd());
    assertEquals(trialEnd, subscription.getTrialEnd());
    assertEquals(cancelAt, subscription.getCancelAt());
    assertEquals(canceledAt, subscription.getCanceledAt());
    assertEquals(cancellationReason, subscription.getCancellationReason());
    assertEquals(metadata, subscription.getMetadata());
    assertEquals(createdAt, subscription.getCreatedAt());
    assertEquals(updatedAt, subscription.getUpdatedAt());
  }

  @Test
  void onCreate_shouldGenerateIdAndTimestamps() {
    // Given
    subscription.setPlan(plan);
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    // When
    subscription.onCreate();

    // Then
    assertNotNull(subscription.getId());
    assertNotNull(subscription.getCreatedAt());
    assertNotNull(subscription.getUpdatedAt());
    assertEquals(subscription.getCreatedAt(), subscription.getUpdatedAt());
  }

  @Test
  void onCreate_shouldNotOverrideExistingId() {
    // Given
    UUID existingId = UUID.randomUUID();
    subscription.setId(existingId);

    // When
    subscription.onCreate();

    // Then
    assertEquals(existingId, subscription.getId());
  }

  @Test
  void onUpdate_shouldUpdateTimestamp() throws InterruptedException {
    // Given
    subscription.onCreate();
    Instant originalUpdatedAt = subscription.getUpdatedAt();
    Thread.sleep(1); // Ensure time difference

    // When
    subscription.onUpdate();

    // Then
    assertTrue(subscription.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void isActive_shouldReturnTrueWhenStatusIsActive() {
    // Given
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertTrue(subscription.isActive());
  }

  @Test
  void isActive_shouldReturnFalseWhenStatusIsNotActive() {
    // Given
    subscription.setStatus(SubscriptionStatus.CANCELED);

    // When & Then
    assertFalse(subscription.isActive());
  }

  @Test
  void isCanceled_shouldReturnTrueWhenStatusIsCanceled() {
    // Given
    subscription.setStatus(SubscriptionStatus.CANCELED);

    // When & Then
    assertTrue(subscription.isCanceled());
  }

  @Test
  void isCanceled_shouldReturnFalseWhenStatusIsNotCanceled() {
    // Given
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertFalse(subscription.isCanceled());
  }

  @Test
  void isInTrial_shouldReturnTrueWhenStatusIsTrialing() {
    // Given
    subscription.setStatus(SubscriptionStatus.TRIALING);

    // When & Then
    assertTrue(subscription.isInTrial());
  }

  @Test
  void isInTrial_shouldReturnFalseWhenStatusIsNotTrialing() {
    // Given
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertFalse(subscription.isInTrial());
  }

  @Test
  void isPastDue_shouldReturnTrueWhenStatusIsPastDue() {
    // Given
    subscription.setStatus(SubscriptionStatus.PAST_DUE);

    // When & Then
    assertTrue(subscription.isPastDue());
  }

  @Test
  void isPastDue_shouldReturnFalseWhenStatusIsNotPastDue() {
    // Given
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertFalse(subscription.isPastDue());
  }

  @Test
  void isPaused_shouldReturnTrueWhenStatusIsPaused() {
    // Given
    subscription.setStatus(SubscriptionStatus.PAUSED);

    // When & Then
    assertTrue(subscription.isPaused());
  }

  @Test
  void isPaused_shouldReturnFalseWhenStatusIsNotPaused() {
    // Given
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertFalse(subscription.isPaused());
  }

  @Test
  void canRenew_shouldReturnTrueWhenStatusIsActive() {
    // Given
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    // When & Then
    assertTrue(subscription.canRenew());
  }

  @Test
  void canRenew_shouldReturnTrueWhenStatusIsTrialing() {
    // Given
    subscription.setStatus(SubscriptionStatus.TRIALING);

    // When & Then
    assertTrue(subscription.canRenew());
  }

  @Test
  void canRenew_shouldReturnFalseWhenStatusIsCanceled() {
    // Given
    subscription.setStatus(SubscriptionStatus.CANCELED);

    // When & Then
    assertFalse(subscription.canRenew());
  }

  @Test
  void canRenew_shouldReturnFalseWhenStatusIsPaused() {
    // Given
    subscription.setStatus(SubscriptionStatus.PAUSED);

    // When & Then
    assertFalse(subscription.canRenew());
  }

  @Test
  void isExpired_shouldReturnTrueWhenCurrentPeriodEndIsInPast() {
    // Given
    subscription.setCurrentPeriodEnd(Instant.now().minusSeconds(86400)); // 1 day ago

    // When & Then
    assertTrue(subscription.isExpired());
  }

  @Test
  void isExpired_shouldReturnFalseWhenCurrentPeriodEndIsInFuture() {
    // Given
    subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(86400)); // 1 day from now

    // When & Then
    assertFalse(subscription.isExpired());
  }

  @Test
  void isExpired_shouldReturnFalseWhenCurrentPeriodEndIsNull() {
    // Given
    subscription.setCurrentPeriodEnd(null);

    // When & Then
    assertFalse(subscription.isExpired());
  }

  @Test
  void subscription_shouldSupportAllStatuses() {
    // Test all subscription statuses
    SubscriptionStatus[] statuses = SubscriptionStatus.values();

    for (final SubscriptionStatus status : statuses) {
      // When
      subscription.setStatus(status);

      // Then
      assertEquals(status, subscription.getStatus());
    }
  }

  @Test
  void subscription_shouldHandleMetadata() {
    // Given
    String complexMetadata = "{\"payment_method\":\"card\",\"trial_used\":true,\"source\":\"api\"}";

    // When
    subscription.setMetadata(complexMetadata);

    // Then
    assertEquals(complexMetadata, subscription.getMetadata());
  }

  @Test
  void subscription_shouldHandleStripeIntegration() {
    // Given
    String stripeSubscriptionId = "sub_1234567890";
    String stripeCustomerId = "cus_0987654321";

    // When
    subscription.setStripeSubscriptionId(stripeSubscriptionId);
    subscription.setStripeCustomerId(stripeCustomerId);

    // Then
    assertEquals(stripeSubscriptionId, subscription.getStripeSubscriptionId());
    assertEquals(stripeCustomerId, subscription.getStripeCustomerId());
  }
}
