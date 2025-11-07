package org.gripday.authservice.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.gripday.authservice.domain.service.TenantContext;

/**
 * Base class for entities that are tenant-aware and require automatic tenant ID injection. Provides automatic tenant context management for multi-tenant data isolation.
 */
@MappedSuperclass
public abstract class TenantAwareEntity {

  @Column(name = "tenant_id", nullable = false, length = 100)
  private String tenantId;

  // Default constructor
  protected TenantAwareEntity() {
  }

  // Constructor with tenant ID
  protected TenantAwareEntity(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  /**
   * Automatically inject tenant ID from current tenant context before persisting. This method is called by JPA before the entity is persisted.
   */
  @PrePersist
  protected void setTenantIdFromContext() {
    if (this.tenantId == null) {
      var currentTenantId = TenantContext.getCurrentTenantId();
      if (currentTenantId != null) {
        this.tenantId = currentTenantId;
      }
    }
  }

  /**
   * Validate tenant context before updating entity. Ensures that updates are only performed within the correct tenant context.
   */
  @PreUpdate
  protected void validateTenantContext() {
    var currentTenantId = TenantContext.getCurrentTenantId();
    if (currentTenantId != null && !currentTenantId.equals(this.tenantId)) {
      throw new IllegalStateException(
          "Tenant context mismatch: current=" + currentTenantId +
              ", entity=" + this.tenantId
      );
    }
  }

  /**
   * Check if this entity belongs to the current tenant context.
   *
   * @return true if entity belongs to current tenant, false otherwise
   */
  public boolean belongsToCurrentTenant() {
    var currentTenantId = TenantContext.getCurrentTenantId();
    return currentTenantId != null && currentTenantId.equals(this.tenantId);
  }

  /**
   * Check if this entity belongs to the specified tenant.
   *
   * @param tenantId the tenant ID to check against
   * @return true if entity belongs to specified tenant, false otherwise
   */
  public boolean belongsToTenant(String tenantId) {
    return tenantId != null && tenantId.equals(this.tenantId);
  }
}