package com.iqscaffold.billingservice.usage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Immutable value object representing a summary of usage for a tenant.
 * 
 * <p>Aggregates usage metrics across different metric types for a specific
 * time period. Used for displaying usage dashboards, generating reports,
 * and calculating billing amounts.
 * 
 * <p>As a Java record, this class is:
 * <ul>
 *   <li>Immutable - all fields are final</li>
 *   <li>Value-based - equality based on field values</li>
 *   <li>Compact - automatic constructor, getters, equals, hashCode, toString</li>
 * </ul>
 * 
 * @param tenantId the tenant identifier
 * @param periodStart start of the usage period
 * @param periodEnd end of the usage period
 * @param metrics list of usage metrics by type
 * @param totalRecords total number of usage records in the period
 * 
 * @see UsageMetric
 * @see MetricType
 */
public record UsageSummary(
    UUID tenantId,
    LocalDateTime periodStart,
    LocalDateTime periodEnd,
    List<UsageMetric> metrics,
    int totalRecords) {

  /**
   * Compact constructor with validation.
   *
   * @param tenantId the tenant identifier
   * @param periodStart start of the usage period
   * @param periodEnd end of the usage period
   * @param metrics list of usage metrics
   * @param totalRecords total number of records
   * @throws IllegalArgumentException if validation fails
   */
  public UsageSummary {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
    if (periodStart == null) {
      throw new IllegalArgumentException("Period start cannot be null");
    }
    if (periodEnd == null) {
      throw new IllegalArgumentException("Period end cannot be null");
    }
    if (periodStart.isAfter(periodEnd)) {
      throw new IllegalArgumentException("Period start must be before or equal to period end");
    }
    if (metrics == null) {
      throw new IllegalArgumentException("Metrics list cannot be null");
    }
    if (totalRecords < 0) {
      throw new IllegalArgumentException("Total records cannot be negative");
    }
  }

  /**
   * Finds a usage metric by type.
   *
   * @param metricType the metric type to find
   * @return the usage metric, or null if not found
   */
  public UsageMetric findMetric(final MetricType metricType) {
    return metrics.stream()
        .filter(m -> m.metricType() == metricType)
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks if usage exists for a specific metric type.
   *
   * @param metricType the metric type to check
   * @return true if usage exists for the metric type
   */
  public boolean hasMetric(final MetricType metricType) {
    return metrics.stream().anyMatch(m -> m.metricType() == metricType);
  }
}
