package com.iqscaffold.billingservice.security;

import java.util.Set;

/**
 * User context record containing authenticated user information extracted from JWT.
 * Provides helper methods for authorization checks.
 */
public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> authorities,
    String tenantId,
    Long organizationId,
    String firstName,
    String lastName
) {
  /**
   * Check if user has a specific authority.
   *
   * @param authority the authority to check
   * @return true if user has the authority
   */
  public boolean hasAuthority(String authority) {
    return authorities.contains(authority);
  }

  /**
   * Check if user is an admin (legacy method for backward compatibility).
   * Note: ADMIN role does NOT have billing access.
   *
   * @return true if user has ADMIN or ROLE_ADMIN authority
   */
  public boolean isAdmin() {
    return authorities.contains("ROLE_ADMIN") || authorities.contains("ADMIN");
  }

  /**
   * Check if user has any billing access (read or write).
   * Includes: SUPER_ADMIN, TENANT_OWNER, BILLING_ADMIN, FINANCE_VIEWER
   *
   * @return true if user can access billing data
   */
  public boolean hasBillingAccess() {
    return authorities.contains("SUPER_ADMIN")
           || authorities.contains("TENANT_OWNER")
           || authorities.contains("BILLING_ADMIN")
           || authorities.contains("FINANCE_VIEWER");
  }

  /**
   * Check if user can modify billing data (write operations).
   * Includes: SUPER_ADMIN, TENANT_OWNER, BILLING_ADMIN
   * Excludes: FINANCE_VIEWER (read-only)
   *
   * @return true if user can modify billing data
   */
  public boolean canModifyBilling() {
    return authorities.contains("SUPER_ADMIN")
           || authorities.contains("TENANT_OWNER")
           || authorities.contains("BILLING_ADMIN");
  }

  /**
   * Check if user has read-only billing access.
   *
   * @return true if user is a FINANCE_VIEWER
   */
  public boolean hasReadOnlyBillingAccess() {
    return authorities.contains("FINANCE_VIEWER");
  }

  /**
   * Check if user is a super admin with platform-wide access.
   *
   * @return true if user has SUPER_ADMIN authority
   */
  public boolean isSuperAdmin() {
    return authorities.contains("SUPER_ADMIN");
  }

  /**
   * Check if user is a tenant owner with full organizational access.
   *
   * @return true if user has TENANT_OWNER authority
   */
  public boolean isTenantOwner() {
    return authorities.contains("TENANT_OWNER");
  }
}
