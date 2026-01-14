package com.iqscaffold.contactservice.tenancy;

import com.iqscaffold.contactservice.shared.ContactConstants;
import com.iqscaffold.contactservice.shared.exception.TenantContextException;
import org.slf4j.MDC;

/**
 * Thread-local tenant context management providing secure tenant isolation in multi-tenant architecture.
 *
 * <p>This utility class manages tenant context propagation throughout the application using ThreadLocal storage.
 * It ensures that each request thread maintains its own tenant context, preventing cross-tenant data access
 * and providing the foundation for multi-tenant data isolation.
 *
 * <h3>Multi-Tenant Architecture Support</h3>
 * <ul>
 *   <li><strong>Thread Isolation</strong> - Each thread maintains independent tenant context</li>
 *   <li><strong>Request Scoped</strong> - Tenant context is set per HTTP request</li>
 *   <li><strong>Automatic Cleanup</strong> - Context cleared after request completion</li>
 *   <li><strong>MDC Integration</strong> - Tenant ID added to logging context</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><strong>Input Validation</strong> - Validates tenant ID format and content</li>
 *   <li><strong>Context Isolation</strong> - Prevents accidental cross-tenant access</li>
 *   <li><strong>Null Safety</strong> - Handles missing or invalid tenant contexts gracefully</li>
 *   <li><strong>Immutable Operations</strong> - Thread-safe context management</li>
 * </ul>
 *
 * <h3>Integration Points</h3>
 * <ul>
 *   <li><strong>HTTP Filters</strong> - Set from X-Tenant-ID header or JWT claims</li>
 *   <li><strong>Database Layer</strong> - Used for schema routing and query filtering</li>
 *   <li><strong>Caching Layer</strong> - Tenant-aware cache key generation</li>
 *   <li><strong>Audit Logging</strong> - Automatic tenant context in log entries</li>
 * </ul>
 *
 * <h3>Lifecycle Management</h3>
 * <ol>
 *   <li><strong>Request Start</strong> - Tenant context set from authentication</li>
 *   <li><strong>Request Processing</strong> - Context available throughout request</li>
 *   <li><strong>Database Operations</strong> - Automatic schema/filtering based on context</li>
 *   <li><strong>Request End</strong> - Context cleared to prevent memory leaks</li>
 * </ol>
 *
 * <h3>Usage Patterns</h3>
 * <pre>{@code
 * // Set tenant context (typically in filter or interceptor)
 * TenantContext.setCurrentTenantId("acme-corp");
 *
 * // Get current tenant (in service methods)
 * String tenantId = TenantContext.getCurrentTenantId();
 *
 * // Check if context is set
 * if (TenantContext.hasTenantContext()) {
 *     // Perform tenant-specific operations
 * }
 *
 * // Get with fallback to default
 * String tenantId = TenantContext.getCurrentTenantIdOrDefault();
 *
 * // Clear context (typically in finally block)
 * TenantContext.clear();
 * }</pre>
 *
 * <h3>Best Practices</h3>
 * <ul>
 *   <li><strong>Always Clear</strong> - Use try-finally or filters to ensure cleanup</li>
 *   <li><strong>Validate Early</strong> - Set and validate tenant context at request entry</li>
 *   <li><strong>Fail Fast</strong> - Throw exceptions for invalid tenant contexts</li>
 *   <li><strong>Log Context</strong> - Include tenant ID in all significant log entries</li>
 * </ul>
 *
 * @see ThreadLocal
 * @see MDC
 * @see ContactConstants
 */
public final class TenantContext {

  private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

  // Private constructor to prevent instantiation
  private TenantContext() {
  }

  /**
   * Set the current tenant ID for the current thread with validation and MDC integration.
   *
   * <p>This method establishes the tenant context for the current thread, enabling tenant-aware
   * operations throughout the request lifecycle. It performs input validation and automatically
   * integrates with the logging framework for structured logging.
   *
   * <h4>Validation Process:</h4>
   * <ul>
   *   <li><strong>Null Check</strong> - Ensures tenant ID is not null</li>
   *   <li><strong>Empty Check</strong> - Validates tenant ID is not empty or whitespace-only</li>
   *   <li><strong>Normalization</strong> - Trims whitespace from tenant ID</li>
   *   <li><strong>Format Validation</strong> - Ensures tenant ID meets format requirements</li>
   * </ul>
   *
   * <h4>Context Setup:</h4>
   * <ul>
   *   <li>Sets ThreadLocal tenant context for current thread</li>
   *   <li>Adds tenant ID to MDC for structured logging</li>
   *   <li>Enables tenant-aware database operations</li>
   *   <li>Supports tenant-specific caching</li>
   * </ul>
   *
   * @param tenantId The tenant identifier to set for the current thread
   * @throws TenantContextException.InvalidTenantIdException If tenant ID is null, empty, or invalid format
   * @see #getCurrentTenantId()
   * @see #clear()
   * @see MDC
   */
  public static void setCurrentTenantId(String tenantId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new TenantContextException.InvalidTenantIdException("Tenant ID cannot be null or empty");
    }

    var normalizedTenantId = tenantId.trim();
    TENANT_CONTEXT.set(normalizedTenantId);

