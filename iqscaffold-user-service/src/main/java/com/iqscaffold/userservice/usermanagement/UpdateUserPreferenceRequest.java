package com.iqscaffold.userservice.usermanagement;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user preferences.
 */
public record UpdateUserPreferenceRequest(
    @Size(max = 10, message = "Locale must not exceed 10 characters")
    String locale,

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    String timezone,

    @Size(max = 3, message = "Currency must not exceed 3 characters")
    String currency,

    @Size(max = 50, message = "Date format must not exceed 50 characters")
    String dateFormat,

    @Size(max = 50, message = "Time format must not exceed 50 characters")
    String timeFormat,

    @Pattern(regexp = "light|dark|auto", message = "Theme must be 'light', 'dark', or 'auto'")
    String theme,

    @Size(max = 500, message = "Profile photo URL must not exceed 500 characters")
    String profilePhotoUrl,

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    String phoneNumber,

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    String bio,

    Boolean notificationEmail,

    Boolean notificationSms,

    Boolean notificationPush,

    Boolean twoFactorEnabled,

    @Pattern(regexp = "sms|email|app", message = "Two-factor method must be 'sms', 'email', or 'app'")
    String twoFactorMethod,

    String customSettings
) {
}
