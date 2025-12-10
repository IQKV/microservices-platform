package com.iqscaffold.gatewayservice.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when API quota is exceeded.
 *
 * <p>This exception is thrown by the QuotaEnforcementFilter when a tenant
 * has exceeded their API call quota for the current billing period.
 */
public class QuotaExceededException extends RuntimeException {

  private final String tenantId;
  private final String metricType;
  private final long currentUsage;
  private final long limit;
  private final LocalDateTime resetsAt;

  public QuotaExceededException(
      final String tenantId,
      final String metricType,
      final long currentUsage,
      final long limit,
      final LocalDateTime resetsAt) {
    super(String.format("Quota exceeded for %s: used %d of %d (resets at %s)",
        metricType, currentUsage, limit, resetsAt));
    this.tenantId = tenantId;
    this.metricType = metricType;
    this.currentUsage = currentUsage;
    this.limit = limit;
    this.resetsAt = resetsAt;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getMetricType() {
    return metricType;
  }

  public long getCurrentUsage() {
    return currentUsage;
  }

  public long getLimit() {
    return limit;
  }

  public LocalDateTime getResetsAt() {
    return resetsAt;
  }
}
