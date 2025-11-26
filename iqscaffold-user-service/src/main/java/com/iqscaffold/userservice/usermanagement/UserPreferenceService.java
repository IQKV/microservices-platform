package com.iqscaffold.userservice.usermanagement;

import com.iqscaffold.userservice.security.UserAuditLog;
import com.iqscaffold.userservice.security.UserAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user preference operations allowing users to manage their own preferences.
 */
@Service
@Transactional
public class UserPreferenceService {

  private static final Logger logger = LoggerFactory.getLogger(UserPreferenceService.class);

  private final UserPreferenceRepository preferenceRepository;
  private final UserRepository userRepository;
  private final UserAuditLogRepository auditLogRepository;

  public UserPreferenceService(
      final UserPreferenceRepository preferenceRepository,
      final UserRepository userRepository,
      final UserAuditLogRepository auditLogRepository) {
    this.preferenceRepository = preferenceRepository;
    this.userRepository = userRepository;
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "userPreferences", key = "#currentUser.userId() + '_' + #currentUser.tenantId()",
             condition = "#currentUser != null && #currentUser.tenantId() != null")
  public UserPreferenceDto getMyPreferences(UserContext currentUser) {
    var tenantId = currentUser.tenantId();
    var userId = currentUser.userId();

    var preference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultPreference(userId, tenantId))
    );

    logAuditEvent("GET_USER_PREFERENCE", "Retrieved preferences", currentUser);

    return convertToDto(preference);
  }

  @Caching(evict = {
      @CacheEvict(value = "userPreferences", key = "#currentUser.userId() + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "userPreferences", allEntries = true)
  })
  public UserPreferenceDto updateMyPreferences(UpdateUserPreferenceRequest request, UserContext currentUser) {
    var tenantId = currentUser.tenantId();
    var userId = currentUser.userId();

    var preference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultPreference(userId, tenantId))
    );

    if (!preference.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Preference not found in current tenant");
    }

    applyUpdates(preference, request);

    var updatedPreference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.save(preference)
    );

    logAuditEvent("UPDATE_USER_PREFERENCE", "Updated preferences", currentUser);

    return convertToDto(updatedPreference);
  }

  @Caching(evict = {
      @CacheEvict(value = "userPreferences", key = "#currentUser.userId() + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "userPreferences", allEntries = true)
  })
  public void deleteMyPreferences(UserContext currentUser) {
    var tenantId = currentUser.tenantId();
    var userId = currentUser.userId();

    var preference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.findByUserId(userId)
            .orElseThrow(() -> new UserPreferenceException("Preference not found"))
    );

    if (!preference.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Preference not found in current tenant");
    }

    com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.delete(preference)
    );

    logAuditEvent("DELETE_USER_PREFERENCE", "Deleted preferences", currentUser);
  }

  private UserPreference createDefaultPreference(Long userId, String tenantId) {
    var user = userRepository.findById(userId)
        .orElseThrow(() -> new UserPreferenceException("User not found: " + userId));

    if (!user.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("User not found in current tenant");
    }

    var preference = new UserPreference(user, tenantId);
    return preferenceRepository.save(preference);
  }

  private void applyUpdates(UserPreference preference, UpdateUserPreferenceRequest request) {
    if (request.locale() != null) {
      preference.setLocale(request.locale());
    }
    if (request.timezone() != null) {
      preference.setTimezone(request.timezone());
    }
    if (request.currency() != null) {
      preference.setCurrency(request.currency());
    }
    if (request.dateFormat() != null) {
      preference.setDateFormat(request.dateFormat());
    }
    if (request.timeFormat() != null) {
      preference.setTimeFormat(request.timeFormat());
    }
    if (request.theme() != null) {
      preference.setTheme(request.theme());
    }
    if (request.profilePhotoUrl() != null) {
      preference.setProfilePhotoUrl(request.profilePhotoUrl());
    }
    if (request.phoneNumber() != null) {
      preference.setPhoneNumber(request.phoneNumber());
    }
    if (request.bio() != null) {
      preference.setBio(request.bio());
    }
    if (request.notificationEmail() != null) {
      preference.setNotificationEmail(request.notificationEmail());
    }
    if (request.notificationSms() != null) {
      preference.setNotificationSms(request.notificationSms());
    }
    if (request.notificationPush() != null) {
      preference.setNotificationPush(request.notificationPush());
    }
    if (request.twoFactorEnabled() != null) {
      preference.setTwoFactorEnabled(request.twoFactorEnabled());
    }
    if (request.twoFactorMethod() != null) {
      preference.setTwoFactorMethod(request.twoFactorMethod());
    }
    if (request.customSettings() != null) {
      preference.setCustomSettings(request.customSettings());
    }
  }

  private UserPreferenceDto convertToDto(UserPreference preference) {
    var user = preference.getUser();
    return new UserPreferenceDto(
        preference.getId(),
        user.getId(),
        user.getUsername(),
        preference.getLocale(),
        preference.getTimezone(),
        preference.getCurrency(),
        preference.getDateFormat(),
        preference.getTimeFormat(),
        preference.getTheme(),
        preference.getProfilePhotoUrl(),
        preference.getPhoneNumber(),
        preference.getBio(),
        preference.getNotificationEmail(),
        preference.getNotificationSms(),
        preference.getNotificationPush(),
        preference.getTwoFactorEnabled(),
        preference.getTwoFactorMethod(),
        preference.getCustomSettings(),
        preference.getTenantId(),
        preference.getCreatedAt(),
        preference.getUpdatedAt()
    );
  }

  private void logAuditEvent(String action, String details, UserContext currentUser) {
    try {
      var auditLog = new UserAuditLog(
          currentUser.userId(),
          action,
          details,
          MDC.get("clientIp"),
          MDC.get("userAgent"),
          currentUser.tenantId()
      );

      com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
          currentUser.tenantId(),
          () -> auditLogRepository.save(auditLog)
      );

      logger.info("User preference audit: {} - {} by user {} in tenant {}",
          action, details, currentUser.username(), currentUser.tenantId());
    } catch (final Exception e) {
      logger.error("Failed to log audit event: {}", e.getMessage(), e);
    }
  }

  public static class UserPreferenceException extends RuntimeException {

    public UserPreferenceException(final String message) {
      super(message);
    }

    public UserPreferenceException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
