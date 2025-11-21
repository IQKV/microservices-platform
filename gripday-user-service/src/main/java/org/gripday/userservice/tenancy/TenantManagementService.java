package org.gripday.userservice.tenancy;

import java.util.List;
import java.util.Optional;

import org.gripday.userservice.infrastructure.repository.dto.TenantDto.CreateTenantRequest;
import org.gripday.userservice.infrastructure.repository.dto.TenantDto.TenantResponse;
import org.gripday.userservice.infrastructure.repository.dto.TenantDto.TenantStatistics;
import org.gripday.userservice.infrastructure.repository.dto.TenantDto.TenantSummary;
import org.gripday.userservice.infrastructure.repository.dto.TenantDto.UpdateTenantRequest;
import org.gripday.userservice.usermanagement.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for tenant management operations including CRUD operations and schema provisioning. Handles multi-tenant architecture support with tenant lifecycle management.
 */
@Service
@Transactional
public class TenantManagementService {

  private static final Logger logger = LoggerFactory.getLogger(TenantManagementService.class);

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final SchemaNameResolver schemaNameResolver;
  private final TenantLiquibaseRunner liquibaseRunner;
  private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  public TenantManagementService(
      final TenantRepository tenantRepository,
      final UserRepository userRepository,
      final SchemaNameResolver schemaNameResolver,
      final TenantLiquibaseRunner liquibaseRunner,
      final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.schemaNameResolver = schemaNameResolver;
    this.liquibaseRunner = liquibaseRunner;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Create a new tenant with validation and schema provisioning. Evicts tenant caches to ensure fresh data.
   *
   * @param request   the tenant creation request
   * @param createdBy the user creating the tenant
   * @return the created tenant response
   * @throws IllegalArgumentException if tenant already exists or validation fails
   */
  @CacheEvict(value = "tenants", allEntries = true)
  public TenantResponse createTenant(CreateTenantRequest request, String createdBy) {
    logger.info("Creating new tenant: {}", request.tenantId());

    // Validate tenant doesn't already exist
    if (tenantRepository.existsByTenantId(request.tenantId())) {
      throw new IllegalArgumentException("Tenant with ID '" + request.tenantId() + "' already exists");
    }

    // Validate domain uniqueness if provided
    if (request.domain() != null && !request.domain().trim().isEmpty()) {
      if (tenantRepository.existsByDomain(request.domain())) {
        throw new IllegalArgumentException("Tenant with domain '" + request.domain() + "' already exists");
      }
    }

    // Create tenant entity
    var tenant = new Tenant(request.tenantId(), request.name(), request.description(), createdBy);
    tenant.setDomain(request.domain());
    tenant.setMaxUsers(request.maxUsers());
    tenant.setStorageQuotaGb(request.storageQuotaGb());
    tenant.setApiRateLimitPerMinute(request.apiRateLimitPerMinute());

    // Save tenant
    var savedTenant = tenantRepository.save(tenant);

    // Provision tenant schema (placeholder for future implementation)
    provisionTenantSchema(savedTenant.getTenantId());

    logger.info("Successfully created tenant: {} with ID: {}", savedTenant.getName(), savedTenant.getTenantId());

    return mapToTenantResponse(savedTenant);
  }

  /**
   * Update an existing tenant. Evicts specific tenant cache entries.
   *
   * @param tenantId the tenant ID to update
   * @param request  the update request
   * @return the updated tenant response
   * @throws IllegalArgumentException if tenant not found or validation fails
   */
  @Caching(evict = {
      @CacheEvict(value = "tenants", key = "#tenantId"),
      @CacheEvict(value = "tenants", key = "'all_tenants'"),
      @CacheEvict(value = "tenants", key = "'enabled_tenants'")
  })
  public TenantResponse updateTenant(String tenantId, UpdateTenantRequest request) {
    logger.info("Updating tenant: {}", tenantId);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

    // Update fields if provided using pattern matching and modern syntax
    if (request.name() != null && !request.name().trim().isEmpty()) {
      tenant.setName(request.name().trim());
    }

    if (request.description() != null) {
      tenant.setDescription(request.description().trim().isEmpty() ? null : request.description().trim());
    }

    // Validate domain uniqueness if changing
    if (request.domain() != null && !request.domain().equals(tenant.getDomain())) {
      if (!request.domain().trim().isEmpty() && tenantRepository.existsByDomain(request.domain())) {
        throw new IllegalArgumentException("Tenant with domain '" + request.domain() + "' already exists");
      }
      tenant.setDomain(request.domain().trim().isEmpty() ? null : request.domain().trim());
    }

    if (request.maxUsers() != null) {
      tenant.setMaxUsers(request.maxUsers());
    }

    if (request.storageQuotaGb() != null) {
      tenant.setStorageQuotaGb(request.storageQuotaGb());
    }

    if (request.apiRateLimitPerMinute() != null) {
      tenant.setApiRateLimitPerMinute(request.apiRateLimitPerMinute());
    }

    if (request.enabled() != null) {
      tenant.setEnabled(request.enabled());
    }

    var updatedTenant = tenantRepository.save(tenant);

    logger.info("Successfully updated tenant: {}", tenantId);

    return mapToTenantResponse(updatedTenant);
  }

  /**
   * Get tenant by tenant ID. Cached with tenant ID as key.
   *
   * @param tenantId the tenant ID
   * @return the tenant response
   * @throws IllegalArgumentException if tenant not found
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "tenants", key = "#tenantId")
  public TenantResponse getTenant(String tenantId) {
    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

    return mapToTenantResponse(tenant);
  }

  /**
   * Get all tenants with optional filtering. Cached based on enabled filter.
   *
   * @param enabledOnly if true, return only enabled tenants
   * @return list of tenant summaries
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "tenants", key = "#enabledOnly ? 'enabled_tenants' : 'all_tenants'")
  public List<TenantSummary> getAllTenants(boolean enabledOnly) {
    var tenants = enabledOnly ? tenantRepository.findByEnabledTrue() : tenantRepository.findAll();

    return tenants.stream()
        .map(this::mapToTenantSummary)
        .toList();
  }

  /**
   * Delete a tenant and all associated data. Evicts all tenant-related cache entries.
   *
   * @param tenantId the tenant ID to delete
   * @throws IllegalArgumentException if tenant not found
   */
  @CacheEvict(value = "tenants", allEntries = true)
  public void deleteTenant(String tenantId) {
    logger.warn("Deleting tenant: {}", tenantId);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

    // Check if tenant has users
    var userCount = TenantContext.executeInTenantContext(tenantId, () -> userRepository.count());
    if (userCount > 0) {
      throw new IllegalStateException("Cannot delete tenant with existing users. User count: " + userCount);
    }

    // Delete tenant
    tenantRepository.delete(tenant);

    // Clean up tenant schema (placeholder for future implementation)
    cleanupTenantSchema(tenantId);

    logger.warn("Successfully deleted tenant: {}", tenantId);
  }

  /**
   * Enable or disable a tenant.
   *
   * @param tenantId the tenant ID
   * @param enabled  the enabled status
   * @return the updated tenant response
   */
  public TenantResponse setTenantEnabled(String tenantId, boolean enabled) {
    logger.info("Setting tenant {} enabled status to: {}", tenantId, enabled);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

    tenant.setEnabled(enabled);
    var updatedTenant = tenantRepository.save(tenant);

    return mapToTenantResponse(updatedTenant);
  }

  /**
   * Get tenant statistics including user counts and quota utilization.
   *
   * @return list of tenant statistics
   */
  @Transactional(readOnly = true)
  public List<TenantStatistics> getTenantStatistics() {
    var previous = TenantContext.getCurrentTenantId();
    TenantContext.clear();
    var tenants = tenantRepository.findAll();
    if (previous != null) {
      TenantContext.setCurrentTenantId(previous);
    }

    return tenants.stream()
        .map(t -> {
          var count = countEnabledUsersForTenant(t.getTenantId());
          var utilization = TenantStatistics.calculateUtilization(count, t.getMaxUsers());
          return new TenantStatistics(
              t.getTenantId(),
              t.getName(),
              t.getEnabled(),
              count,
              t.getMaxUsers(),
              utilization,
              t.getCreatedAt()
          );
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Tenant> findTenantsWithUserCountBetween(long minUsers, long maxUsers) {
    var previous = TenantContext.getCurrentTenantId();
    TenantContext.clear();
    var tenants = tenantRepository.findByEnabledTrue();
    if (previous != null) {
      TenantContext.setCurrentTenantId(previous);
    }
    return tenants.stream()
        .filter(t -> {
          var count = countEnabledUsersForTenant(t.getTenantId());
          return count >= minUsers && count <= maxUsers;
        })
        .sorted(java.util.Comparator.comparing(Tenant::getCreatedAt).reversed())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Tenant> findTenantsExceedingUserQuota() {
    var previous = TenantContext.getCurrentTenantId();
    TenantContext.clear();
    var tenants = tenantRepository.findByEnabledTrue();
    if (previous != null) {
      TenantContext.setCurrentTenantId(previous);
    }
    return tenants.stream()
        .filter(t -> t.getMaxUsers() != null)
        .filter(t -> {
          var count = countEnabledUsersForTenant(t.getTenantId());
          return count > t.getMaxUsers();
        })
        .sorted(java.util.Comparator.comparing(Tenant::getCreatedAt).reversed())
        .toList();
  }

  private boolean isH2() {
    try {
      var ds = this.jdbcTemplate.getDataSource();
      if (ds == null) {
        return false;
      }
      try (var c = ds.getConnection()) {
        var name = c.getMetaData().getDatabaseProductName();
        return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("h2");
      }
    } catch (final Exception e) {
      return false;
    }
  }

  private long countEnabledUsersForTenant(String tenantId) {
    if (isH2()) {
      var schema = schemaNameResolver.toSchema(tenantId);
      var schemaUpper = schema.toUpperCase(java.util.Locale.ROOT);
      var existsSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'USERS'";
      var exists = this.jdbcTemplate.queryForObject(existsSql, Integer.class, schemaUpper);
      long schemaCount = 0L;
      if (exists != null && exists > 0) {
        var sqlSchema = "SELECT COUNT(*) FROM " + schemaUpper + ".users WHERE enabled = TRUE";
        Long resultSchema = this.jdbcTemplate.queryForObject(sqlSchema, Long.class);
        schemaCount = resultSchema != null ? resultSchema : 0L;
      }
      var sqlPublic = "SELECT COUNT(*) FROM PUBLIC.users WHERE enabled = ? AND tenant_id = ?";
      Long resultPublic = this.jdbcTemplate.queryForObject(sqlPublic, Long.class, true, tenantId);
      long publicCount = resultPublic != null ? resultPublic : 0L;
      return schemaCount + publicCount;
    }
    return TenantContext.executeInTenantContext(tenantId, userRepository::countByEnabledTrue);
  }

  /**
   * Find a tenant by domain for tenant resolution.
   *
   * @param domain the domain to search for
   * @return optional tenant
   */
  @Transactional(readOnly = true)
  public Optional<Tenant> findTenantByDomain(String domain) {
    if (domain == null || domain.trim().isEmpty()) {
      return Optional.empty();
    }

    return tenantRepository.findByDomain(domain);
  }

  /**
   * Validate tenant exists and is enabled. Cached for frequent tenant validation checks.
   *
   * @param tenantId the tenant ID to validate
   * @return true if tenant exists and is enabled
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "tenants", key = "'valid_' + #tenantId")
  public boolean isValidTenant(String tenantId) {
    return tenantRepository.findByTenantId(tenantId)
        .map(Tenant::isActive)
        .orElse(false);
  }

  // Private helper methods

  private TenantResponse mapToTenantResponse(Tenant tenant) {
    return new TenantResponse(
        tenant.getId(),
        tenant.getTenantId(),
        tenant.getName(),
        tenant.getDescription(),
        tenant.getEnabled(),
        tenant.getDomain(),
        tenant.getMaxUsers(),
        tenant.getStorageQuotaGb(),
        tenant.getApiRateLimitPerMinute(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt(),
        tenant.getCreatedBy()
    );
  }

  private TenantSummary mapToTenantSummary(Tenant tenant) {
    var userCount = TenantContext.executeInTenantContext(tenant.getTenantId(), userRepository::countByEnabledTrue);

    return new TenantSummary(
        tenant.getTenantId(),
        tenant.getName(),
        tenant.getEnabled(),
        userCount,
        tenant.getMaxUsers(),
        tenant.getCreatedAt()
    );
  }

  private void provisionTenantSchema(String tenantId) {
    var schema = schemaNameResolver.toSchema(tenantId);
    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
    try {
      liquibaseRunner.runTenantChangelog(schema);
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to apply tenant changelog for schema: " + schema, e);
    }
    logger.info("Provisioned schema {} for tenant: {}", schema, tenantId);
  }

  private void cleanupTenantSchema(String tenantId) {
    // Placeholder for tenant schema cleanup
    // This could include dropping tenant-specific database schemas,
    // cleaning up tenant-specific configurations, etc.
    logger.info("Cleaning up schema for tenant: {}", tenantId);
  }
}
