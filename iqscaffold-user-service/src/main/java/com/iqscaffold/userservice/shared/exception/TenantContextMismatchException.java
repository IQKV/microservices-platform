package com.iqscaffold.userservice.shared.exception;

/**
 * Exception thrown when there is a mismatch between the current tenant context
 * and the tenant associated with an entity or operation.
 * This is a security-critical exception that prevents cross-tenant data access.
 */
public class TenantContextMismatchException extends RuntimeException {

  private final String currentTenantId;
  private final String entityTenantId;

  public TenantContextMismatchException(final String message, final String currentTenantId, final String entityTenantId) {
    super(message);
    this.currentTenantId = currentTenantId;
    this.entityTenantId = entityTenantId;
  }

  public String getCurrentTenantId() {
    return currentTenantId;
  }

  public String getEntityTenantId() {
    return entityTenantId;
  }

  @Override
  public String toString() {
    return String.format("%s [currentTenant=%s, entityTenant=%s]",
        getMessage(), currentTenantId, entityTenantId);
  }
}
