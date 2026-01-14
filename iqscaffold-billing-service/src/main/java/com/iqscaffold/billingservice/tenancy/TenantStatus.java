package com.iqscaffold.billingservice.tenancy;

/**
 * Enumeration of tenant lifecycle states.
 *
 * <p>Defines the possible states a tenant can be in during its lifecycle:
 * <ul>
 *   <li><strong>ACTIVE</strong> - Tenant is fully operational and accessible</li>
 *   <li><strong>SUSPENDED</strong> - Tenant is temporarily disabled but data is preserved; can be restored</li>
 *   <li><strong>ARCHIVED</strong> - Tenant is archived; data is preserved but inaccessible; cannot be restored</li>
 * </ul>
 *
 * <p>State Transitions:
 * <pre>
 * ACTIVE → SUSPENDED → ARCHIVED
 * ACTIVE → ARCHIVED (direct)
 * SUSPENDED → ACTIVE (restore)
 * ARCHIVED → (terminal state, no transitions)
 * </pre>
 */
public enum TenantStatus {
  /**
   * Tenant is fully operational and accessible to users.
   */
  ACTIVE,

  /**
   * Tenant is temporarily suspended. Data is preserved and can be restored to ACTIVE.
   */
  SUSPENDED,

  /**
   * Tenant is archived. Data is preserved but inaccessible. Terminal state - cannot transition to other states.
   */
  ARCHIVED
}
