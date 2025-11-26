package com.iqscaffold.userservice.usermanagement;

import java.time.LocalDateTime;

/**
 * UserPreference DTO.
 */
public record UserPreferenceDto(
    Long id,
    Long userId,
    String username,
    String locale,
    String timezone,
    String currency,
    String dateFormat,
    String timeFormat,
    String theme,
    String profilePhotoUrl,
    String phoneNumber,
    String bio,
    Boolean notificationEmail,
    Boolean notificationSms,
    Boolean notificationPush,
    Boolean twoFactorEnabled,
    String twoFactorMethod,
    String customSettings,
    String tenantId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
