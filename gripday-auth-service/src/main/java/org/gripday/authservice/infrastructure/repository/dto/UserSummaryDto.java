package org.gripday.authservice.infrastructure.repository.dto;

import java.time.LocalDateTime;

/**
 * User summary DTO record for minimal user information display.
 * Used in lists, dropdowns, and summary views where full user data is not needed.
 */
public record UserSummaryDto(
    Long id,
    String username,
    String email,
    String displayName,
    Boolean enabled,
    String tenantId,
    LocalDateTime lastActivity
) {
    
    /**
     * Create a UserSummaryDto from basic user information.
     */
    public static UserSummaryDto of(Long id, String username, String email, 
                                   String firstName, String lastName, 
                                   Boolean enabled, String tenantId) {
        var displayName = createDisplayName(firstName, lastName, username);
        return new UserSummaryDto(id, username, email, displayName, enabled, tenantId, null);
    }
    
    /**
     * Create a UserSummaryDto with last activity information.
     */
    public static UserSummaryDto withActivity(Long id, String username, String email, 
                                            String firstName, String lastName, 
                                            Boolean enabled, String tenantId,
                                            LocalDateTime lastActivity) {
        var displayName = createDisplayName(firstName, lastName, username);
        return new UserSummaryDto(id, username, email, displayName, enabled, tenantId, lastActivity);
    }
    
    /**
     * Create display name from first name, last name, and username.
     */
    private static String createDisplayName(String firstName, String lastName, String username) {
        if (firstName != null && lastName != null && !firstName.isBlank() && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return username;
    }
    
    /**
     * Check if user is currently active.
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    /**
     * Get user status as string.
     */
    public String status() {
        return isEnabled() ? "ACTIVE" : "INACTIVE";
    }
    
    /**
     * Check if user has recent activity (within last 30 days).
     */
    public boolean hasRecentActivity() {
        if (lastActivity == null) return false;
        var thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return lastActivity.isAfter(thirtyDaysAgo);
    }
    
    /**
     * Get activity status description.
     */
    public String activityStatus() {
        if (lastActivity == null) return "No activity recorded";
        if (hasRecentActivity()) return "Recently active";
        return "Inactive";
    }
    
    /**
     * Create a compact string representation for logging.
     */
    public String toLogString() {
        return String.format("User[id=%d, username=%s, tenant=%s, enabled=%s]", 
                           id, username, tenantId, enabled);
    }
}