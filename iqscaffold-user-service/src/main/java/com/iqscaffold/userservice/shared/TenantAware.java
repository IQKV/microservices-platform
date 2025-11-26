package com.iqscaffold.userservice.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import com.iqscaffold.userservice.shared.exception.TenantContextMismatchException;
import com.iqscaffold.userservice.tenancy.TenantContext;

/**
 * Base class for entities that are tenant-aware and require automatic tenant ID injection. Provides automatic tenant context management for multi-tenant data isolation.
 */
@MappedSuperclass
public abstract class TenantAware {

  @Column(name = "tenant_id", nullable = false, length = 100)
  private String tenantId;

  // Default constructor
  protected TenantAware() {
  }

  // Constructor with tenant ID
  protected TenantAware(final String tenantId) {
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
   *
   * @throws TenantContextMismatchException if current tenant context doesn't match entity tenant
   */
  @PreUpdate
  protected void validateTenantContext() {
    var currentTenantId = TenantContext.getCurrentTenantId();
    if (currentTenantId != null && !currentTenantId.equals(this.tenantId)) {
      throw new TenantContextMismatchException(
          "Tenant context mismatch during entity update",
          currentTenantId,
          this.tenantId
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
  public boolean belongsToTenant(final String tenantId) {
    return tenantId != null && tenantId.equals(this.tenantId);
  }
}
