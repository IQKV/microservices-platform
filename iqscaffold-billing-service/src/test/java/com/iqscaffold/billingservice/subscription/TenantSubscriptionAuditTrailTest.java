package com.iqscaffold.billingservice.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantSubscriptionAuditTrailTest {

  private TenantSubscriptionAuditTrail auditTrail;
  private TenantSubscription tenantSubscription;

  @BeforeEach
  void setUp() {
    auditTrail = new TenantSubscriptionAuditTrail();
    tenantSubscription = new TenantSubscription();
    tenantSubscription.setId(UUID.randomUUID());
  }

  @Test
  void constructor_shouldCreateEmptyAuditTrail() {
    // When
    TenantSubscriptionAuditTrail newAuditTrail = new TenantSubscriptionAuditTrail();

    // Then
    assertNull(newAuditTrail.getId());
    assertNull(newAuditTrail.getTenantSubscription());
    assertNull(newAuditTrail.getOldStatus());
    assertNull(newAuditTrail.getNewStatus());
    assertNull(newAuditTrail.getChangedBy());
    assertNull(newAuditTrail.getReason());
    assertNull(newAuditTrail.getMetadata());
    assertNull(newAuditTrail.getCreatedAt());
  }

  @Test
  void settersAndGetters_shouldWorkCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    SubscriptionStatus oldStatus = SubscriptionStatus.ACTIVE;
    SubscriptionStatus newStatus = SubscriptionStatus.PAUSED;
    String changedBy = "user@example.com";
    String reason = "User requested pause";
    String metadata = "{\"reason_code\":\"user_request\"}";
    Instant createdAt = Instant.now();

    // When
    auditTrail.setId(id);
    auditTrail.setTenantSubscription(tenantSubscription);
    auditTrail.setOldStatus(oldStatus);
    auditTrail.setNewStatus(newStatus);
    auditTrail.setChangedBy(changedBy);
    auditTrail.setReason(reason);
    auditTrail.setMetadata(metadata);
    auditTrail.setCreatedAt(createdAt);

    // Then
    assertEquals(id, auditTrail.getId());
    assertEquals(tenantSubscription, auditTrail.getTenantSubscription());
    assertEquals(oldStatus, auditTrail.getOldStatus());
    assertEquals(newStatus, auditTrail.getNewStatus());
    assertEquals(changedBy, auditTrail.getChangedBy());
    assertEquals(reason, auditTrail.getReason());
    assertEquals(metadata, auditTrail.getMetadata());
    assertEquals(createdAt, auditTrail.getCreatedAt());
  }

  @Test
  void onCreate_shouldGenerateIdAndTimestamp() {
    // Given
    auditTrail.setTenantSubscription(tenantSubscription);
    auditTrail.setNewStatus(SubscriptionStatus.ACTIVE);

    // When
    auditTrail.onCreate();

    // Then
    assertNotNull(auditTrail.getId());
    assertNotNull(auditTrail.getCreatedAt());
  }

  @Test
  void onCreate_shouldNotOverrideExistingId() {
    // Given
    UUID existingId = UUID.randomUUID();
    auditTrail.setId(existingId);

    // When
    auditTrail.onCreate();

    // Then
    assertEquals(existingId, auditTrail.getId());
  }

  @Test
  void auditTrail_shouldTrackStatusTransition() {
    // Given
    SubscriptionStatus oldStatus = SubscriptionStatus.ACTIVE;
    SubscriptionStatus newStatus = SubscriptionStatus.CANCELED;
    String reason = "Subscription canceled by user";
    String changedBy = "admin@example.com";

    // When
    auditTrail.setTenantSubscription(tenantSubscription);
    auditTrail.setOldStatus(oldStatus);
    auditTrail.setNewStatus(newStatus);
    auditTrail.setReason(reason);
    auditTrail.setChangedBy(changedBy);
    auditTrail.onCreate();

    // Then
    assertEquals(tenantSubscription, auditTrail.getTenantSubscription());
    assertEquals(oldStatus, auditTrail.getOldStatus());
    assertEquals(newStatus, auditTrail.getNewStatus());
    assertEquals(reason, auditTrail.getReason());
    assertEquals(changedBy, auditTrail.getChangedBy());
    assertNotNull(auditTrail.getId());
    assertNotNull(auditTrail.getCreatedAt());
  }

  @Test
  void auditTrail_shouldAllowNullOldStatusForNewSubscription() {
    // Given
    SubscriptionStatus newStatus = SubscriptionStatus.ACTIVE;
    String reason = "New subscription created";

    // When
    auditTrail.setTenantSubscription(tenantSubscription);
    auditTrail.setOldStatus(null); // No previous status for new subscription
    auditTrail.setNewStatus(newStatus);
    auditTrail.setReason(reason);
    auditTrail.onCreate();

    // Then
    assertEquals(tenantSubscription, auditTrail.getTenantSubscription());
    assertNull(auditTrail.getOldStatus());
    assertEquals(newStatus, auditTrail.getNewStatus());
    assertEquals(reason, auditTrail.getReason());
    assertNotNull(auditTrail.getId());
    assertNotNull(auditTrail.getCreatedAt());
  }

  @Test
  void auditTrail_shouldStoreMetadata() {
    // Given
    String metadata = "{\"payment_method\":\"card\",\"proration\":true}";

    // When
    auditTrail.setTenantSubscription(tenantSubscription);
    auditTrail.setNewStatus(SubscriptionStatus.ACTIVE);
    auditTrail.setMetadata(metadata);
    auditTrail.onCreate();

    // Then
    assertEquals(metadata, auditTrail.getMetadata());
  }

  @Test
  void auditTrail_shouldHandleAllStatusTransitions() {
    // Test various status transitions
    SubscriptionStatus[] statuses = SubscriptionStatus.values();

    for (int i = 0; i < statuses.length - 1; i++) {
      // Given
      TenantSubscriptionAuditTrail trail = new TenantSubscriptionAuditTrail();
      SubscriptionStatus oldStatus = statuses[i];
      SubscriptionStatus newStatus = statuses[i + 1];

      // When
      trail.setTenantSubscription(tenantSubscription);
      trail.setOldStatus(oldStatus);
      trail.setNewStatus(newStatus);
      trail.setReason("Status transition test");
      trail.onCreate();

      // Then
      assertEquals(oldStatus, trail.getOldStatus());
      assertEquals(newStatus, trail.getNewStatus());
      assertNotNull(trail.getId());
      assertNotNull(trail.getCreatedAt());
    }
  }
}
