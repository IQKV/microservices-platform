package org.gripday.userservice.infrastructure.repository.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * User projection record for lightweight user data transfer. Used for queries that don't require full User entity data.
 */
public record UserProjection(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    Boolean enabled,
    Boolean emailVerified,
    LocalDateTime createdAt,
    String tenantId,
    Set<String> authorityNames
) {

  /**
   * Create a UserProjection with basic user information.
   */
  public static UserProjection of(Long id, String username, String email,
                                  String firstName, String lastName,
                                  Boolean enabled, Boolean emailVerified,
                                  LocalDateTime createdAt, String tenantId) {
    return new UserProjection(id, username, email, firstName, lastName,
        enabled, emailVerified, createdAt, tenantId, Set.of());
  }

  /**
   * Create a UserProjection with authorities.
   */
  public static UserProjection withAuthorities(Long id, String username, String email,
                                               String firstName, String lastName,
                                               Boolean enabled, Boolean emailVerified,
                                               LocalDateTime createdAt, String tenantId,
                                               Set<String> authorityNames) {
    return new UserProjection(id, username, email, firstName, lastName,
        enabled, emailVerified, createdAt, tenantId, authorityNames);
  }

  /**
   * Get full name combining first and last name.
   */
  public String fullName() {
    return firstName + " " + lastName;
  }

  /**
   * Check if user is active (enabled and email verified).
   */
  public boolean isActive() {
    return enabled != null && enabled && emailVerified != null && emailVerified;
  }

  /**
   * Check if user has a specific authority.
   */
  public boolean hasAuthority(String authorityName) {
    return authorityNames != null && authorityNames.contains(authorityName);
  }

  /**
   * Check if user has admin privileges.
   */
  public boolean isAdmin() {
    return hasAuthority("ADMIN") || hasAuthority("SUPER_ADMIN");
  }

  /**
   * Get display name for UI purposes.
   */
  public String displayName() {
    var full = fullName();
    return full.isBlank() ? username : full;
  }
}
