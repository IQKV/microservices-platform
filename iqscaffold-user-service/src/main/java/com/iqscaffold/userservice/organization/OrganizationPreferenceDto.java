package com.iqscaffold.userservice.organization;

import java.time.LocalDateTime;

/**
 * OrganizationPreference DTO for system-wide organization preferences (public schema).
 */
public record OrganizationPreferenceDto(
    Long id,
    Long organizationId,
    String organizationName,
    String defaultLocale,
    String defaultTimezone,
    String defaultCurrency,
    Boolean allowUserRegistration,
    Boolean requireEmailVerification,
    Integer passwordMinLength,
    Boolean passwordRequireUppercase,
    Boolean passwordRequireLowercase,
    Boolean passwordRequireNumbers,
    Boolean passwordRequireSpecialChars,
    Integer sessionTimeoutMinutes,
    Integer maxLoginAttempts,
    Integer lockoutDurationMinutes,
    Boolean twoFactorAuthRequired,
    String notificationEmail,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
