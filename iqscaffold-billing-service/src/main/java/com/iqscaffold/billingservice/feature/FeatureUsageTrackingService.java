package com.iqscaffold.billingservice.feature;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for tracking feature usage for analytics and billing purposes.
 *
 * <p>This service records feature usage events asynchronously to avoid impacting
 * request performance. Usage data is used for:
 * <ul>
 *   <li>Feature adoption analytics</li>
 *   <li>Usage-based billing</li>
 *   <li>Feature deprecation planning</li>
 *   <li>Performance monitoring</li>
 * </ul>
 */
@Service
@Transactional
public class FeatureUsageTrackingService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureUsageTrackingService.class);

  private final FeatureUsageLogRepository usageLogRepository;

  public FeatureUsageTrackingService(final FeatureUsageLogRepository usageLogRepository) {
    this.usageLogRepository = usageLogRepository;
  }

  /**
   * Records feature usage asynchronously.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the feature that was used
   * @param endpoint   the endpoint where the feature was used (optional)
   */
  @Async
  public void recordUsage(String tenantId, String featureKey, String endpoint) {
    try {
      FeatureUsageLog usageLog = new FeatureUsageLog(tenantId, featureKey, endpoint);

      // Capture additional context from MDC if available
      String userId = MDC.get("userId");
      String correlationId = MDC.get("correlationId");
      String sessionId = MDC.get("sessionId");

      usageLog.setUserId(userId);
      usageLog.setCorrelationId(correlationId);
      usageLog.setSessionId(sessionId);

      usageLogRepository.save(usageLog);

      logger.debug("Recorded feature usage: tenant={}, feature={}, endpoint={}", tenantId, featureKey, endpoint);

    } catch (final Exception e) {
      logger.error("Failed to record feature usage for tenant {} and feature {}: {}",
          tenantId, featureKey, e.getMessage(), e);
      // Don't rethrow - usage tracking should not fail the main request
    }
  }

  /**
   * Records feature usage with additional metadata.
   */
  @Async
  public void recordUsage(String tenantId, String featureKey, String endpoint, Map<String, Object> metadata) {
    try {
      FeatureUsageLog usageLog = new FeatureUsageLog(tenantId, featureKey, endpoint);

      // Capture additional context from MDC if available
      String userId = MDC.get("userId");
      String correlationId = MDC.get("correlationId");
      String sessionId = MDC.get("sessionId");

      usageLog.setUserId(userId);
      usageLog.setCorrelationId(correlationId);
      usageLog.setSessionId(sessionId);
      usageLog.setMetadata(metadata);

      usageLogRepository.save(usageLog);

      logger.debug("Recorded feature usage with metadata: tenant={}, feature={}, endpoint={}",
          tenantId, featureKey, endpoint);

    } catch (final Exception e) {
      logger.error("Failed to record feature usage with metadata for tenant {} and feature {}: {}",
          tenantId, featureKey, e.getMessage(), e);
    }
  }

  /**
   * Gets usage count for a feature within a time period.
   */
  public long getUsageCount(String tenantId, String featureKey, Instant startTime, Instant endTime) {
    return usageLogRepository.countByTenantAndFeatureInTimeRange(tenantId, featureKey, startTime, endTime);
  }

  /**
   * Gets usage statistics for a tenant.
   */
  public Map<String, Long> getUsageStatistics(String tenantId, Instant startTime, Instant endTime) {
    List<Object[]> results = usageLogRepository.getFeatureUsageStatsByTenant(tenantId, startTime, endTime);

    Map<String, Long> statistics = new HashMap<>();
    for (final Object[] result : results) {
      String featureKey = (String) result[0];
      Long usageCount = (Long) result[1];
      statistics.put(featureKey, usageCount);
    }

    return statistics;
  }

  /**
   * Gets daily usage count for a feature in the last 30 days.
   */
  public long getDailyUsageCount(String tenantId, String featureKey) {
    Instant endTime = Instant.now();
    Instant startTime = endTime.minus(1, ChronoUnit.DAYS);

    return usageLogRepository.countByTenantAndFeatureInTimeRange(tenantId, featureKey, startTime, endTime);
  }

  /**
   * Gets monthly usage count for a feature.
   */
  public long getMonthlyUsageCount(String tenantId, String featureKey) {
    Instant endTime = Instant.now();
    Instant startTime = endTime.minus(30, ChronoUnit.DAYS);

    return usageLogRepository.countByTenantAndFeatureInTimeRange(tenantId, featureKey, startTime, endTime);
  }

  /**
   * Checks if a tenant has used a feature recently (within last 7 days).
   */
  public boolean hasRecentUsage(String tenantId, String featureKey) {
    Instant endTime = Instant.now();
    Instant startTime = endTime.minus(7, ChronoUnit.DAYS);

    long usageCount = usageLogRepository.countByTenantAndFeatureInTimeRange(tenantId, featureKey, startTime, endTime);
    return usageCount > 0;
  }

  /**
   * Gets the most used features across all tenants.
   */
  public Map<String, Long> getMostUsedFeatures(int limit) {
    Instant endTime = Instant.now();
    Instant startTime = endTime.minus(30, ChronoUnit.DAYS);

    List<Object[]> results = usageLogRepository.getMostUsedFeatures(startTime, endTime,
        org.springframework.data.domain.PageRequest.of(0, limit));

    Map<String, Long> mostUsed = new HashMap<>();
    for (final Object[] result : results) {
      String featureKey = (String) result[0];
      Long usageCount = (Long) result[1];
      mostUsed.put(featureKey, usageCount);
    }

    return mostUsed;
  }

  /**
   * Cleans up old usage logs (data retention).
   * Should be called periodically to manage database size.
   */
  @Transactional
  public void cleanupOldUsageLogs(int retentionDays) {
    Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

    try {
      usageLogRepository.deleteByTimestampBefore(cutoffTime);
      logger.info("Cleaned up feature usage logs older than {} days", retentionDays);
    } catch (final Exception e) {
      logger.error("Failed to cleanup old usage logs: {}", e.getMessage(), e);
    }
  }
}
