package com.iqscaffold.billingservice.usage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for UsageRecord entity.
 *
 * <p>This repository provides methods to query and persist usage records.
 * Usage records track resource consumption for billing and quota enforcement.
 *
 * <p>Uses text blocks (Java 21) for multi-line JPQL queries to improve readability.
 */
@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

  /**
   * Finds all usage records for a tenant.
   *
   * @param tenantId tenant identifier
   * @param pageable pagination information
   * @return page of usage records ordered by recorded date descending
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
      ORDER BY ur.recordedAt DESC
      """)
  Page<UsageRecord> findByTenantId(
      @Param("tenantId") UUID tenantId,
      Pageable pageable
  );

  /**
   * Finds all usage records for a tenant (for GDPR export).
   *
   * @param tenantId tenant identifier
   * @return list of all usage records for the tenant
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
      ORDER BY ur.recordedAt DESC
      """)
  List<UsageRecord> findByTenantId(@Param("tenantId") String tenantId);

  /**
   * Finds usage records by tenant and metric type.
   *
   * @param tenantId   tenant identifier
   * @param metricType metric type (API_CALLS, STORAGE_GB, ACTIVE_USERS, CUSTOM)
   * @param pageable   pagination information
   * @return page of usage records matching criteria
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
        AND ur.metricType = :metricType
      ORDER BY ur.recordedAt DESC
      """)
  Page<UsageRecord> findByTenantIdAndMetricType(
      @Param("tenantId") UUID tenantId,
      @Param("metricType") MetricType metricType,
      Pageable pageable
  );

  /**
   * Finds usage records by subscription.
   *
   * @param subscriptionId subscription identifier
   * @return list of usage records for the subscription
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.subscription.id = :subscriptionId
      ORDER BY ur.recordedAt DESC
      """)
  List<UsageRecord> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

  /**
   * Finds usage records for a billing period.
   *
   * @param tenantId    tenant identifier
   * @param periodStart billing period start date
   * @param periodEnd   billing period end date
   * @return list of usage records in the billing period
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
        AND ur.billingPeriodStart = :periodStart
        AND ur.billingPeriodEnd = :periodEnd
      ORDER BY ur.recordedAt DESC
      """)
  List<UsageRecord> findByTenantIdAndBillingPeriod(
      @Param("tenantId") UUID tenantId,
      @Param("periodStart") LocalDateTime periodStart,
      @Param("periodEnd") LocalDateTime periodEnd
  );

  /**
   * Finds usage records by tenant, metric type, and billing period.
   *
   * @param tenantId    tenant identifier
   * @param metricType  metric type
   * @param periodStart billing period start date
   * @param periodEnd   billing period end date
   * @return list of usage records matching criteria
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
        AND ur.metricType = :metricType
        AND ur.billingPeriodStart = :periodStart
        AND ur.billingPeriodEnd = :periodEnd
      ORDER BY ur.recordedAt DESC
      """)
  List<UsageRecord> findByTenantIdAndMetricTypeAndBillingPeriod(
      @Param("tenantId") UUID tenantId,
      @Param("metricType") MetricType metricType,
      @Param("periodStart") LocalDateTime periodStart,
      @Param("periodEnd") LocalDateTime periodEnd
  );

  /**
   * Finds usage records within a date range.
   *
   * @param tenantId  tenant identifier
   * @param startDate start of date range
   * @param endDate   end of date range
   * @return list of usage records in the date range
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
        AND ur.recordedAt >= :startDate
        AND ur.recordedAt <= :endDate
      ORDER BY ur.recordedAt DESC
      """)
  List<UsageRecord> findByTenantIdAndDateRange(
      @Param("tenantId") UUID tenantId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * Calculates total usage for a tenant and metric type in a billing period.
   *
   * @param tenantId    tenant identifier
   * @param metricType  metric type
   * @param periodStart billing period start date
   * @param periodEnd   billing period end date
   * @return sum of quantities for the metric in the period
   */
  @Query("""
      SELECT COALESCE(SUM(ur.quantity), 0) FROM UsageRecord ur
      WHERE ur.tenantId = :tenantId
        AND ur.metricType = :metricType
        AND ur.billingPeriodStart = :periodStart
        AND ur.billingPeriodEnd = :periodEnd
      """)
  long calculateTotalUsageForPeriod(
      @Param("tenantId") UUID tenantId,
      @Param("metricType") MetricType metricType,
      @Param("periodStart") LocalDateTime periodStart,
      @Param("periodEnd") LocalDateTime periodEnd
  );

  /**
   * Calculates total usage for a tenant and metric type within a date range.
   *
   * @param tenantId   tenant identifier
   * @param metricType metric type
   * @param startDate  start of date range
   * @param endDate    end of date range
   * @return sum of quantities for the metric in the date range
   */
  @Query("""
      SELECT COALESCE(SUM(ur.quantity), 0) FROM UsageRecord ur
      WHERE ur.tenantId = :tenantId
        AND ur.metricType = :metricType
        AND ur.recordedAt >= :startDate
        AND ur.recordedAt <= :endDate
      """)
  long calculateTotalUsageForDateRange(
      @Param("tenantId") UUID tenantId,
      @Param("metricType") MetricType metricType,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * Aggregates usage by metric type for a tenant in a billing period.
   * Returns a list of metric types with their total usage.
   *
   * @param tenantId    tenant identifier
   * @param periodStart billing period start date
   * @param periodEnd   billing period end date
   * @return list of usage metrics with aggregated quantities
   */
  @Query("""
      SELECT new com.iqscaffold.billingservice.usage.UsageMetric(
        ur.metricType,
        SUM(ur.quantity),
        ur.unit
      )
      FROM UsageRecord ur
      WHERE ur.tenantId = :tenantId
        AND ur.billingPeriodStart = :periodStart
        AND ur.billingPeriodEnd = :periodEnd
      GROUP BY ur.metricType, ur.unit
      ORDER BY ur.metricType ASC
      """)
  List<UsageMetric> aggregateUsageByMetricType(
      @Param("tenantId") UUID tenantId,
      @Param("periodStart") LocalDateTime periodStart,
      @Param("periodEnd") LocalDateTime periodEnd
  );

  /**
   * Finds the most recent usage record for a tenant and metric type.
   *
   * @param tenantId   tenant identifier
   * @param metricType metric type
   * @return optional containing the most recent usage record, or empty if none exists
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
        AND ur.metricType = :metricType
      ORDER BY ur.recordedAt DESC
      LIMIT 1
      """)
  java.util.Optional<UsageRecord> findMostRecentByTenantIdAndMetricType(
      @Param("tenantId") UUID tenantId,
      @Param("metricType") MetricType metricType
  );

  /**
   * Counts usage records for a tenant in a billing period.
   *
   * @param tenantId    tenant identifier
   * @param periodStart billing period start date
   * @param periodEnd   billing period end date
   * @return number of usage records in the period
   */
  @Query("""
      SELECT COUNT(ur) FROM UsageRecord ur
      WHERE ur.tenantId = :tenantId
        AND ur.billingPeriodStart = :periodStart
        AND ur.billingPeriodEnd = :periodEnd
      """)
  long countByTenantIdAndBillingPeriod(
      @Param("tenantId") UUID tenantId,
      @Param("periodStart") LocalDateTime periodStart,
      @Param("periodEnd") LocalDateTime periodEnd
  );

  /**
   * Deletes usage records older than the specified date.
   * Used for data retention and cleanup.
   *
   * @param cutoffDate date before which records should be deleted
   * @return number of records deleted
   */
  @Query("""
      DELETE FROM UsageRecord ur
      WHERE ur.recordedAt < :cutoffDate
      """)
  int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);


  /**
   * Finds usage records for a tenant after a specific date.
   *
   * @param tenantId  tenant identifier
   * @param afterDate date after which to find records
   * @return list of usage records after the date
   */
  @Query("""
      SELECT ur FROM UsageRecord ur
      LEFT JOIN FETCH ur.subscription s
      LEFT JOIN FETCH s.plan
      WHERE ur.tenantId = :tenantId
        AND ur.recordedAt > :afterDate
      ORDER BY ur.recordedAt DESC
      """)
  List<UsageRecord> findByTenantIdAndRecordedAtAfter(
      @Param("tenantId") String tenantId,
      @Param("afterDate") LocalDateTime afterDate
  );

  /**
   * Deletes all usage records for a tenant (for GDPR deletion).
   *
   * @param tenantId tenant identifier
   * @return number of records deleted
   */
  @Query("""
      DELETE FROM UsageRecord ur
      WHERE ur.tenantId = :tenantId
      """)
  int deleteByTenantId(@Param("tenantId") String tenantId);

  /**
   * Deletes usage records before a specific date (for retention policy).
   *
   * @param cutoffDate date before which to delete records
   * @return number of records deleted
   */
  @Query("""
      DELETE FROM UsageRecord ur
      WHERE ur.recordedAt < :cutoffDate
      """)
  int deleteByRecordedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
