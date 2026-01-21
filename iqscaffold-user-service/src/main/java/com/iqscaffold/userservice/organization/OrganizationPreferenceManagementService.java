package com.iqscaffold.userservice.organization;

import com.iqscaffold.userservice.security.UserAuditLog;
import com.iqscaffold.userservice.security.UserAuditLogRepository;
import com.iqscaffold.userservice.tenancy.TenantContext;
import com.iqscaffold.userservice.usermanagement.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for organization preference management operations.
 *
 * <p>Organization preferences are stored in PUBLIC schema alongside organizations.
 * These preferences define organization-wide settings like password policies, security
 * settings, and localization defaults.
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

  /**
   * Get all organization preferences with pagination.
   * SUPER_ADMIN sees all, ADMIN sees only their organization's preferences.
   */
  @Transactional(readOnly = true)
  public Page<OrganizationPreferenceDto> getAllPreferences(Pageable pageable, UserContext currentUser) {
    validateAdminAccess(currentUser, "LIST_ORGANIZATION_PREFERENCES");

    Page<OrganizationPreference> preferences;

    if (currentUser.hasAuthority("SUPER_ADMIN")) {
      // Super admin sees all preferences
      preferences = preferenceRepository.findAll(pageable);
    } else {
      // Regular admin sees only their organization's preferences
      var org = organizationRepository.findByTenantId(currentUser.tenantId())
          .orElseThrow(() -> new OrganizationPreferenceManagementException("Organization not found for tenant: " + currentUser.tenantId()));

      var pref = preferenceRepository.findByOrganizationId(org.getId()).orElse(null);
      if (pref != null) {
        var prefList = java.util.List.of(pref);
        preferences = new org.springframework.data.domain.PageImpl<>(prefList, pageable, 1);
      } else {
        preferences = Page.empty(pageable);
      }
    }

    logAuditEvent("LIST_ORGANIZATION_PREFERENCES", "Listed " + preferences.getNumberOfElements() + " preferences", currentUser);

    return preferences.map(this::convertToDto);
  }

  /**
   * Get preference by ID with tenant validation.
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "organizationPreferences", key = "#preferenceId")
  public OrganizationPreferenceDto getPreferenceById(Long preferenceId, UserContext currentUser) {
    validateAdminAccess(currentUser, "GET_ORGANIZATION_PREFERENCE");

    var preference = findPreferenceByIdWithTenantCheck(preferenceId, currentUser);

    logAuditEvent("GET_ORGANIZATION_PREFERENCE", "Retrieved preference for organization: " + preference.getOrganization().getName(), currentUser);

    return convertToDto(preference);
  }

  /**
   * Get preference by organization ID with tenant validation.
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "organizationPreferences", key = "'org_' + #organizationId")
  public OrganizationPreferenceDto getPreferenceByOrganizationId(Long organizationId, UserContext currentUser) {
    validateAdminAccess(currentUser, "GET_ORGANIZATION_PREFERENCE");

    var organization = findOrganizationByIdWithTenantCheck(organizationId, currentUser);

    var preference = preferenceRepository.findByOrganizationId(organizationId)
        .orElseThrow(() -> new OrganizationPreferenceManagementException("Preference not found for organization: " + organizationId));

    logAuditEvent("GET_ORGANIZATION_PREFERENCE", "Retrieved preference for organization: " + organization.getName(), currentUser);

    return convertToDto(preference);
  }

  /**
   * Get preference for current user's organization.
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "organizationPreferences", key = "'tenant_' + #currentUser.tenantId()")
  public OrganizationPreferenceDto getPreferenceForCurrentTenant(UserContext currentUser) {
    var organization = organizationRepository.findByTenantId(currentUser.tenantId())
        .orElseThrow(() -> new OrganizationPreferenceManagementException("Organization not found for tenant: " + currentUser.tenantId()));

    var preference = preferenceRepository.findByOrganizationId(organization.getId())
        .orElseThrow(() -> new OrganizationPreferenceManagementException("Preference not found for organization: " + organization.getId()));

    return convertToDto(preference);
  }

  /**
   * Create organization preference.
   */
  @CacheEvict(value = "organizationPreferences", allEntries = true)
  public OrganizationPreferenceDto createPreference(CreateOrganizationPreferenceRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "CREATE_ORGANIZATION_PREFERENCE");

    var organization = findOrganizationByIdWithTenantCheck(request.organizationId(), currentUser);

    if (preferenceRepository.existsByOrganizationId(request.organizationId())) {
      throw new OrganizationPreferenceManagementException("Preference already exists for organization: " + request.organizationId());
    }

    var preference = new OrganizationPreference(organization);
    applyRequestToPreference(preference, request);

    var savedPreference = preferenceRepository.save(preference);

    logAuditEvent("CREATE_ORGANIZATION_PREFERENCE", "Created preference for organization: " + organization.getName(), currentUser);

    return convertToDto(savedPreference);
  }

  /**
   * Update organization preference.
   */
  @Caching(evict = {
      @CacheEvict(value = "organizationPreferences", key = "#preferenceId"),
      @CacheEvict(value = "organizationPreferences", allEntries = true)
  })
  public OrganizationPreferenceDto updatePreference(Long preferenceId, UpdateOrganizationPreferenceRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "UPDATE_ORGANIZATION_PREFERENCE");

    var preference = findPreferenceByIdWithTenantCheck(preferenceId, currentUser);

    if (request.defaultLocale() != null) {
      preference.setDefaultLocale(request.defaultLocale());
    }
    if (request.defaultTimezone() != null) {
      preference.setDefaultTimezone(request.defaultTimezone());
    }
    if (request.defaultCurrency() != null) {
      preference.setDefaultCurrency(request.defaultCurrency());
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
    if (request.twoFactorAuthRequired() != null) {
      preference.setTwoFactorAuthRequired(request.twoFactorAuthRequired());
    }
    if (request.notificationEmail() != null) {
      preference.setNotificationEmail(request.notificationEmail());
    }

    var updatedPreference = preferenceRepository.save(preference);

    logAuditEvent("UPDATE_ORGANIZATION_PREFERENCE", "Updated preference for organization: " + preference.getOrganization().getName(), currentUser);

    return convertToDto(updatedPreference);
  }

  /**
   * Delete organization preference.
   */
  @Caching(evict = {
      @CacheEvict(value = "organizationPreferences", key = "#preferenceId"),
      @CacheEvict(value = "organizationPreferences", allEntries = true)
  })
  public void deletePreference(Long preferenceId, UserContext currentUser) {
    validateAdminAccess(currentUser, "DELETE_ORGANIZATION_PREFERENCE");

    var preference = findPreferenceByIdWithTenantCheck(preferenceId, currentUser);
    var organizationName = preference.getOrganization().getName();

    preferenceRepository.delete(preference);

    logAuditEvent("DELETE_ORGANIZATION_PREFERENCE", "Deleted preference for organization: " + organizationName, currentUser);
  }

  private void validateAdminAccess(UserContext currentUser, String operation) {
    if (!currentUser.hasAuthority("ADMIN") && !currentUser.hasAuthority("SUPER_ADMIN")) {
      throw new AccessDeniedException("Insufficient permissions for operation: " + operation);
    }
  }

  private OrganizationPreference findPreferenceByIdWithTenantCheck(Long preferenceId, UserContext currentUser) {
    var preference = preferenceRepository.findById(preferenceId)
        .orElseThrow(() -> new OrganizationPreferenceManagementException("Preference not found: " + preferenceId));

    // Super admin can access any preference
    if (currentUser.hasAuthority("SUPER_ADMIN")) {
      return preference;
    }

    // Regular admin can only access their organization's preference
    var organization = preference.getOrganization();
    if (!organization.getTenantId().equals(currentUser.tenantId())) {
      throw new AccessDeniedException("Access denied to preference: " + preferenceId);
    }

    return preference;
  }

  private Organization findOrganizationByIdWithTenantCheck(Long organizationId, UserContext currentUser) {
    var organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new OrganizationPreferenceManagementException("Organization not found: " + organizationId));

    // Super admin can access any organization
    if (currentUser.hasAuthority("SUPER_ADMIN")) {
      return organization;
    }

    // Regular admin can only access their own organization
    if (!organization.getTenantId().equals(currentUser.tenantId())) {
      throw new AccessDeniedException("Access denied to organization: " + organizationId);
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
    if (request.twoFactorAuthRequired() != null) {
      preference.setTwoFactorAuthRequired(request.twoFactorAuthRequired());
    }
    if (request.notificationEmail() != null) {
      preference.setNotificationEmail(request.notificationEmail());
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
        preference.getTwoFactorAuthRequired(),
        preference.getNotificationEmail(),
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

      TenantContext.executeInTenantContext(currentUser.tenantId(), () -> auditLogRepository.save(auditLog));

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
