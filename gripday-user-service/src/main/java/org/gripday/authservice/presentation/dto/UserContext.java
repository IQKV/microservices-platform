package org.gripday.authservice.presentation.dto;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable user context record for JWT claims and user data transfer. Contains user information for multi-tenant support.
 */
@Schema(
    name = "UserContext",
    description = "Comprehensive user information and context for authenticated users"
)
public record UserContext(
    @Schema(
        description = "Unique user identifier",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Long userId,

    @Schema(
        description = "Unique username",
        example = "john.doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String username,

    @Schema(
        description = "User's email address",
        example = "john.doe@example.com",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email,

    @Schema(
        description = "Set of user roles for authorization",
        example = "[\"USER\", \"ADMIN\"]"
    )
    Set<String> roles,

    @Schema(
        description = "Set of specific permissions granted to the user",
        example = "[\"READ_PROFILE\", \"WRITE_PROFILE\"]"
    )
    Set<String> permissions,

    @Schema(
        description = "User's first name",
        example = "John"
    )
    String firstName,

    @Schema(
        description = "User's last name",
        example = "Doe"
    )
    String lastName,

    @Schema(
        description = "Tenant identifier for multi-tenant isolation",
        example = "tenant-123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String tenantId,

    @Schema(
        description = "Additional custom claims and metadata",
        example = "{\"department\": \"Engineering\", \"location\": \"US\"}"
    )
    Map<String, Object> customClaims
) {

  // Compact constructor for validation and immutability
  public UserContext {
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(username, "Username cannot be null");
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(tenantId, "Tenant ID cannot be null");

    // Ensure immutable collections
    roles = roles != null ? Set.copyOf(roles) : Set.of();
    permissions = permissions != null ? Set.copyOf(permissions) : Set.of();
    customClaims = customClaims != null ? Map.copyOf(customClaims) : Map.of();
  }

  /**
   * Check if user has a specific role.
   */
  public boolean hasRole(String role) {
    return roles.contains(role);
  }

  /**
   * Check if user has a specific permission.
   */
  public boolean hasPermission(String permission) {
    return permissions.contains(permission);
  }

  /**
   * Get user's full name.
   */
  public String getFullName() {
    var first = firstName != null ? firstName : "";
    var last = lastName != null ? lastName : "";
    return (first + " " + last).trim();
  }

  /**
   * Check if user has admin privileges.
   */
  public boolean isAdmin() {
    return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
  }
}