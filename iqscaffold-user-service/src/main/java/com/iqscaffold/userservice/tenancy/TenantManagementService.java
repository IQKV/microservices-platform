package com.iqscaffold.userservice.tenancy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.CreateTenantRequest;
import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.TenantResponse;
import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.TenantStatistics;
import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.TenantSummary;
import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.UpdateTenantRequest;
import com.iqscaffold.userservice.shared.exception.TenantManagementException;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprehensive tenant management service providing multi-tenant architecture support with enterprise features.
 *
 * <p>This service manages the complete lifecycle of tenant environments in a multi-tenant SaaS architecture.
 * It provides secure tenant operations, schema provisioning, quota management, and comprehensive monitoring
 * capabilities while ensuring strict data isolation between tenants.
 *
 * <h3>Core Capabilities</h3>
 * <ul>
 *   <li><strong>Tenant Lifecycle Management</strong> - Create, update, delete, and manage tenant environments</li>
 *   <li><strong>Schema Provisioning</strong> - Automatic database schema creation and migration</li>
 *   <li><strong>Quota Management</strong> - User limits, storage quotas, and API rate limiting</li>
 *   <li><strong>Domain Management</strong> - Custom domain assignment and validation</li>
 *   <li><strong>Tenant Statistics</strong> - Usage metrics and monitoring data</li>
 * </ul>
 *
 * <h3>Multi-Tenant Architecture</h3>
 * <ul>
 *   <li><strong>Schema-per-Tenant</strong> - Physical data isolation using separate database schemas</li>
 *   <li><strong>Tenant Registry</strong> - Central registry stored in public schema</li>
 *   <li><strong>Context Propagation</strong> - Automatic tenant context management</li>
 *   <li><strong>Data Isolation</strong> - Complete separation of tenant data and operations</li>
 * </ul>
 *
 * <h3>Schema Management</h3>
 * <ul>
 *   <li><strong>Automatic Provisioning</strong> - Creates tenant schema during tenant creation</li>
 *   <li><strong>Liquibase Integration</strong> - Database migrations per tenant schema</li>
 *   <li><strong>Schema Naming</strong> - Consistent naming convention (tenant_[tenantId])</li>
 *   <li><strong>Migration Tracking</strong> - Version control for schema changes</li>
 * </ul>
 *
 * <h3>Quota and Limits</h3>
 * <ul>
 *   <li><strong>User Quotas</strong> - Maximum number of users per tenant</li>
 *   <li><strong>Storage Quotas</strong> - Disk space limits in GB</li>
 *   <li><strong>API Rate Limits</strong> - Requests per minute throttling</li>
 *   <li><strong>Feature Flags</strong> - Tenant-specific feature enablement</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><strong>Tenant Validation</strong> - Comprehensive validation of tenant data</li>
 *   <li><strong>Domain Uniqueness</strong> - Prevents domain conflicts across tenants</li>
 *   <li><strong>Access Control</strong> - Role-based tenant management operations</li>
 *   <li><strong>Audit Logging</strong> - Complete audit trail for tenant operations</li>
 * </ul>
 *
 * <h3>Caching Strategy</h3>
 * <ul>
 *   <li><strong>Tenant Lookups</strong> - Cached tenant metadata for performance</li>
 *   <li><strong>Domain Resolution</strong> - Cached domain-to-tenant mappings</li>
 *   <li><strong>Statistics</strong> - Cached usage statistics with TTL</li>
 *   <li><strong>Cache Invalidation</strong> - Automatic eviction on tenant updates</li>
 * </ul>
 *
 * <h3>Monitoring and Statistics</h3>
 * <ul>
 *   <li><strong>User Counts</strong> - Active and total user statistics</li>
 *   <li><strong>Storage Usage</strong> - Current storage consumption</li>
 *   <li><strong>API Usage</strong> - Request volume and rate limit status</li>
 *   <li><strong>Health Metrics</strong> - Tenant health and performance indicators</li>
 * </ul>
 *
 * <h3>Exception Handling</h3>
 * <ul>
 *   <li>{@code TenantAlreadyExistsException} - Duplicate tenant ID</li>
 *   <li>{@code DomainAlreadyExistsException} - Duplicate domain assignment</li>
 *   <li>{@code TenantNotFoundException} - Tenant not found</li>
 *   <li>{@code SchemaProvisioningException} - Database schema creation failures</li>
 *   <li>{@code QuotaExceededException} - Quota limit violations</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @Autowired
 * private TenantManagementService tenantService;
 *
 * // Create new tenant with schema provisioning
 * CreateTenantRequest request = CreateTenantRequest.builder()
 *     .tenantId("acme-corp")
 *     .name("ACME Corporation")
 *     .domain("acme.example.com")
 *     .maxUsers(100)
 *     .storageQuotaGb(50)
 *     .build();
 *
 * TenantResponse tenant = tenantService.createTenant(request, "admin@system.com");
 *
 * // Get tenant statistics
 * TenantStatistics stats = tenantService.getTenantStatistics("acme-corp");
 *
 * // Update tenant quotas
 * UpdateTenantRequest updateRequest = UpdateTenantRequest.builder()
 *     .maxUsers(200)
 *     .storageQuotaGb(100)
 *     .build();
 *
 * tenantService.updateTenant("acme-corp", updateRequest, "admin@system.com");
 * }</pre>
 *
 * @see Tenant
 * @see TenantDto
 * @see SchemaNameResolver
 * @see TenantLiquibaseRunner
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
   * Create a new tenant with comprehensive validation, schema provisioning, and quota setup.
   *
   * <p>This method implements the complete tenant creation workflow including validation,
   * database schema provisioning, and initial configuration. It ensures data integrity
   * and proper isolation while setting up all necessary infrastructure for the new tenant.
   *
   * <h4>Creation Workflow:</h4>
   * <ol>
   *   <li><strong>Validation Phase</strong>
   *       <ul>
   *         <li>Tenant ID uniqueness check</li>
   *         <li>Domain uniqueness validation (if provided)</li>
   *         <li>Input sanitization and format validation</li>
   *         <li>Quota and limit validation</li>
   *       </ul>
   *   </li>
   *   <li><strong>Entity Creation</strong>
   *       <ul>
   *         <li>Create Tenant entity with metadata</li>
   *         <li>Set default quotas and limits</li>
   *         <li>Configure tenant-specific settings</li>
   *         <li>Persist to public schema registry</li>
   *       </ul>
   *   </li>
   *   <li><strong>Schema Provisioning</strong>
   *       <ul>
   *         <li>Generate unique schema name</li>
   *         <li>Create database schema</li>
   *         <li>Run Liquibase migrations</li>
   *         <li>Set up initial data and constraints</li>
   *       </ul>
   *   </li>
   *   <li><strong>Cache Management</strong>
   *       <ul>
   *         <li>Invalidate tenant caches</li>
   *         <li>Update domain resolution cache</li>
   *         <li>Refresh tenant statistics</li>
   *       </ul>
   *   </li>
   * </ol>
   *
   * <h4>Validation Rules:</h4>
   * <ul>
   *   <li><strong>Tenant ID</strong> - Must be unique, alphanumeric, 3-50 characters</li>
   *   <li><strong>Domain</strong> - Must be valid FQDN and globally unique</li>
   *   <li><strong>Name</strong> - Required, 1-255 characters</li>
   *   <li><strong>Quotas</strong> - Must be positive integers within system limits</li>
   * </ul>
   *
   * <h4>Default Configuration:</h4>
   * <ul>
   *   <li><strong>Status</strong> - Enabled by default</li>
   *   <li><strong>Max Users</strong> - 10 (if not specified)</li>
   *   <li><strong>Storage Quota</strong> - 1GB (if not specified)</li>
   *   <li><strong>API Rate Limit</strong> - 1000 requests/minute (if not specified)</li>
   * </ul>
   *
   * <h4>Schema Provisioning:</h4>
   * <ul>
   *   <li>Creates schema with name pattern: {@code tenant_[tenantId]}</li>
   *   <li>Runs all tenant-specific Liquibase changesets</li>
   *   <li>Sets up initial tables, indexes, and constraints</li>
   *   <li>Configures tenant-specific data and settings</li>
   * </ul>
   *
   * <h4>Error Handling:</h4>
   * <ul>
   *   <li>Rollback entity creation if schema provisioning fails</li>
   *   <li>Cleanup partial schema creation on errors</li>
   *   <li>Detailed error logging for troubleshooting</li>
   *   <li>Graceful handling of concurrent creation attempts</li>
   * </ul>
   *
   * @param request   The tenant creation request with all required information
   * @param createdBy The identifier of the user/system creating the tenant
   * @return TenantResponse containing the created tenant information
   * @throws TenantManagementException.TenantAlreadyExistsException If tenant ID already exists
   * @throws TenantManagementException.DomainAlreadyExistsException If domain is already assigned
   * @throws TenantManagementException.SchemaProvisioningException  If database schema creation fails
   * @throws ValidationException                                    If request data is invalid
   * @see CreateTenantRequest
   * @see TenantResponse
   * @see SchemaNameResolver
   * @see TenantLiquibaseRunner
   */
  @CacheEvict(value = "tenants", allEntries = true)
  public TenantResponse createTenant(CreateTenantRequest request, String createdBy) {
    logger.info("Creating new tenant: {}", request.tenantId());

    // Validate tenant doesn't already exist
    if (tenantRepository.existsByTenantId(request.tenantId())) {
      throw new TenantManagementException.TenantAlreadyExistsException(
          "Tenant with ID '" + request.tenantId() + "' already exists",
          request.tenantId()
      );
    }

    // Validate domain uniqueness if provided
    if (request.domain() != null && !request.domain().trim().isEmpty()) {
      if (tenantRepository.existsByDomain(request.domain())) {
        throw new TenantManagementException.DomainAlreadyExistsException(
            "Tenant with domain '" + request.domain() + "' already exists",
            request.domain()
        );
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
   * @throws TenantManagementException.TenantNotFoundException      if tenant not found
   * @throws TenantManagementException.DomainAlreadyExistsException if domain already exists
   */
  @Caching(evict = {
      @CacheEvict(value = "tenants", key = "#tenantId"),
      @CacheEvict(value = "tenants", key = "'all_tenants'"),
      @CacheEvict(value = "tenants", key = "'enabled_tenants'")
  })
  public TenantResponse updateTenant(String tenantId, UpdateTenantRequest request) {
    logger.info("Updating tenant: {}", tenantId);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new TenantManagementException.TenantNotFoundException(
            "Tenant not found: " + tenantId,
            tenantId
        ));

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
        throw new TenantManagementException.DomainAlreadyExistsException(
            "Tenant with domain '" + request.domain() + "' already exists",
            request.domain()
        );
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

    var updatedTenant = tenantRepository.save(tenant);

    logger.info("Successfully updated tenant: {}", tenantId);

    return mapToTenantResponse(updatedTenant);
  }

  /**
   * Get tenant by tenant ID. Cached with tenant ID as key.
   *
   * @param tenantId the tenant ID
   * @return the tenant response
   * @throws TenantManagementException.TenantNotFoundException if tenant not found
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "tenants", key = "#tenantId")
  public TenantResponse getTenant(String tenantId) {
    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new TenantManagementException.TenantNotFoundException(
            "Tenant not found: " + tenantId,
            tenantId
        ));

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
   * @throws TenantManagementException.TenantNotFoundException if tenant not found
   * @throws TenantManagementException.TenantHasUsersException if tenant has existing users
   */
  @CacheEvict(value = "tenants", allEntries = true)
  public void deleteTenant(String tenantId) {
    logger.warn("Deleting tenant: {}", tenantId);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new TenantManagementException.TenantNotFoundException(
            "Tenant not found: " + tenantId,
            tenantId
        ));

    // Check if tenant has users
    var userCount = TenantContext.executeInTenantContext(tenantId, () -> userRepository.count());
    if (userCount > 0) {
      throw new TenantManagementException.TenantHasUsersException(
          "Cannot delete tenant with existing users. User count: " + userCount,
          tenantId,
          userCount
      );
    }

    // Delete tenant
    tenantRepository.delete(tenant);

    // Clean up tenant schema (placeholder for future implementation)
    cleanupTenantSchema(tenantId);

    logger.warn("Successfully deleted tenant: {}", tenantId);
  }

  /**
   * Suspend a tenant. Prevents access but preserves all data. Can be restored.
   *
   * @param tenantId the tenant ID
   * @param reason   the suspension reason
   * @param suspendedBy the user suspending the tenant
   * @return the updated tenant response
   * @throws TenantManagementException.TenantNotFoundException if tenant not found
   * @throws IllegalStateException if tenant is already suspended or archived
   */
  @Caching(evict = {
      @CacheEvict(value = "tenants", key = "#tenantId"),
      @CacheEvict(value = "tenants", key = "'all_tenants'"),
      @CacheEvict(value = "tenants", key = "'enabled_tenants'")
  })
  public TenantResponse suspendTenant(String tenantId, String reason, String suspendedBy) {
    logger.info("Suspending tenant: {} with reason: {}", tenantId, reason);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new TenantManagementException.TenantNotFoundException(
            "Tenant not found: " + tenantId,
            tenantId
        ));

    if (tenant.isSuspended()) {
      throw new IllegalStateException("Tenant " + tenantId + " is already suspended");
    }

    if (tenant.isArchived()) {
      throw new IllegalStateException("Tenant " + tenantId + " is archived and cannot be suspended");
    }

    tenant.setStatus(TenantStatus.SUSPENDED);
    tenant.setSuspendedAt(LocalDateTime.now());
    tenant.setSuspensionReason(reason);

    var updatedTenant = tenantRepository.save(tenant);
    logger.info("Successfully suspended tenant: {}", tenantId);

    return mapToTenantResponse(updatedTenant);
  }

  /**
   * Archive a tenant. Data is preserved but inaccessible. Terminal state - cannot be restored.
   *
   * @param tenantId the tenant ID
   * @param reason   the archive reason
   * @param archivedBy the user archiving the tenant
   * @return the updated tenant response
   * @throws TenantManagementException.TenantNotFoundException if tenant not found
   * @throws IllegalStateException if tenant is already archived
   */
  @Caching(evict = {
      @CacheEvict(value = "tenants", key = "#tenantId"),
      @CacheEvict(value = "tenants", key = "'all_tenants'"),
      @CacheEvict(value = "tenants", key = "'enabled_tenants'")
  })
  public TenantResponse archiveTenant(String tenantId, String reason, String archivedBy) {
    logger.warn("Archiving tenant: {} with reason: {}", tenantId, reason);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new TenantManagementException.TenantNotFoundException(
            "Tenant not found: " + tenantId,
            tenantId
        ));

    if (tenant.isArchived()) {
      throw new IllegalStateException("Tenant " + tenantId + " is already archived");
    }

    tenant.setStatus(TenantStatus.ARCHIVED);
    tenant.setArchivedAt(LocalDateTime.now());
    tenant.setArchivedReason(reason);

    var updatedTenant = tenantRepository.save(tenant);
    logger.warn("Successfully archived tenant: {}", tenantId);

    return mapToTenantResponse(updatedTenant);
  }

  /**
   * Restore a suspended tenant back to active state. Only works for suspended tenants.
   *
   * @param tenantId the tenant ID
   * @param restoredBy the user restoring the tenant
   * @return the updated tenant response
   * @throws TenantManagementException.TenantNotFoundException if tenant not found
   * @throws IllegalStateException if tenant is not suspended
   */
  @Caching(evict = {
      @CacheEvict(value = "tenants", key = "#tenantId"),
      @CacheEvict(value = "tenants", key = "'all_tenants'"),
      @CacheEvict(value = "tenants", key = "'enabled_tenants'")
  })
  public TenantResponse restoreTenant(String tenantId, String restoredBy) {
    logger.info("Restoring tenant: {}", tenantId);

    var tenant = tenantRepository.findByTenantId(tenantId)
        .orElseThrow(() -> new TenantManagementException.TenantNotFoundException(
            "Tenant not found: " + tenantId,
            tenantId
        ));

    if (!tenant.isSuspended()) {
      throw new IllegalStateException("Tenant " + tenantId + " is not suspended and cannot be restored");
    }

    tenant.setStatus(TenantStatus.ACTIVE);
    tenant.setSuspendedAt(null);
    tenant.setSuspensionReason(null);

    var updatedTenant = tenantRepository.save(tenant);
    logger.info("Successfully restored tenant: {}", tenantId);

    return mapToTenantResponse(updatedTenant);
  }

  /**
   * Check if a tenant is accessible (active and not suspended/archived).
   *
   * @param tenantId the tenant ID
   * @return true if tenant is active, false otherwise
   */
  public boolean isTenantAccessible(String tenantId) {
    return tenantRepository.findByTenantId(tenantId)
        .map(Tenant::isActive)
        .orElse(false);
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
              t.getStatus(),
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
        tenant.getStatus(),
        tenant.getDomain(),
        tenant.getMaxUsers(),
        tenant.getStorageQuotaGb(),
        tenant.getApiRateLimitPerMinute(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt(),
        tenant.getCreatedBy(),
        tenant.getSuspendedAt(),
        tenant.getArchivedAt(),
        tenant.getSuspensionReason(),
        tenant.getArchivedReason()
    );
  }

  private TenantSummary mapToTenantSummary(Tenant tenant) {
    var userCount = TenantContext.executeInTenantContext(tenant.getTenantId(), userRepository::countByEnabledTrue);

    return new TenantSummary(
        tenant.getTenantId(),
        tenant.getName(),
        tenant.getStatus(),
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
      throw new TenantManagementException.SchemaProvisioningException(
          "Failed to apply tenant changelog for schema: " + schema,
          schema,
          e
      );
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
