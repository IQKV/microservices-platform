package com.iqscaffold.billingservice.feature;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for feature usage tracking and analytics.
 */
@Repository
public interface FeatureUsageLogRepository extends JpaRepository<FeatureUsageLog, Long> {

  /**
   * Finds usage logs for a specific tenant and feature within a time range.
   */
  @Query("SELECT f FROM FeatureUsageLog f WHERE f.tenantId = :tenantId AND f.featureKey = :featureKey AND f.timestamp BETWEEN :startTime AND :endTime ORDER BY f.timestamp DESC")
  List<FeatureUsageLog> findByTenantAndFeatureInTimeRange(
      @Param("tenantId") String tenantId,
      @Param("featureKey") String featureKey,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Counts usage for a specific tenant and feature within a time range.
   */
  @Query("SELECT COUNT(f) FROM FeatureUsageLog f WHERE f.tenantId = :tenantId AND f.featureKey = :featureKey AND f.timestamp BETWEEN :startTime AND :endTime")
  long countByTenantAndFeatureInTimeRange(
      @Param("tenantId") String tenantId,
      @Param("featureKey") String featureKey,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Finds usage logs for a tenant within a time range.
   */
  Page<FeatureUsageLog> findByTenantIdAndTimestampBetweenOrderByTimestampDesc(
      String tenantId, Instant startTime, Instant endTime, Pageable pageable);

  /**
   * Gets feature usage statistics for a tenant.
   */
  @Query("SELECT f.featureKey, COUNT(f) as usageCount FROM FeatureUsageLog f WHERE f.tenantId = :tenantId AND f.timestamp BETWEEN :startTime AND :endTime GROUP BY f.featureKey ORDER BY usageCount DESC")
  List<Object[]> getFeatureUsageStatsByTenant(
      @Param("tenantId") String tenantId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Gets most used features across all tenants.
   */
  @Query("SELECT f.featureKey, COUNT(f) as usageCount FROM FeatureUsageLog f WHERE f.timestamp BETWEEN :startTime AND :endTime GROUP BY f.featureKey ORDER BY usageCount DESC")
  List<Object[]> getMostUsedFeatures(
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime,
      Pageable pageable);

  /**
   * Gets endpoint usage for a specific feature.
   */
  @Query("SELECT f.endpoint, COUNT(f) as usageCount FROM FeatureUsageLog f WHERE f.featureKey = :featureKey AND f.timestamp BETWEEN :startTime AND :endTime GROUP BY f.endpoint ORDER BY usageCount DESC")
  List<Object[]> getEndpointUsageByFeature(
      @Param("featureKey") String featureKey,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Deletes old usage logs (for data retention).
   */
  void deleteByTimestampBefore(Instant cutoffTime);

  /**
   * Gets daily usage counts for a feature.
   */
  @Query(value = "SELECT DATE(timestamp) as usage_date, COUNT(*) as usage_count FROM feature_usage_log WHERE feature_key = :featureKey AND timestamp BETWEEN :startTime AND :endTime GROUP BY DATE(timestamp) ORDER BY usage_date", nativeQuery = true)
  List<Object[]> getDailyUsageByFeature(
      @Param("featureKey") String featureKey,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Gets unique users using a feature.
   */
  @Query("SELECT COUNT(DISTINCT f.userId) FROM FeatureUsageLog f WHERE f.featureKey = :featureKey AND f.timestamp BETWEEN :startTime AND :endTime")
  long countUniqueUsersByFeature(
      @Param("featureKey") String featureKey,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);
}