package org.gripday.authservice.domain.service;

import org.gripday.authservice.infrastructure.entity.Organization;
import org.gripday.authservice.infrastructure.entity.UserAuditLog;
import org.gripday.authservice.infrastructure.repository.OrganizationRepository;
import org.gripday.authservice.infrastructure.repository.UserAuditLogRepository;
import org.gripday.authservice.infrastructure.repository.UserRepository;
import org.gripday.authservice.presentation.dto.CreateOrganizationRequest;
import org.gripday.authservice.presentation.dto.OrganizationDto;
import org.gripday.authservice.presentation.dto.UpdateOrganizationRequest;
import org.gripday.authservice.presentation.dto.UserContext;
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
 * Service for organization management operations with admin-only access and tenant isolation.
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

  @Transactional(readOnly = true)
  public Page<OrganizationDto> getAllOrganizations(Pageable pageable, UserContext currentUser) {
    validateAdminAccess(currentUser, "LIST_ORGANIZATIONS");

    var tenantId = currentUser.tenantId();
    var organizations = organizationRepository.findByTenantId(tenantId);

    var organizationDtos = organizations.stream()
        .map(this::convertToDto)
        .toList();

    var start = (int) pageable.getOffset();
    var end = Math.min((start + pageable.getPageSize()), organizationDtos.size());
    var pageContent = organizationDtos.subList(start, end);

    logAuditEvent("LIST_ORGANIZATIONS", "Listed " + pageContent.size() + " organizations", currentUser);

    return new PageImpl<>(pageContent, pageable, organizationDtos.size());
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "organizations", key = "#organizationId + '_' + #currentUser.tenantId()",
      condition = "#currentUser != null && #currentUser.tenantId() != null")
  public OrganizationDto getOrganizationById(Long organizationId, UserContext currentUser) {
    validateAdminAccess(currentUser, "GET_ORGANIZATION");

    var organization = findOrganizationByIdWithTenantCheck(organizationId, currentUser.tenantId());

    logAuditEvent("GET_ORGANIZATION", "Retrieved organization: " + organization.getName(), currentUser);

    return convertToDto(organization);
  }

  @CacheEvict(value = "organizations", allEntries = true, condition = "#currentUser != null && #currentUser.tenantId() != null")
  public OrganizationDto createOrganization(CreateOrganizationRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "CREATE_ORGANIZATION");

    var tenantId = currentUser.tenantId();

    if (organizationRepository.existsByName(request.name())) {
      throw new OrganizationManagementException("Organization name already exists: " + request.name());
    }

    var organization = new Organization(request.name(), tenantId);
    organization.setDescription(request.description());
    organization.setIndustry(request.industry());
    organization.setWebsite(request.website());
    organization.setPhone(request.phone());
    organization.setAddress(request.address());
    organization.setCity(request.city());
    organization.setCountry(request.country());
    organization.setEnabled(request.enabled() != null ? request.enabled() : true);

    if (request.ownerId() != null) {
      var owner = userRepository.findById(request.ownerId())
          .orElseThrow(() -> new OrganizationManagementException("Owner user not found: " + request.ownerId()));

      if (!owner.getTenantId().equals(tenantId)) {
        throw new AccessDeniedException("Owner user not found in current tenant");
      }

      owner.setOrganization(organization);
      organization.setOwner(owner);
    }

    var savedOrganization = organizationRepository.save(organization);

    logAuditEvent("CREATE_ORGANIZATION", "Created organization: " + savedOrganization.getName(), currentUser);

    return convertToDto(savedOrganization);
  }

  @Caching(evict = {
      @CacheEvict(value = "organizations", key = "#organizationId + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "organizations", allEntries = true)
  })
  public OrganizationDto updateOrganization(Long organizationId, UpdateOrganizationRequest request, UserContext currentUser) {
    validateAdminAccess(currentUser, "UPDATE_ORGANIZATION");

    var organization = findOrganizationByIdWithTenantCheck(organizationId, currentUser.tenantId());

    if (request.name() != null && !request.name().equals(organization.getName())) {
      if (organizationRepository.existsByName(request.name())) {
        throw new OrganizationManagementException("Organization name already exists: " + request.name());
      }
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

    if (request.ownerId() != null) {
      var owner = userRepository.findById(request.ownerId())
          .orElseThrow(() -> new OrganizationManagementException("Owner user not found: " + request.ownerId()));

      if (!owner.getTenantId().equals(currentUser.tenantId())) {
        throw new AccessDeniedException("Owner user not found in current tenant");
      }

      if (organization.getOwner() != null) {
        organization.getOwner().setOrganization(null);
      }

      owner.setOrganization(organization);
      organization.setOwner(owner);
    }

    var updatedOrganization = organizationRepository.save(organization);

    logAuditEvent("UPDATE_ORGANIZATION", "Updated organization: " + updatedOrganization.getName(), currentUser);

    return convertToDto(updatedOrganization);
  }

  @Caching(evict = {
      @CacheEvict(value = "organizations", key = "#organizationId + '_' + #currentUser.tenantId()"),
      @CacheEvict(value = "organizations", allEntries = true)
  })
  public void deleteOrganization(Long organizationId, UserContext currentUser) {
    validateAdminAccess(currentUser, "DELETE_ORGANIZATION");

    var organization = findOrganizationByIdWithTenantCheck(organizationId, currentUser.tenantId());

    if (organization.getOwner() != null) {
      organization.getOwner().setOrganization(null);
    }

    organizationRepository.delete(organization);

    logAuditEvent("DELETE_ORGANIZATION", "Deleted organization: " + organization.getName(), currentUser);
  }

  private void validateAdminAccess(UserContext currentUser, String operation) {
    if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("SUPER_ADMIN")) {
      throw new AccessDeniedException("Insufficient permissions for operation: " + operation);
    }
  }

  private Organization findOrganizationByIdWithTenantCheck(Long organizationId, String tenantId) {
    var organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new OrganizationManagementException("Organization not found: " + organizationId));

    if (!organization.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Organization not found in current tenant");
    }

    return organization;
  }

  private OrganizationDto convertToDto(Organization organization) {
    var owner = organization.getOwner();
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
        owner != null ? owner.getId() : null,
        owner != null ? owner.getUsername() : null,
        organization.getTenantId(),
        organization.getCreatedAt(),
        organization.getUpdatedAt()
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

      auditLogRepository.save(auditLog);

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
