package com.iqscaffold.billingservice.security;

import java.util.Set;

/**
 * User context extracted from JWT token.
 *
 * <p>Contains user information from JWT claims for authorization and audit logging.
 * This record is immutable and thread-safe.
 *
 * <p>JWT Claims Mapping:
 * <ul>
 *   <li>userId - from JWT 'sub' claim</li>
 *   <li>username - from JWT 'username' claim</li>
 *   <li>email - from JWT 'email' claim</li>
 *   <li>authorities - from JWT 'roles' claim (contains authorities, not roles)</li>
 *   <li>tenantId - from JWT 'tenant_id' claim</li>
 *   <li>firstName - from JWT 'firstName' claim</li>
 *   <li>lastName - from JWT 'lastName' claim</li>
 * </ul>
 */
public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> authorities,
    String tenantId,
    String firstName,
    String lastName
) {

  /**
   * Check if the user has a specific authority.
   *
   * @param authority the authority to check
   * @return true if the user has the authority, false otherwise
   */
  public boolean hasAuthority(String authority) {
    return authorities != null && authorities.contains(authority);
  }

  /**
   * Check if the user is an admin (has ADMIN or SUPER_ADMIN authority).
   *
   * @return true if the user is an admin, false otherwise
   */
  public boolean isAdmin() {
    return hasAuthority("ADMIN") || hasAuthority("SUPER_ADMIN");
  }

  /**
   * Get the user's full name.
   *
   * @return the full name, or username if names are not available
   */
  public String getFullName() {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else if (lastName != null) {
      return lastName;
    }
    return username;
  }
}
