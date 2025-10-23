package org.gripday.authservice.presentation.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * User DTO for user management operations using Java 21 record.
 * Contains user information for admin operations with tenant context.
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
        var first = firstName != null ? firstName : "";
        var last = lastName != null ? lastName : "";
        return (first + " " + last).trim();
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