package com.iqscaffold.billingservice.usage;

/**
 * Immutable value object representing usage for a specific metric type.
 *
 * <p>Encapsulates the total usage quantity and unit for a single metric type
 * within a time period. Used as part of usage summaries and quota checks.
 *
 * <p>As a Java record, this class is:
 * <ul>
 *   <li>Immutable - all fields are final</li>
 *   <li>Value-based - equality based on field values</li>
 *   <li>Compact - automatic constructor, getters, equals, hashCode, toString</li>
 * </ul>
 *
 * @param metricType the type of metric
 * @param quantity   the total quantity used
 * @param unit       the unit of measurement (e.g., "requests", "GB", "emails")
 * @param limit      the quota limit for this metric (null if unlimited)
 * @see MetricType
 * @see UsageSummary
 */
public record UsageMetric(
    MetricType metricType,
    long quantity,
    String unit,
    Long limit) {

  /**
   * Compact constructor with validation.
   *
   * @param metricType the type of metric
   * @param quantity   the total quantity used
   * @param unit       the unit of measurement
   * @param limit      the quota limit
   * @throws IllegalArgumentException if validation fails
   */
  public UsageMetric {
    if (metricType == null) {
      throw new IllegalArgumentException("Metric type cannot be null");
    }
    if (quantity < 0) {
      throw new IllegalArgumentException("Quantity cannot be negative");
    }
    if (limit != null && limit < 0) {
      throw new IllegalArgumentException("Limit cannot be negative");
    }
  }

  /**
   * Creates a usage metric without a limit (unlimited).
   *
   * @param metricType the type of metric
   * @param quantity   the total quantity used
   * @param unit       the unit of measurement
   * @return a new usage metric
   */
  public static UsageMetric unlimited(
      final MetricType metricType,
      final long quantity,
      final String unit) {
    return new UsageMetric(metricType, quantity, unit, null);
  }

  /**
   * Checks if this metric has a quota limit.
   *
   * @return true if a limit is defined
   */
  public boolean hasLimit() {
    return limit != null;
  }

  /**
   * Checks if the usage has exceeded the limit.
   *
   * @return true if usage exceeds the limit
   */
  public boolean isExceeded() {
    return hasLimit() && quantity > limit;
  }

  /**
   * Checks if the usage is approaching the limit (within 90%).
   *
   * @return true if usage is at or above 90% of the limit
   */
  public boolean isApproachingLimit() {
    return hasLimit() && quantity >= (limit * 0.9);
  }

  /**
   * Calculates the percentage of the limit used.
   *
   * @return percentage used (0-100+), or 0 if no limit
   */
  public double percentageUsed() {
    if (!hasLimit() || limit == 0) {
      return 0.0;
    }
    return (quantity * 100.0) / limit;
  }

  /**
   * Calculates the remaining quota.
   *
   * @return remaining quantity, or Long.MAX_VALUE if unlimited
   */
  public long remaining() {
    if (!hasLimit()) {
      return Long.MAX_VALUE;
    }
    return Math.max(0, limit - quantity);
  }
}
