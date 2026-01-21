package com.iqscaffold.billingservice.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit trail for tenant subscription state changes.
 * <p>
 * Tracks all status transitions for compliance and debugging.
 * Stored in tenant-specific schema for isolation.
 */
@Entity
@Table(name = "tenant_subscription_audit_trail")
public class TenantSubscriptionAuditTrail {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_subscription_id", nullable = false)
  private TenantSubscription tenantSubscription;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_status", length = 50)
  private SubscriptionStatus oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 50)
  private SubscriptionStatus newStatus;

  @Column(name = "changed_by")
  private String changedBy;

  @Column(columnDefinition = "TEXT")
  private String reason;

  @Column(columnDefinition = "text")
  private String metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public TenantSubscriptionAuditTrail() {
  }

  // Getters and setters

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public TenantSubscription getTenantSubscription() {
    return tenantSubscription;
  }

  public void setTenantSubscription(TenantSubscription tenantSubscription) {
    this.tenantSubscription = tenantSubscription;
  }

  public SubscriptionStatus getOldStatus() {
    return oldStatus;
  }

  public void setOldStatus(SubscriptionStatus oldStatus) {
    this.oldStatus = oldStatus;
  }

  public SubscriptionStatus getNewStatus() {
    return newStatus;
  }

  public void setNewStatus(SubscriptionStatus newStatus) {
    this.newStatus = newStatus;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = Instant.now();
  }
}
