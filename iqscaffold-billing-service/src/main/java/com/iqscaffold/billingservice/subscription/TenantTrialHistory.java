package com.iqscaffold.billingservice.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity tracking trial period usage history for tenants.
 *
 * <p>This entity enforces the business rule that each tenant receives only one
 * trial period per lifetime. It tracks whether a tenant has ever used a trial
 * period, regardless of whether the trial was completed or canceled.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Each tenant can have exactly one trial period</li>
 *   <li>Once a trial is used, the tenant cannot start another trial</li>
 *   <li>Trial history is immutable once marked as used</li>
 * </ul>
 *
 * <p>This entity is stored in tenant-scoped schemas for data isolation.
 */
@Entity
@Table(name = "tenant_trial_history")
public class TenantTrialHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID tenantId;

  @Column(nullable = false)
  private Boolean hasUsedTrial;

  @Column
  private LocalDateTime firstTrialStartedAt;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Default constructor for JPA.
   */
  protected TenantTrialHistory() {
    // JPA requires a no-arg constructor
  }

  /**
   * Creates a new trial history for a tenant.
   *
   * @param tenantId tenant identifier
   */
  private TenantTrialHistory(UUID tenantId) {
    this.tenantId = tenantId;
    this.hasUsedTrial = false;
  }

  /**
   * Factory method to create a new trial history for a tenant.
   *
   * @param tenantId tenant identifier
   * @return new TenantTrialHistory with no trial used
   * @throws IllegalArgumentException if tenantId is null
   */
  public static TenantTrialHistory create(UUID tenantId) {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }

    return new TenantTrialHistory(tenantId);
  }

  /**
   * Marks the trial as used for this tenant.
   *
   * <p>This method is idempotent - calling it multiple times has the same
   * effect as calling it once.
   */
  public void markTrialUsed() {
    if (!this.hasUsedTrial) {
      this.hasUsedTrial = true;
      this.firstTrialStartedAt = LocalDateTime.now();
    }
  }

  /**
   * Checks if the tenant has used their trial period.
   *
   * @return true if trial has been used
   */
  public boolean hasUsedTrial() {
    return hasUsedTrial;
  }

  @PrePersist
  protected void onCreate() {
    var now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Getters

  public Long getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public Boolean getHasUsedTrial() {
    return hasUsedTrial;
  }

  public LocalDateTime getFirstTrialStartedAt() {
    return firstTrialStartedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TenantTrialHistory that)) {
      return false;
    }
    return Objects.equals(id, that.id) && Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tenantId);
  }

  @Override
  public String toString() {
    return "TenantTrialHistory{" +
           "id=" + id +
           ", tenantId=" + tenantId +
           ", hasUsedTrial=" + hasUsedTrial +
           ", firstTrialStartedAt=" + firstTrialStartedAt +
           '}';
  }
}
