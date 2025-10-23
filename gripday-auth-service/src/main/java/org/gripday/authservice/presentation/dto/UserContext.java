package org.gripday.authservice.presentation.dto;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable user context record for JWT claims and user data transfer.
 * Contains comprehensive user information for multi-tenant support.
 */
public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> roles,
    Set<String> permissions,
    String firstName,
    String lastName,
    String tenantId,
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