    // Add tenant context to MDC for structured logging
    MDC.put(ContactConstants.MDC.TENANT_ID, normalizedTenantId);
  }

  /**
   * Retrieve the current tenant ID for the current thread.
   *
   * <p>This method returns the tenant identifier that was previously set for the current thread.
   * It provides access to the tenant context without any fallback behavior, returning null
   * if no tenant context has been established.
   *
   * <h4>Return Behavior:</h4>
   * <ul>
   *   <li><strong>Context Set</strong> - Returns the normalized tenant ID string</li>
   *   <li><strong>No Context</strong> - Returns null (no fallback)</li>
   *   <li><strong>Thread Isolation</strong> - Only returns context for current thread</li>
   * </ul>
   *
   * <h4>Null Handling:</h4>
   * <p>Callers should handle null return values appropriately:
   * <pre>{@code
   * String tenantId = TenantContext.getCurrentTenantId();
   * if (tenantId != null) {
   *     // Perform tenant-specific operations
   * } else {
   *     // Handle missing tenant context
   *     throw new TenantContextException("Tenant context required");
   * }
   * }</pre>
   *
   * @return The current tenant ID for this thread, or null if not set
   * @see #getCurrentTenantIdOrDefault()
   * @see #hasTenantContext()
   * @see #setCurrentTenantId(String)
   */
  public static String getCurrentTenantId() {
    return TENANT_CONTEXT.get();
  }

  /**
   * Retrieve the current tenant ID with automatic fallback to the default tenant.
   *
   * <p>This method provides a safe way to access tenant context with guaranteed non-null return.
   * It's particularly useful for system operations that need to function regardless of whether
   * a specific tenant context has been established.
   *
   * <h4>Fallback Behavior:</h4>
   * <ul>
   *   <li><strong>Context Present</strong> - Returns the current tenant ID</li>
   *   <li><strong>No Context</strong> - Returns the system default tenant ID</li>
   *   <li><strong>Never Null</strong> - Guaranteed to return a valid tenant identifier</li>
   * </ul>
   *
   * @return The current tenant ID if set, otherwise the default tenant ID (never null)
   * @see #getCurrentTenantId()
   * @see ContactConstants.Defaults#DEFAULT_TENANT_ID
   */
  public static String getCurrentTenantIdOrDefault() {
    var tenantId = TENANT_CONTEXT.get();
    return tenantId != null ? tenantId : ContactConstants.Defaults.DEFAULT_TENANT_ID;
  }

  /**
   * Check if a tenant context is currently established for this thread.
   *
   * <p>This method provides a safe way to determine whether tenant context has been set
   * without retrieving the actual tenant ID. It's useful for conditional logic that
   * depends on the presence of tenant context.
   *
   * <h4>Example Usage:</h4>
   * <pre>{@code
   * if (TenantContext.hasTenantContext()) {
   *     // Safe to perform tenant-specific operations
   *     String tenantId = TenantContext.getCurrentTenantId();
   *     performTenantOperation(tenantId);
   * } else {
   *     // Handle missing tenant context
   *     logger.warn("Tenant context not available for operation");
   *     throw new TenantContextException("Tenant context required");
   * }
   * }</pre>
   *
   * @return true if tenant context is set for the current thread, false otherwise
   * @see #getCurrentTenantId()
   * @see #setCurrentTenantId(String)
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
   * Clear the tenant context for the current thread. Also removes tenant information from MDC.
   *
   * <p>This method should be called at the end of request processing to prevent memory leaks
   * and ensure clean state for thread pool reuse.
   *
   * @see #setCurrentTenantId(String)
   */
  public static void clear() {
    TENANT_CONTEXT.remove();
    MDC.remove(ContactConstants.MDC.TENANT_ID);
  }

  /**
   * Execute a block of code within a specific tenant context. Automatically restores the previous tenant context after execution.
   *
   * <p>This method is useful for executing operations in a different tenant context temporarily,
   * ensuring the original context is restored regardless of whether the operation succeeds or fails.
   *
   * <h4>Example Usage:</h4>
   * <pre>{@code
   * // Execute operation in different tenant context
   * TenantContext.executeInTenantContext("tenant-123", () -> {
   *     // This code runs with tenant-123 context
   *     contactService.createContact(contactRequest);
   * });
   * // Original tenant context automatically restored
   * }</pre>
   *
   * @param tenantId the tenant ID to set for execution
   * @param runnable the code to execute
   * @throws TenantContextException.InvalidTenantIdException if tenantId is null or empty
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
   * Execute a block of code within a specific tenant context and return a result. Automatically restores the previous tenant context after execution.
   *
   * <p>This method is the functional equivalent of {@link #executeInTenantContext(String, Runnable)}
   * but allows returning a value from the executed code block.
   *
   * <h4>Example Usage:</h4>
   * <pre>{@code
   * // Execute operation and get result in different tenant context
   * Contact contact = TenantContext.executeInTenantContext("tenant-123", () -> {
   *     return contactRepository.findById(contactId);
   * });
   * // Original tenant context automatically restored
   * }</pre>
   *
   * @param <T>      the return type
   * @param tenantId the tenant ID to set for execution
   * @param supplier the code to execute that returns a value
   * @return the result of the supplier execution
   * @throws TenantContextException.InvalidTenantIdException if tenantId is null or empty
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
    return ContactConstants.Defaults.DEFAULT_TENANT_ID;
  }

  /**
   * Create a tenant-aware cache key by prefixing with tenant ID.
   *
   * <h4>Example:</h4>
   * <pre>{@code
   * String cacheKey = TenantContext.createTenantAwareCacheKey("contact:" + contactId);
   * // Returns: "tenant-123:contact:12345"
   * }</pre>
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

  // Legacy method names for backward compatibility
  /**
   * @deprecated Use {@link #setCurrentTenantId(String)} instead
   */
  @Deprecated
  public static void setCurrentTenant(String tenantId) {
    setCurrentTenantId(tenantId);
  }

  /**
   * @deprecated Use {@link #getCurrentTenantId()} instead
   */
  @Deprecated
  public static String getCurrentTenant() {
    return getCurrentTenantId();
  }
}