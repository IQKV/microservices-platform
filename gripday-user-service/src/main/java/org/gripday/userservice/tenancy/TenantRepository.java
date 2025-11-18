package org.gripday.userservice.tenancy;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
   * Find tenant by subdomain.
   *
   * @param subdomain the subdomain to search for
   * @return Optional containing the tenant if found
   */
  Optional<Tenant> findBySubdomain(String subdomain);

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
   * Check if tenant exists by subdomain.
   *
   * @param subdomain the subdomain to check
   * @return true if tenant exists, false otherwise
   */
  boolean existsBySubdomain(String subdomain);

  /**
   * Find all enabled tenants.
   *
   * @return list of enabled tenants
   */
  List<Tenant> findByEnabledTrue();

  /**
   * Find all disabled tenants.
   *
   * @return list of disabled tenants
   */
  List<Tenant> findByEnabledFalse();

  /**
   * Find tenants created by a specific user.
   *
   * @param createdBy the user who created the tenants
   * @return list of tenants created by the user
   */
  List<Tenant> findByCreatedBy(String createdBy);

  /**
   * Find tenants with user count within specified limits using custom query. This query joins with users table to count users per tenant.
   *
   * @param minUsers minimum user count
   * @param maxUsers maximum user count
   * @return list of tenants within user count limits
   */
  @Query("""
      SELECT t FROM Tenant t 
      WHERE t.enabled = true 
      AND (
          SELECT COUNT(u) FROM User u 
          WHERE u.tenantId = t.tenantId 
          AND u.enabled = true
      ) BETWEEN :minUsers AND :maxUsers
      ORDER BY t.createdAt DESC
      """)
  List<Tenant> findTenantsWithUserCountBetween(@Param("minUsers") long minUsers,
      @Param("maxUsers") long maxUsers);

  /**
   * Find tenants that have exceeded their user quota using custom query.
   *
   * @return list of tenants that have exceeded user quota
   */
  @Query("""
      SELECT t FROM Tenant t 
      WHERE t.enabled = true 
      AND t.maxUsers IS NOT NULL 
      AND (
          SELECT COUNT(u) FROM User u 
          WHERE u.tenantId = t.tenantId 
          AND u.enabled = true
      ) > t.maxUsers
      ORDER BY t.createdAt DESC
      """)
  List<Tenant> findTenantsExceedingUserQuota();

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

  /**
   * Get tenant statistics using custom query with text blocks. Returns tenant ID, name, user count, and enabled status.
   *
   * @return list of tenant statistics
   */
  @Query("""
      SELECT t.tenantId, t.name, t.enabled, 
             COALESCE(COUNT(u), 0) as userCount,
             t.maxUsers, t.createdAt
      FROM Tenant t 
      LEFT JOIN User u ON u.tenantId = t.tenantId AND u.enabled = true
      GROUP BY t.id, t.tenantId, t.name, t.enabled, t.maxUsers, t.createdAt
      ORDER BY t.createdAt DESC
      """)
  List<Object[]> getTenantStatistics();
}
