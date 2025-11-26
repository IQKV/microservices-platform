package com.iqscaffold.userservice.organization;

import com.iqscaffold.userservice.security.UserAuditLog;
import com.iqscaffold.userservice.security.UserAuditLogRepository;
import com.iqscaffold.userservice.usermanagement.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for organization preference management operations with admin-only access and tenant isolation.
 */
@Service
@Transactional
public class OrganizationPreferenceManagementService {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationPreferenceManagementService.class);

  private final OrganizationPreferenceRepository preferenceRepository;
  private final OrganizationRepository organizationRepository;
  private final UserAuditLogRepository auditLogRepository;

  public OrganizationPreferenceManagementService(
      final OrganizationPreferenceRepository preferenceRepository,
      final OrganizationRepository organizationRepository,
      final UserAuditLogRepository auditLogRepository) {
    this.preferenceRepository = preferenceRepository;
    this.organizationRepository = organizationRepository;
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(readOnly = true)
  public Page<OrganizationPreferenceDto> getAllPreferences(Pageable pageable, UserContext currentUser) {
    validateAdminAccess(currentUser, "LIST_ORGANIZATION_PREFERENCES");

    var tenantId = currentUser.tenantId();
    var preferences = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.findAll()
    );

    var preferenceDtos = preferences.stream()
        .map(this::convertToDto)
        .toList();

    var start = (int) pageable.getOffset();
    var end = Math.min((start + pageable.getPageSize()), preferenceDtos.size());
    var pageContent = preferenceDtos.subList(start, end);

    logAuditEvent("LIST_ORGANIZATION_PREFERENCES", "Listed " + pageContent.size() + " preferences", currentUser);

    return new PageImpl<>(pageContent, pageable, preferenceDtos.size());
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "organizationPreferences", key = "#preferenceId + '_' + #currentUser.tenantId()",
             condition = "#currentUser != null && #currentUser.tenantId() != null")
  public OrganizationPreferenceDto getPreferenceById(Long preferenceId, UserContext currentUser) {
    validateAdminAccess(currentUser, "GET_ORGANIZATION_PREFERENCE");

    var preference = findPreferenceByIdWithTenantCheck(preferenceId, currentUser.tenantId());

    logAuditEvent("GET_ORGANIZATION_PREFERENCE", "Retrieved preference for organization: " + preference.getOrganization().getName(), currentUser);

    return convertToDto(preference);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "organizationPreferences", key = "'org_' + #organizationId + '_' + #currentUser.tenantId()",
             condition = "#currentUser != null && #currentUser.tenantId() != null")
  public OrganizationPreferenceDto getPreferenceByOrganizationId(Long organizationId, UserContext currentUser) {
    validateAdminAccess(currentUser, "GET_ORGANIZATION_PREFERENCE");

    var tenantId = currentUser.tenantId();
    var organization = findOrganizationByIdWithTenantCheck(organizationId, tenantId);

    var preference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.findByOrganizationId(organizationId)
            .orElseThrow(() -> new OrganizationPreferenceManagementException("Preference not found for organization: " + organizationId))
    );

    if (!preference.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Preference not found in current tenant");
    }

    logAuditEvent("GET_ORGANIZATION_PREFERENCE", "Retrieved preference for organization: " + organization.getName(), currentUser);

    return convertToDto(preference);
  }

  @CacheEvict(value = "organizationPreferences", allEntries = true, condition = "#currentUser != null && #currentUser.tenantId() != null")
  public OrganizationPreferenceDto createPreference(CreateOrganizationPreferenceRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "CREATE_ORGANIZATION_PREFERENCE");

    var tenantId = currentUser.tenantId();
    var organization = findOrganizationByIdWithTenantCheck(request.organizationId(), tenantId);

    if (com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.existsByOrganizationId(request.organizationId())
    )) {
      throw new OrganizationPreferenceManagementException("Preference already exists for organization: " + request.organizationId());
    }

    var preference = new OrganizationPreference(organization, tenantId);
    applyRequestToPreference(preference, request);

    var savedPreference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.save(preference)
    );

    logAuditEvent("CREATE_ORGANIZATION_PREFERENCE", "Created preference for organization: " + organization.getName(), currentUser);

    return convertToDto(savedPreference);
  }

  @Caching(evict = {
      @CacheEvict(value = "organizationPreferences", key = "#preferenceId + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "organizationPreferences", allEntries = true)
  })
  public OrganizationPreferenceDto updatePreference(Long preferenceId, UpdateOrganizationPreferenceRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "UPDATE_ORGANIZATION_PREFERENCE");

    var preference = findPreferenceByIdWithTenantCheck(preferenceId, currentUser.tenantId());

    if (request.defaultLocale() != null) {
      preference.setDefaultLocale(request.defaultLocale());
    }
    if (request.defaultTimezone() != null) {
      preference.setDefaultTimezone(request.defaultTimezone());
    }
    if (request.defaultCurrency() != null) {
      preference.setDefaultCurrency(request.defaultCurrency());
    }
    if (request.defaultDateFormat() != null) {
      preference.setDefaultDateFormat(request.defaultDateFormat());
    }
    if (request.defaultTimeFormat() != null) {
      preference.setDefaultTimeFormat(request.defaultTimeFormat());
    }
    if (request.allowUserRegistration() != null) {
      preference.setAllowUserRegistration(request.allowUserRegistration());
    }
    if (request.requireEmailVerification() != null) {
      preference.setRequireEmailVerification(request.requireEmailVerification());
    }
    if (request.passwordMinLength() != null) {
      preference.setPasswordMinLength(request.passwordMinLength());
    }
    if (request.passwordRequireUppercase() != null) {
      preference.setPasswordRequireUppercase(request.passwordRequireUppercase());
    }
    if (request.passwordRequireLowercase() != null) {
      preference.setPasswordRequireLowercase(request.passwordRequireLowercase());
    }
    if (request.passwordRequireNumbers() != null) {
      preference.setPasswordRequireNumbers(request.passwordRequireNumbers());
    }
    if (request.passwordRequireSpecialChars() != null) {
      preference.setPasswordRequireSpecialChars(request.passwordRequireSpecialChars());
    }
    if (request.sessionTimeoutMinutes() != null) {
      preference.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
    }
    if (request.maxLoginAttempts() != null) {
      preference.setMaxLoginAttempts(request.maxLoginAttempts());
    }
    if (request.lockoutDurationMinutes() != null) {
      preference.setLockoutDurationMinutes(request.lockoutDurationMinutes());
    }
    if (request.enableTwoFactorAuth() != null) {
      preference.setEnableTwoFactorAuth(request.enableTwoFactorAuth());
    }
    if (request.requireTwoFactorAuth() != null) {
      preference.setRequireTwoFactorAuth(request.requireTwoFactorAuth());
    }
    if (request.notificationEmail() != null) {
      preference.setNotificationEmail(request.notificationEmail());
    }
    if (request.supportEmail() != null) {
      preference.setSupportEmail(request.supportEmail());
    }
    if (request.customSettings() != null) {
      preference.setCustomSettings(request.customSettings());
    }

    var updatedPreference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        currentUser.tenantId(),
        () -> preferenceRepository.save(preference)
    );

    logAuditEvent("UPDATE_ORGANIZATION_PREFERENCE", "Updated preference for organization: " + preference.getOrganization().getName(), currentUser);

    return convertToDto(updatedPreference);
  }

  @Caching(evict = {
      @CacheEvict(value = "organizationPreferences", key = "#preferenceId + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "organizationPreferences", allEntries = true)
  })
  public void deletePreference(Long preferenceId, UserContext currentUser) {
    validateAdminAccess(currentUser, "DELETE_ORGANIZATION_PREFERENCE");

    var preference = findPreferenceByIdWithTenantCheck(preferenceId, currentUser.tenantId());
    var organizationName = preference.getOrganization().getName();

    com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        currentUser.tenantId(),
        () -> preferenceRepository.delete(preference)
    );

    logAuditEvent("DELETE_ORGANIZATION_PREFERENCE", "Deleted preference for organization: " + organizationName, currentUser);
  }

  private void validateAdminAccess(UserContext currentUser, String operation) {
    if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("SUPER_ADMIN")) {
      throw new AccessDeniedException("Insufficient permissions for operation: " + operation);
    }
  }

  private OrganizationPreference findPreferenceByIdWithTenantCheck(Long preferenceId, String tenantId) {
    var preference = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> preferenceRepository.findById(preferenceId)
            .orElseThrow(() -> new OrganizationPreferenceManagementException("Preference not found: " + preferenceId))
    );

    if (!preference.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Preference not found in current tenant");
    }

    return preference;
  }

  private Organization findOrganizationByIdWithTenantCheck(Long organizationId, String tenantId) {
    var organization = com.iqscaffold.userservice.tenancy.TenantContext.executeInTenantContext(
        tenantId,
        () -> organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationPreferenceManagementException("Organization not found: " + organizationId))
    );

    if (!organization.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Organization not found in current tenant");
    }

    return organization;
  }

  private void applyRequestToPreference(OrganizationPreference preference, CreateOrganizationPreferenceRequest request) {
    if (request.defaultLocale() != null) {
      preference.setDefaultLocale(request.defaultLocale());
    }
    if (request.defaultTimezone() != null) {
      preference.setDefaultTimezone(request.defaultTimezone());
    }
    if (request.defaultCurrency() != null) {
      preference.setDefaultCurrency(request.defaultCurrency());
    }
    if (request.defaultDateFormat() != null) {
      preference.setDefaultDateFormat(request.defaultDateFormat());
    }
    if (request.defaultTimeFormat() != null) {
      preference.setDefaultTimeFormat(request.defaultTimeFormat());
    }
    if (request.allowUserRegistration() != null) {
      preference.setAllowUserRegistration(request.allowUserRegistration());
    }
    if (request.requireEmailVerification() != null) {
      preference.setRequireEmailVerification(request.requireEmailVerification());
    }
    if (request.passwordMinLength() != null) {
      preference.setPasswordMinLength(request.passwordMinLength());
    }
    if (request.passwordRequireUppercase() != null) {
      preference.setPasswordRequireUppercase(request.passwordRequireUppercase());
    }
    if (request.passwordRequireLowercase() != null) {
      preference.setPasswordRequireLowercase(request.passwordRequireLowercase());
    }
    if (request.passwordRequireNumbers() != null) {
      preference.setPasswordRequireNumbers(request.passwordRequireNumbers());
    }
    if (request.passwordRequireSpecialChars() != null) {
      preference.setPasswordRequireSpecialChars(request.passwordRequireSpecialChars());
    }
    if (request.sessionTimeoutMinutes() != null) {
      preference.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
    }
    if (request.maxLoginAttempts() != null) {
      preference.setMaxLoginAttempts(request.maxLoginAttempts());
    }
    if (request.lockoutDurationMinutes() != null) {
      preference.setLockoutDurationMinutes(request.lockoutDurationMinutes());
    }
    if (request.enableTwoFactorAuth() != null) {
      preference.setEnableTwoFactorAuth(request.enableTwoFactorAuth());
    }
    if (request.requireTwoFactorAuth() != null) {
      preference.setRequireTwoFactorAuth(request.requireTwoFactorAuth());
    }
    if (request.notificationEmail() != null) {
      preference.setNotificationEmail(request.notificationEmail());
    }
    if (request.supportEmail() != null) {
      preference.setSupportEmail(request.supportEmail());
    }
    if (request.customSettings() != null) {
      preference.setCustomSettings(request.customSettings());
    }
  }

  private OrganizationPreferenceDto convertToDto(OrganizationPreference preference) {
    var organization = preference.getOrganization();
    return new OrganizationPreferenceDto(
        preference.getId(),
        organization.getId(),
        organization.getName(),
        preference.getDefaultLocale(),
        preference.getDefaultTimezone(),
        preference.getDefaultCurrency(),
        preference.getDefaultDateFormat(),
        preference.getDefaultTimeFormat(),
        preference.getAllowUserRegistration(),
        preference.getRequireEmailVerification(),
        preference.getPasswordMinLength(),
        preference.getPasswordRequireUppercase(),
        preference.getPasswordRequireLowercase(),
        preference.getPasswordRequireNumbers(),
        preference.getPasswordRequireSpecialChars(),
        preference.getSessionTimeoutMinutes(),
        preference.getMaxLoginAttempts(),
        preference.getLockoutDurationMinutes(),
        preference.getEnableTwoFactorAuth(),
        preference.getRequireTwoFactorAuth(),
        preference.getNotificationEmail(),
        preference.getSupportEmail(),
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

      logger.info("Organization preference management audit: {} - {} by user {} in tenant {}",
          action, details, currentUser.username(), currentUser.tenantId());
    } catch (final Exception e) {
      logger.error("Failed to log audit event: {}", e.getMessage(), e);
    }
  }

  public static class OrganizationPreferenceManagementException extends RuntimeException {

    public OrganizationPreferenceManagementException(final String message) {
      super(message);
    }

    public OrganizationPreferenceManagementException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
