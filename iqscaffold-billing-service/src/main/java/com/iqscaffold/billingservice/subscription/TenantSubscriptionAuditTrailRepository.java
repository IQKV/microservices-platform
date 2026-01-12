package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TenantSubscriptionAuditTrail} entities.
 */
@Repository
public interface TenantSubscriptionAuditTrailRepository extends JpaRepository<TenantSubscriptionAuditTrail, UUID> {

  /**
   * Find audit trail entries by tenant subscription ID.
   *
   * @param tenantSubscriptionId Tenant subscription ID
   * @param pageable             Pagination parameters
   * @return Page of audit trail entries
   */
  @Query("SELECT a FROM TenantSubscriptionAuditTrail a WHERE a.tenantSubscription.id = :tenantSubscriptionId ORDER BY a.createdAt DESC")
  Page<TenantSubscriptionAuditTrail> findByTenantSubscriptionId(@Param("tenantSubscriptionId") UUID tenantSubscriptionId, Pageable pageable);

  /**
   * Find audit trail entries by tenant ID.
   *
   * @param tenantId Tenant ID
   * @param pageable Pagination parameters
   * @return Page of audit trail entries
   */
  @Query("SELECT a FROM TenantSubscriptionAuditTrail a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
  Page<TenantSubscriptionAuditTrail> findByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

  /**
   * Find recent audit trail entries for a subscription.
   *
   * @param tenantSubscriptionId Tenant subscription ID
   * @param pageable             Pagination parameters for limiting results
   * @return List of recent audit trail entries
   */
  @Query(value = "SELECT a FROM TenantSubscriptionAuditTrail a WHERE a.tenantSubscription.id = :tenantSubscriptionId ORDER BY a.createdAt DESC")
  List<TenantSubscriptionAuditTrail> findRecentByTenantSubscriptionId(@Param("tenantSubscriptionId") UUID tenantSubscriptionId, Pageable pageable);
}
