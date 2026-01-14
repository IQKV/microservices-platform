package com.iqscaffold.contactservice.security;

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
   * Check if user is an admin.
   *
   * @return true if user has ADMIN or ROLE_ADMIN authority
   */
  public boolean isAdmin() {
    return authorities.contains("ROLE_ADMIN") || authorities.contains("ADMIN");
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
   * Check if user has USER role.
   *
   * @return true if user has USER or ROLE_USER authority
   */
  public boolean isUser() {
    return authorities.contains("ROLE_USER") || authorities.contains("USER");
  }
}
