package com.iqscaffold.userservice.organization;

import java.time.LocalDateTime;

/**
 * OrganizationPreference DTO.
 */
public record OrganizationPreferenceDto(
    Long id,
    Long organizationId,
    String organizationName,
    String defaultLocale,
    String defaultTimezone,
    String defaultCurrency,
    String defaultDateFormat,
    String defaultTimeFormat,
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
    Boolean enableTwoFactorAuth,
    Boolean requireTwoFactorAuth,
    String notificationEmail,
    String supportEmail,
    String customSettings,
    String tenantId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
