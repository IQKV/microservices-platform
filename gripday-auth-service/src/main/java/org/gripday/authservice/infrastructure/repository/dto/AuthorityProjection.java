package org.gripday.authservice.infrastructure.repository.dto;

import java.time.LocalDateTime;

/**
 * Authority projection record for lightweight authority data transfer.
 * Used for queries that don't require full Authority entity data.
 */
public record AuthorityProjection(
    Long id,
    String name,
    String description,
    LocalDateTime createdAt,
    Long userCount
) {
    
    /**
     * Create an AuthorityProjection without user count.
     */
    public static AuthorityProjection of(Long id, String name, String description, LocalDateTime createdAt) {
        return new AuthorityProjection(id, name, description, createdAt, 0L);
    }
    
    /**
     * Create an AuthorityProjection with user count.
     */
    public static AuthorityProjection withUserCount(Long id, String name, String description, 
                                                   LocalDateTime createdAt, Long userCount) {
        return new AuthorityProjection(id, name, description, createdAt, userCount);
    }
    
    /**
     * Check if authority has users assigned.
     */
    public boolean hasUsers() {
        return userCount != null && userCount > 0;
    }
    
    /**
     * Get display name for UI purposes.
     */
    public String displayName() {
        if (description != null && !description.isBlank()) {
            return name + " - " + description;
        }
        return name;
    }
    
    /**
     * Check if this is an admin authority.
     */
    public boolean isAdminAuthority() {
        return "ADMIN".equals(name) || "SUPER_ADMIN".equals(name);
    }
    
    /**
     * Check if this is a default user authority.
     */
    public boolean isDefaultAuthority() {
        return "USER".equals(name) || "BASIC_USER".equals(name) || "STANDARD_USER".equals(name);
    }
    
    /**
     * Get authority level for sorting purposes.
     */
    public int authorityLevel() {
        return switch (name) {
            case "SUPER_ADMIN" -> 100;
            case "ADMIN" -> 50;
            case "USER", "BASIC_USER", "STANDARD_USER" -> 10;
            default -> 1;
        };
    }
    
    /**
     * Create a compact string representation for logging.
     */
    public String toLogString() {
        return String.format("Authority[id=%d, name=%s, userCount=%d]", id, name, userCount);
    }
}