package org.gripday.userservice.usermanagement;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * User DTO for user management operations. Contains user information for admin operations with tenant context.
 */
public record UserDto(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    Boolean enabled,
    Boolean emailVerified,
    Set<String> roles,
    String tenantId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  /**
   * Get user's full name.
   */
  public String getFullName() {
    if (firstName == null && lastName == null) {
      return "";
    }
    if (firstName == null) {
      return lastName;
    }
    if (lastName == null) {
      return firstName;
    }
    return firstName + " " + lastName;
  }

  /**
   * Check if user is active (enabled and email verified).
   */
  public boolean isActive() {
    return Boolean.TRUE.equals(enabled) && Boolean.TRUE.equals(emailVerified);
  }

  /**
   * Check if user has admin privileges.
   */
  public boolean isAdmin() {
    return roles != null && (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"));
  }
}
