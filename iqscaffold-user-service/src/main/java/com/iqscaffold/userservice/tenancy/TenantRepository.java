package com.iqscaffold.userservice.tenancy;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Tenant entity operations. Provides tenant management operations for multi-tenant architecture.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

  /**
   * Find tenant by tenant ID.
   *
   * @param tenantId the tenant ID to search for
   * @return Optional containing the tenant if found
   */
  Optional<Tenant> findByTenantId(String tenantId);

  /**
   * Find tenant by domain name.
   *
   * @param domain the domain name to search for
   * @return Optional containing the tenant if found
   */
  Optional<Tenant> findByDomain(String domain);

  /**
   * Check if tenant exists by tenant ID.
   *
   * @param tenantId the tenant ID to check
   * @return true if tenant exists, false otherwise
   */
  boolean existsByTenantId(String tenantId);

  /**
   * Check if tenant exists by domain.
   *
   * @param domain the domain to check
   * @return true if tenant exists, false otherwise
   */
  boolean existsByDomain(String domain);

  /**
   * Find all enabled tenants.
   *
   * @return list of enabled tenants
   */
  List<Tenant> findByEnabledTrue();

  /**
   * Find tenants by status.
   *
   * @param status the tenant status
   * @return list of tenants with the given status
   */
  List<Tenant> findByStatus(TenantStatus status);

  /**
   * Find tenants created by a specific user.
   *
   * @param createdBy the user who created the tenants
   * @return list of tenants created by the user
   */
  List<Tenant> findByCreatedBy(String createdBy);


  /**
   * Find tenants by name containing specified text (case-insensitive).
   *
   * @param name the name text to search for
   * @return list of tenants with matching names
   */
  List<Tenant> findByNameContainingIgnoreCase(String name);

  /**
   * Count total number of enabled tenants.
   *
   * @return count of enabled tenants
   */
  long countByEnabledTrue();

  /**
   * Count total number of disabled tenants.
   *
   * @return count of disabled tenants
   */
  long countByEnabledFalse();


}
