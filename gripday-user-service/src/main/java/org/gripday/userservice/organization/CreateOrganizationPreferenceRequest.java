package org.gripday.userservice.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating organization preferences.
 */
public record CreateOrganizationPreferenceRequest(
    @NotNull(message = "Organization ID is required")
    Long organizationId,

    @Size(max = 10, message = "Default locale must not exceed 10 characters")
    String defaultLocale,

    @Size(max = 50, message = "Default timezone must not exceed 50 characters")
    String defaultTimezone,

    @Size(max = 3, message = "Default currency must not exceed 3 characters")
    String defaultCurrency,

    @Size(max = 50, message = "Default date format must not exceed 50 characters")
    String defaultDateFormat,

    @Size(max = 50, message = "Default time format must not exceed 50 characters")
    String defaultTimeFormat,

    Boolean allowUserRegistration,

    Boolean requireEmailVerification,

    @Min(value = 6, message = "Password minimum length must be at least 6")
    Integer passwordMinLength,

    Boolean passwordRequireUppercase,

    Boolean passwordRequireLowercase,

    Boolean passwordRequireNumbers,

    Boolean passwordRequireSpecialChars,

    @Min(value = 1, message = "Session timeout must be at least 1 minute")
    Integer sessionTimeoutMinutes,

    @Min(value = 1, message = "Max login attempts must be at least 1")
    Integer maxLoginAttempts,

    @Min(value = 1, message = "Lockout duration must be at least 1 minute")
    Integer lockoutDurationMinutes,

    Boolean enableTwoFactorAuth,

    Boolean requireTwoFactorAuth,

    @Email(message = "Notification email must be valid")
    @Size(max = 255, message = "Notification email must not exceed 255 characters")
    String notificationEmail,

    @Email(message = "Support email must be valid")
    @Size(max = 255, message = "Support email must not exceed 255 characters")
    String supportEmail,

    String customSettings
) {
}
