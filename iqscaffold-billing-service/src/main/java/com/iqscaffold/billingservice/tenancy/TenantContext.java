package com.iqscaffold.billingservice.tenancy;

import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.shared.exception.TenantContextException;
import org.slf4j.MDC;

/**
 * ThreadLocal-based tenant context management for multi-tenant architecture.
 * Provides tenant isolation and context propagation throughout the application.
 */
public final class TenantContext {

  private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

  // Private constructor to prevent instantiation
  private TenantContext() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Set the current tenant ID for the current thread.
   * Also adds tenant information to MDC for structured logging.
   *
   * @param tenantId the tenant ID to set
   * @throws TenantContextException.InvalidTenantIdException if tenant ID is null or empty
   */
  public static void setCurrentTenantId(String tenantId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new TenantContextException.InvalidTenantIdException("Tenant ID cannot be null or empty");
    }

    var normalizedTenantId = tenantId.trim();
    TENANT_CONTEXT.set(normalizedTenantId);

    // Add tenant context to MDC for structured logging
    MDC.put(BillingConstants.MDC.TENANT_ID, normalizedTenantId);
  }

  /**
   * Get the current tenant ID for the current thread.
   *
   * @return the current tenant ID, or null if not set
   */
  public static String getCurrentTenantId() {
    return TENANT_CONTEXT.get();
  }

  /**
   * Get the current tenant ID or return the default tenant ID if not set.
   *
   * @return the current tenant ID or default tenant ID
   */
  public static String getCurrentTenantIdOrDefault() {
    var tenantId = TENANT_CONTEXT.get();
    return tenantId != null ? tenantId : BillingConstants.Defaults.DEFAULT_TENANT_ID;
  }

  /**
   * Check if a tenant context is currently set.
   *
   * @return true if tenant context is set, false otherwise
   */
  public static boolean hasTenantContext() {
    return TENANT_CONTEXT.get() != null;
  }

  /**
   * Check if the current tenant matches the specified tenant ID.
   *
   * @param tenantId the tenant ID to check against
   * @return true if current tenant matches, false otherwise
   */
  public static boolean isCurrentTenant(String tenantId) {
    var currentTenantId = TENANT_CONTEXT.get();
    return currentTenantId != null && currentTenantId.equals(tenantId);
  }

  /**
   * Clear the tenant context for the current thread.
   * Also removes tenant information from MDC.
   */
  public static void clear() {
    TENANT_CONTEXT.remove();
    MDC.remove(BillingConstants.MDC.TENANT_ID);
  }

  /**
   * Execute a block of code within a specific tenant context.
   * Automatically restores the previous tenant context after execution.
   *
   * @param tenantId the tenant ID to set for execution
   * @param runnable the code to execute
   */
  public static void executeInTenantContext(String tenantId, Runnable runnable) {
    var previousTenantId = getCurrentTenantId();
    try {
      setCurrentTenantId(tenantId);
      runnable.run();
    } finally {
      if (previousTenantId != null) {
        setCurrentTenantId(previousTenantId);
      } else {
        clear();
      }
    }
  }

  /**
   * Execute a block of code within a specific tenant context and return a result.
   * Automatically restores the previous tenant context after execution.
   *
   * @param <T>      the return type
   * @param tenantId the tenant ID to set for execution
   * @param supplier the code to execute that returns a value
   * @return the result of the supplier execution
   */
  public static <T> T executeInTenantContext(String tenantId, java.util.function.Supplier<T> supplier) {
    var previousTenantId = getCurrentTenantId();
    try {
      setCurrentTenantId(tenantId);
      return supplier.get();
    } finally {
      if (previousTenantId != null) {
        setCurrentTenantId(previousTenantId);
      } else {
        clear();
      }
    }
  }

  /**
   * Get the default tenant ID used when no tenant context is set.
   *
   * @return the default tenant ID
   */
  public static String getDefaultTenantId() {
    return BillingConstants.Defaults.DEFAULT_TENANT_ID;
  }

  /**
   * Create a tenant-aware cache key by prefixing with tenant ID.
   *
   * @param key the base cache key
   * @return tenant-prefixed cache key
   */
  public static String createTenantAwareCacheKey(String key) {
    var tenantId = getCurrentTenantIdOrDefault();
    return tenantId + ":" + key;
  }

  /**
   * Create a tenant-aware cache key with custom separator.
   *
   * @param key       the base cache key
   * @param separator the separator to use between tenant ID and key
   * @return tenant-prefixed cache key
   */
  public static String createTenantAwareCacheKey(String key, String separator) {
    var tenantId = getCurrentTenantIdOrDefault();
    return tenantId + separator + key;
  }
}
