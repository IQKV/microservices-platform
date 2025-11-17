package org.gripday.userservice.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.gripday.userservice.tenancy.TenantContext;

/**
 * Base class for tenant-aware entities.
 * Provides automatic tenant context management for multi-tenant data isolation.
 * Part of the shared kernel.
 */
@MappedSuperclass
public abstract class TenantAware {

  @Column(name = "tenant_id", nullable = false, length = 100)
  private String tenantId;

  protected TenantAware() {
  }

  protected TenantAware(final String tenantId) {
    this.tenantId = tenantId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @PrePersist
  protected void setTenantIdFromContext() {
    if (this.tenantId == null) {
      var currentTenantId = TenantContext.getCurrentTenantId();
      if (currentTenantId != null) {
        this.tenantId = currentTenantId;
      }
    }
  }

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

  public boolean belongsToCurrentTenant() {
    var currentTenantId = TenantContext.getCurrentTenantId();
    return currentTenantId != null && currentTenantId.equals(this.tenantId);
  }

  public boolean belongsToTenant(final String tenantId) {
    return tenantId != null && tenantId.equals(this.tenantId);
  }
}
