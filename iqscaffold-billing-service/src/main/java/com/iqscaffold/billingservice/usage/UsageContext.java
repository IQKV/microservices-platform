package com.iqscaffold.billingservice.usage;

/**
 * Value object representing usage context for quota validation.
 * 
 * <p>Encapsulates the current usage and quota limit for a specific metric type.
 * This immutable value object is used by specifications and domain services to
 * evaluate whether usage has exceeded quota limits.
 * 
 * <p>Example usage:
 * <pre>{@code
 * UsageContext context = new UsageContext(
 *   MetricType.API_CALLS,
 *   950L,  // current usage
 *   1000L  // quota limit
 * );
 * 
 * QuotaExceededSpecification spec = new QuotaExceededSpecification();
 * boolean exceeded = spec.isSatisfiedBy(context); // false
 * }</pre>
 * 
 * @param metricType the type of metric being tracked
 * @param currentUsage the current usage amount
 * @param quotaLimit the maximum allowed usage
 */
public record UsageContext(
    MetricType metricType,
    long currentUsage,
    long quotaLimit
) {

  /**
   * Creates a new usage context with validation.
   * 
   * @param metricType the type of metric being tracked
   * @param currentUsage the current usage amount
   * @param quotaLimit the maximum allowed usage
   * @throws IllegalArgumentException if metricType is null, currentUsage is negative, or quotaLimit is negative
   */
  public UsageContext {
    if (metricType == null) {
      throw new IllegalArgumentException("Metric type cannot be null");
    }
    if (currentUsage < 0) {
      throw new IllegalArgumentException("Current usage cannot be negative");
    }
    if (quotaLimit < 0) {
      throw new IllegalArgumentException("Quota limit cannot be negative");
    }
  }

  /**
   * Calculates the remaining quota.
   * 
   * @return the amount of quota remaining (0 if exceeded)
   */
  public long remainingQuota() {
    return Math.max(0, quotaLimit - currentUsage);
  }

  /**
   * Calculates the usage percentage.
   * 
   * @return the percentage of quota used (0-100+)
   */
  public double usagePercentage() {
    if (quotaLimit == 0) {
      return 0.0;
    }
    return (currentUsage * 100.0) / quotaLimit;
  }

  /**
   * Checks if usage is approaching the quota limit.
   * 
   * @param thresholdPercentage the threshold percentage (e.g., 80 for 80%)
   * @return true if usage is at or above the threshold
   */
  public boolean isApproachingLimit(final double thresholdPercentage) {
    return usagePercentage() >= thresholdPercentage;
  }

  /**
   * Checks if the quota is unlimited.
   * 
   * @return true if quota limit is Long.MAX_VALUE (unlimited)
   */
  public boolean isUnlimited() {
    return quotaLimit == Long.MAX_VALUE;
  }
}
