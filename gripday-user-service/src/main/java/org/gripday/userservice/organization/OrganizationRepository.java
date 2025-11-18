package org.gripday.userservice.organization;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Organization entity operations.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByName(String name);

  List<Organization> findByTenantId(String tenantId);

  @Query("""
      SELECT o FROM Organization o 
      WHERE o.tenantId = :tenantId 
        AND o.enabled = true
      ORDER BY o.createdAt DESC
      """)
  List<Organization> findEnabledByTenantId(@Param("tenantId") String tenantId);

  boolean existsByName(String name);

  long countByTenantId(String tenantId);

  @Query("""
      SELECT COUNT(o) FROM Organization o 
      WHERE o.tenantId = :tenantId 
        AND o.enabled = true
      """)
  long countEnabledByTenantId(@Param("tenantId") String tenantId);
}
