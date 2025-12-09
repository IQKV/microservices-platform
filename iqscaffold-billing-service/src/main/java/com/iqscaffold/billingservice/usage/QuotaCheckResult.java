package com.iqscaffold.billingservice.usage;

/**
 * Immutable value object representing the result of a quota check.
 *
 * <p>Encapsulates whether a requested operation is allowed based on current
 * usage and quota limits. Provides detailed information about the quota
 * status for decision-making and error messaging.
 *
 * <p>As a Java record, this class is:
 * <ul>
 *   <li>Immutable - all fields are final</li>
 *   <li>Value-based - equality based on field values</li>
 *   <li>Compact - automatic constructor, getters, equals, hashCode, toString</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * QuotaCheckResult result = quotaService.checkQuota(tenantId, MetricType.API_CALLS, 1);
 * if (!result.allowed()) {
 *   throw new QuotaExceededException(
 *     "API call quota exceeded",
 *     result.metricType().name(),
 *     result.limit(),
 *     result.currentUsage()
 *   );
 * }
 * }</pre>
 *
 * @param metricType      the type of metric being checked
 * @param allowed         whether the requested operation is allowed
 * @param currentUsage    the current usage amount
 * @param requestedAmount the amount being requested
 * @param limit           the quota limit (null if unlimited)
 * @param remainingQuota  the remaining quota after the request
 * @param message         optional message explaining the result
 * @see MetricType
 * @see UsageMetric
 */
public record QuotaCheckResult(
    MetricType metricType,
    boolean allowed,
    long currentUsage,
    long requestedAmount,
    Long limit,
    long remainingQuota,
    String message) {

  /**
   * Compact constructor with validation.
   *
   * @param metricType      the type of metric
   * @param allowed         whether the operation is allowed
   * @param currentUsage    current usage amount
   * @param requestedAmount requested amount
   * @param limit           quota limit
   * @param remainingQuota  remaining quota
   * @param message         optional message
   * @throws IllegalArgumentException if validation fails
   */
  public QuotaCheckResult {
    if (metricType == null) {
      throw new IllegalArgumentException("Metric type cannot be null");
    }
    if (currentUsage < 0) {
      throw new IllegalArgumentException("Current usage cannot be negative");
    }
    if (requestedAmount < 0) {
      throw new IllegalArgumentException("Requested amount cannot be negative");
    }
    if (limit != null && limit < 0) {
      throw new IllegalArgumentException("Limit cannot be negative");
    }
    if (remainingQuota < 0) {
      throw new IllegalArgumentException("Remaining quota cannot be negative");
    }
  }

  /**
   * Creates a result indicating the quota check passed.
   *
   * @param metricType      the metric type
   * @param currentUsage    current usage
   * @param requestedAmount requested amount
   * @param limit           quota limit
   * @param remainingQuota  remaining quota
   * @return a quota check result indicating success
   */
  public static QuotaCheckResult allowed(
      final MetricType metricType,
      final long currentUsage,
      final long requestedAmount,
      final Long limit,
      final long remainingQuota) {
    return new QuotaCheckResult(
        metricType,
        true,
        currentUsage,
        requestedAmount,
        limit,
        remainingQuota,
        "Quota check passed");
  }

  /**
   * Creates a result indicating the quota check failed.
   *
   * @param metricType      the metric type
   * @param currentUsage    current usage
   * @param requestedAmount requested amount
   * @param limit           quota limit
   * @param message         explanation of why the check failed
   * @return a quota check result indicating failure
   */
  public static QuotaCheckResult denied(
      final MetricType metricType,
      final long currentUsage,
      final long requestedAmount,
      final Long limit,
      final String message) {
    return new QuotaCheckResult(
        metricType,
        false,
        currentUsage,
        requestedAmount,
        limit,
        0,
        message);
  }

  /**
   * Creates a result for unlimited quota (always allowed).
   *
   * @param metricType      the metric type
   * @param currentUsage    current usage
   * @param requestedAmount requested amount
   * @return a quota check result for unlimited quota
   */
  public static QuotaCheckResult unlimited(
      final MetricType metricType,
      final long currentUsage,
      final long requestedAmount) {
    return new QuotaCheckResult(
        metricType,
        true,
        currentUsage,
        requestedAmount,
        null,
        Long.MAX_VALUE,
        "Unlimited quota");
  }

  /**
   * Checks if the quota has a limit.
   *
   * @return true if a limit is defined
   */
  public boolean hasLimit() {
    return limit != null;
  }

  /**
   * Checks if the quota is exceeded.
   *
   * @return true if current usage exceeds the limit
   */
  public boolean isExceeded() {
    return hasLimit() && currentUsage >= limit;
  }

  /**
   * Checks if the quota is approaching the limit (within 90%).
   *
   * @return true if usage is at or above 90% of the limit
   */
  public boolean isApproachingLimit() {
    return hasLimit() && currentUsage >= (limit * 0.9);
  }

  /**
   * Calculates the percentage of quota used.
   *
   * @return percentage used (0-100+), or 0 if unlimited
   */
  public double percentageUsed() {
    if (!hasLimit() || limit == 0) {
      return 0.0;
    }
    return (currentUsage * 100.0) / limit;
  }

  /**
   * Gets the total usage after the requested amount would be applied.
   *
   * @return projected usage
   */
  public long projectedUsage() {
    return currentUsage + requestedAmount;
  }
}
