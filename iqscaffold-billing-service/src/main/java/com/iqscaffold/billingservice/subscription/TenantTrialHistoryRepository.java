package com.iqscaffold.billingservice.subscription;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for TenantTrialHistory entity.
 * 
 * <p>Provides methods to query and persist tenant trial history records.
 * Used to enforce the business rule that each tenant receives only one trial period.
 */
@Repository
public interface TenantTrialHistoryRepository extends JpaRepository<TenantTrialHistory, Long> {

  /**
   * Finds trial history for a tenant.
   * 
   * @param tenantId tenant identifier
   * @return optional containing the trial history, or empty if not found
   */
  @Query("""
      SELECT t FROM TenantTrialHistory t
      WHERE t.tenantId = :tenantId
      """)
  Optional<TenantTrialHistory> findByTenantId(@Param("tenantId") UUID tenantId);

  /**
   * Checks if a tenant has used their trial period.
   * 
   * @param tenantId tenant identifier
   * @return true if tenant has used trial
   */
  @Query("""
      SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
      FROM TenantTrialHistory t
      WHERE t.tenantId = :tenantId
        AND t.hasUsedTrial = true
      """)
  boolean hasUsedTrial(@Param("tenantId") UUID tenantId);
}
