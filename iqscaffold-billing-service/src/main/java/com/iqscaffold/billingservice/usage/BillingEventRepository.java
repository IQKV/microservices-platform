package com.iqscaffold.billingservice.usage;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for BillingEvent entity.
 *
 * <p>Provides query methods for retrieving audit logs and billing events.
 * All queries are automatically scoped to the current tenant schema.
 *
 * <p><strong>Note:</strong> This repository does not provide update or delete
 * methods as billing events are immutable audit records (REQ-SEC-012).
 */
@Repository
public interface BillingEventRepository extends JpaRepository<BillingEvent, Long> {

  /**
   * Find all events for a specific tenant.
   *
   * @param tenantId the tenant identifier
   * @param pageable pagination information
   * @return page of billing events
   */
  Page<BillingEvent> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

  /**
   * Find events by entity type and entity ID.
   *
   * @param entityType the entity type (e.g., SUBSCRIPTION, INVOICE)
   * @param entityId   the entity identifier
   * @param pageable   pagination information
   * @return page of billing events
   */
  Page<BillingEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
      String entityType, Long entityId, Pageable pageable);

  /**
   * Find events by event type.
   *
   * @param eventType the event type (e.g., SUBSCRIPTION_CREATED)
   * @param pageable  pagination information
   * @return page of billing events
   */
  Page<BillingEvent> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

  /**
   * Find events by user ID.
   *
   * @param userId   the user identifier
   * @param pageable pagination information
   * @return page of billing events
   */
  Page<BillingEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /**
   * Find events within a date range.
   *
   * @param startDate the start date
   * @param endDate   the end date
   * @param pageable  pagination information
   * @return page of billing events
   */
  @Query("""
      SELECT e FROM BillingEvent e
      WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate
      ORDER BY e.createdAt DESC
      """)
  Page<BillingEvent> findByDateRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  /**
   * Find events for a specific entity.
   *
   * @param tenantId   the tenant identifier
   * @param entityType the entity type
   * @param entityId   the entity identifier
   * @return list of billing events
   */
  List<BillingEvent> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
      String tenantId, String entityType, Long entityId);

  /**
   * Count events by event type within a date range.
   *
   * @param eventType the event type
   * @param startDate the start date
   * @param endDate   the end date
   * @return count of events
   */
  @Query("""
      SELECT COUNT(e) FROM BillingEvent e
      WHERE e.eventType = :eventType
      AND e.createdAt >= :startDate
      AND e.createdAt <= :endDate
      """)
  long countByEventTypeAndDateRange(
      @Param("eventType") String eventType,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);
}
