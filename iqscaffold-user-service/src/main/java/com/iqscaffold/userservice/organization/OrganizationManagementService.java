package com.iqscaffold.userservice.organization;

import com.iqscaffold.userservice.security.UserAuditLog;
import com.iqscaffold.userservice.security.UserAuditLogRepository;
import com.iqscaffold.userservice.tenancy.TenantContext;
import com.iqscaffold.userservice.usermanagement.UserContext;
import com.iqscaffold.userservice.usermanagement.UserRepository;
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
 * Service for organization management operations.
 *
 * <p>Organizations are stored in PUBLIC schema (system-wide) and represent billing entities
 * with a 1:1 relationship to tenants. This service provides CRUD operations with proper
 * authorization checks.
 *
 * <h3>Access Control</h3>
 * <ul>
 *   <li>SUPER_ADMIN - Can manage all organizations across all tenants</li>
 *   <li>ADMIN - Can only manage their own organization (tenant-scoped)</li>
 * </ul>
 */
@Service
@Transactional
public class OrganizationManagementService {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationManagementService.class);

  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;
  private final UserAuditLogRepository auditLogRepository;

  public OrganizationManagementService(
      final OrganizationRepository organizationRepository,
      final UserRepository userRepository,
      final UserAuditLogRepository auditLogRepository) {
    this.organizationRepository = organizationRepository;
    this.userRepository = userRepository;
    this.auditLogRepository = auditLogRepository;
  }

  /**
   * Get all organizations with pagination.
   * SUPER_ADMIN sees all, ADMIN sees only their organization.
   */
  @Transactional(readOnly = true)
  public Page<OrganizationDto> getAllOrganizations(Pageable pageable, UserContext currentUser) {
    validateAdminAccess(currentUser, "LIST_ORGANIZATIONS");

    Page<Organization> organizations;

    if (currentUser.hasAuthority("SUPER_ADMIN")) {
      // Super admin sees all organizations
      organizations = organizationRepository.findAll(pageable);
    } else {
      // Regular admin sees only their organization
      var org = organizationRepository.findByTenantId(currentUser.tenantId())
          .orElseThrow(() -> new OrganizationManagementException("Organization not found for tenant: " + currentUser.tenantId()));
      organizations = Page.empty(pageable);
      // Return single organization as page
      var orgList = java.util.List.of(org);
      organizations = new org.springframework.data.domain.PageImpl<>(orgList, pageable, 1);
    }

    logAuditEvent("LIST_ORGANIZATIONS", "Listed " + organizations.getNumberOfElements() + " organizations", currentUser);

    return organizations.map(this::convertToDto);
  }

  /**
   * Get organization by ID with tenant validation.
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "organizations", key = "#organizationId")
  public OrganizationDto getOrganizationById(Long organizationId, UserContext currentUser) {
    validateAdminAccess(currentUser, "GET_ORGANIZATION");

    var organization = findOrganizationByIdWithTenantCheck(organizationId, currentUser);

    logAuditEvent("GET_ORGANIZATION", "Retrieved organization: " + organization.getName(), currentUser);

    return convertToDto(organization);
  }

  /**
   * Get organization for current user's tenant.
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "organizations", key = "'tenant_' + #currentUser.tenantId()")
  public OrganizationDto getOrganizationForCurrentTenant(UserContext currentUser) {
    var organization = organizationRepository.findByTenantId(currentUser.tenantId())
        .orElseThrow(() -> new OrganizationManagementException("Organization not found for tenant: " + currentUser.tenantId()));

    return convertToDto(organization);
  }

  /**
   * Get organization by tenant ID.
   * Internal endpoint for service-to-service communication.
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "organizations", key = "'tenant_' + #tenantId")
  public OrganizationDto getOrganizationByTenantId(String tenantId) {
    var organization = organizationRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new OrganizationManagementException("Organization not found for tenant: " + tenantId));

    return convertToDto(organization);
  }

  /**
   * Create a default organization for self-service tenant provisioning.
   * This method is used internally during tenant signup and does not require user context.
   *
   * @param name      organization name
   * @param tenantId  tenant ID to associate with
   * @param createdBy creator identifier (admin username or email)
   * @return created organization entity
   */
  @CacheEvict(value = "organizations", allEntries = true)
  public Organization createDefaultOrganization(String name, String tenantId, String createdBy) {
    logger.info("Creating default organization '{}' for tenant: {}", name, tenantId);

    // Validate tenant doesn't already have an organization
    if (organizationRepository.existsByTenantId(tenantId)) {
      throw new OrganizationManagementException("Organization already exists for tenant: " + tenantId);
    }

    var organization = new Organization(name, tenantId);
    organization.setEnabled(true);
    organization.setSubscriptionStatus("trial");
    organization.setSubscriptionPlan("basic");
    organization.setMaxUsers(10);
    organization.setCreatedBy(createdBy);

    var savedOrganization = organizationRepository.save(organization);

    logger.info("Created default organization: {} (ID: {}) for tenant: {}",
        savedOrganization.getName(), savedOrganization.getId(), savedOrganization.getTenantId());

    return savedOrganization;
  }

  /**
   * Create a new organization (SUPER_ADMIN only).
   * This also establishes the 1:1 relationship with a tenant.
   */
  @CacheEvict(value = "organizations", allEntries = true)
  public OrganizationDto createOrganization(CreateOrganizationRequest request, UserContext currentUser) {
    validateSuperAdminAccess(currentUser, "CREATE_ORGANIZATION");

    // Validate tenant doesn't already have an organization
    if (organizationRepository.existsByTenantId(request.tenantId())) {
      throw new OrganizationManagementException("Organization already exists for tenant: " + request.tenantId());
    }

    var organization = new Organization(request.name(), request.tenantId());
    organization.setDescription(request.description());
    organization.setIndustry(request.industry());
    organization.setWebsite(request.website());
    organization.setPhone(request.phone());
    organization.setAddress(request.address());
    organization.setCity(request.city());
    organization.setCountry(request.country());
    organization.setEnabled(true);
    organization.setBillingEmail(request.billingEmail());
    organization.setPaymentGatewayAccountId(request.paymentGatewayAccountId());
    organization.setPaymentGatewayProvider(request.paymentGatewayProvider());
    organization.setSubscriptionStatus(request.subscriptionStatus());
    organization.setSubscriptionPlan(request.subscriptionPlan());
    organization.setMaxUsers(request.maxUsers());
    organization.setCreatedBy(currentUser.username());

    // Validate owner user exists in the tenant
    if (request.ownerUserId() != null) {
      validateOwnerUser(request.ownerUserId(), request.tenantId());
      organization.setOwnerUserId(request.ownerUserId());
    }

    var savedOrganization = organizationRepository.save(organization);

    logAuditEvent("CREATE_ORGANIZATION", "Created organization: " + savedOrganization.getName() + " for tenant: " + savedOrganization.getTenantId(), currentUser);

    return convertToDto(savedOrganization);
  }

  /**
   * Update organization details.
   * SUPER_ADMIN can update any, ADMIN can update only their own.
   */
  @Caching(evict = {
      @CacheEvict(value = "organizations", key = "#organizationId"),
      @CacheEvict(value = "organizations", allEntries = true)
  })
  public OrganizationDto updateOrganization(Long organizationId, UpdateOrganizationRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "UPDATE_ORGANIZATION");

    var organization = findOrganizationByIdWithTenantCheck(organizationId, currentUser);

    // Update basic fields
    if (request.name() != null) {
      organization.setName(request.name());
    }

    if (request.description() != null) {
      organization.setDescription(request.description());
    }

    if (request.industry() != null) {
      organization.setIndustry(request.industry());
    }

    if (request.website() != null) {
      organization.setWebsite(request.website());
    }

    if (request.phone() != null) {
      organization.setPhone(request.phone());
    }

    if (request.address() != null) {
      organization.setAddress(request.address());
    }

    if (request.city() != null) {
      organization.setCity(request.city());
    }

    if (request.country() != null) {
      organization.setCountry(request.country());
    }

    if (request.enabled() != null) {
      organization.setEnabled(request.enabled());
    }

    // Update billing fields
    if (request.billingEmail() != null) {
      organization.setBillingEmail(request.billingEmail());
    }

    if (request.paymentGatewayAccountId() != null) {
      organization.setPaymentGatewayAccountId(request.paymentGatewayAccountId());
    }

    if (request.paymentGatewayProvider() != null) {
      organization.setPaymentGatewayProvider(request.paymentGatewayProvider());
    }

    if (request.chargesEnabled() != null) {
      organization.setChargesEnabled(request.chargesEnabled());
    }

    if (request.payoutsEnabled() != null) {
      organization.setPayoutsEnabled(request.payoutsEnabled());
    }

    if (request.subscriptionStatus() != null) {
      organization.setSubscriptionStatus(request.subscriptionStatus());
    }

    if (request.subscriptionPlan() != null) {
      organization.setSubscriptionPlan(request.subscriptionPlan());
    }

    if (request.maxUsers() != null) {
      organization.setMaxUsers(request.maxUsers());
    }

    var updatedOrganization = organizationRepository.save(organization);

    logAuditEvent("UPDATE_ORGANIZATION", "Updated organization: " + updatedOrganization.getName(), currentUser);

    return convertToDto(updatedOrganization);
  }

  /**
   * Delete organization (SUPER_ADMIN only).
   * This will cascade delete the tenant and all associated data.
   */
  @Caching(evict = {
      @CacheEvict(value = "organizations", key = "#organizationId"),
      @CacheEvict(value = "organizations", allEntries = true)
  })
  public void deleteOrganization(Long organizationId, UserContext currentUser) {
    validateSuperAdminAccess(currentUser, "DELETE_ORGANIZATION");

    var organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new OrganizationManagementException("Organization not found: " + organizationId));

    organizationRepository.delete(organization);

    logAuditEvent("DELETE_ORGANIZATION", "Deleted organization: " + organization.getName() + " (tenant: " + organization.getTenantId() + ")", currentUser);
  }

  private void validateAdminAccess(UserContext currentUser, String operation) {
    if (!currentUser.hasAuthority("ADMIN") && !currentUser.hasAuthority("SUPER_ADMIN")) {
      throw new AccessDeniedException("Insufficient permissions for operation: " + operation);
    }
  }

  private void validateSuperAdminAccess(UserContext currentUser, String operation) {
    if (!currentUser.hasAuthority("SUPER_ADMIN")) {
      throw new AccessDeniedException("SUPER_ADMIN role required for operation: " + operation);
    }
  }

  private Organization findOrganizationByIdWithTenantCheck(Long organizationId, UserContext currentUser) {
    var organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new OrganizationManagementException("Organization not found: " + organizationId));

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

  private void validateOwnerUser(Long userId, String tenantId) {
    // Execute in tenant context to check if user exists
    var userExists = TenantContext.executeInTenantContext(tenantId, () ->
        userRepository.existsById(userId)
    );

    if (!userExists) {
      throw new OrganizationManagementException("Owner user not found: " + userId + " in tenant: " + tenantId);
    }
  }

  private OrganizationDto convertToDto(Organization organization) {
    return new OrganizationDto(
        organization.getId(),
        organization.getName(),
        organization.getDescription(),
        organization.getIndustry(),
        organization.getWebsite(),
        organization.getPhone(),
        organization.getAddress(),
        organization.getCity(),
        organization.getCountry(),
        organization.getEnabled(),
        organization.getTenantId(),
        organization.getOwnerUserId(),
        organization.getBillingEmail(),
        organization.getPaymentGatewayAccountId(),
        organization.getPaymentGatewayProvider(),
        organization.getChargesEnabled(),
        organization.getPayoutsEnabled(),
        organization.getSubscriptionStatus(),
        organization.getSubscriptionPlan(),
        organization.getMaxUsers()
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

      logger.info("Organization management audit: {} - {} by user {} in tenant {}",
          action, details, currentUser.username(), currentUser.tenantId());
    } catch (final Exception e) {
      logger.error("Failed to log audit event: {}", e.getMessage(), e);
    }
  }

  public static class OrganizationManagementException extends RuntimeException {

    public OrganizationManagementException(final String message) {
      super(message);
    }

    public OrganizationManagementException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
