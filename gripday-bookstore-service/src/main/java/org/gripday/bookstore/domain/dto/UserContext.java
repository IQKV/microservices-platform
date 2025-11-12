package org.gripday.bookstore.domain.dto;

import java.util.Map;
import java.util.Set;

public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> roles,
    Set<String> permissions,
    String department,
    String organizationId,
    Map<String, Object> customClaims
) {

  public boolean hasRole(String role) {
    return roles != null && roles.contains(role);
  }

  public boolean hasAnyRole(String... roles) {
    if (this.roles == null) {
      return false;
    }
    for (final String role : roles) {
      if (this.roles.contains(role)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAdmin() {
    return hasAnyRole("ADMIN", "SUPERADMIN");
  }

  public boolean isSuperAdmin() {
    return hasRole("SUPERADMIN");
  }
}