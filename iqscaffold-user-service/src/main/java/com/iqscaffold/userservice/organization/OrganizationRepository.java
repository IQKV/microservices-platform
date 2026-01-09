package com.iqscaffold.userservice.organization;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Organization entity operations.
 * Organizations are stored in PUBLIC schema (system-wide).
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByTenantId(String tenantId);

  Optional<Organization> findByName(String name);

  List<Organization> findByEnabledTrue();

  @Query("""
      SELECT o FROM Organization o 
      WHERE o.enabled = true
      ORDER BY o.createdAt DESC
      """)
  List<Organization> findEnabledOrderByCreatedAtDesc();

  boolean existsByName(String name);

  boolean existsByTenantId(String tenantId);

  long countByEnabledTrue();

  @Query("""
      SELECT o FROM Organization o 
      WHERE o.stripeAccountId = :stripeAccountId
      """)
  Optional<Organization> findByStripeAccountId(@Param("stripeAccountId") String stripeAccountId);

  @Query("""
      SELECT o FROM Organization o 
      WHERE o.subscriptionStatus = :status
      AND o.enabled = true
      ORDER BY o.createdAt DESC
      """)
  List<Organization> findBySubscriptionStatus(@Param("status") String status);
}
