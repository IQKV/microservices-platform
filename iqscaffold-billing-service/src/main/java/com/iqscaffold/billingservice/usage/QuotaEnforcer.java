package com.iqscaffold.billingservice.usage;

import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.Subscription;
import org.springframework.stereotype.Service;

/**
 * Domain service for enforcing quota limits based on subscription plans.
 * 
 * <p>This service encapsulates the business logic for validating whether a tenant
 * can perform an operation based on their current usage and subscription plan quotas.
 * It provides quota checking, enforcement, and grace period handling.
 * 
 * <p>Quota enforcement involves:
 * <ul>
 *   <li>Checking current usage against plan limits</li>
 *   <li>Applying grace periods (5% overage allowed)</li>
 *   <li>Handling unlimited quotas</li>
 *   <li>Providing detailed quota status information</li>
 * </ul>
 * 
 * <p>This logic spans multiple aggregates (Subscription, SubscriptionPlan, UsageRecord)
 * and doesn't naturally belong to any single aggregate. Therefore, it's implemented
 * as a stateless domain service.
 * 
 * <p>Usage example:
 * <pre>{@code
 * QuotaCheckResult result = quotaEnforcer.checkQuota(
 *   subscription,
 *   MetricType.API_CALLS,
 *   1
 * );
 * 
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
 * <p>Design Rationale: Domain services keep business logic in the domain layer while
 * avoiding artificial assignment to aggregates. They remain stateless and focused on
 * domain operations.
 * 
 * @see QuotaCheckResult
 * @see Subscription
 * @see SubscriptionPlan
 * @see PlanQuotas
 * @see MetricType
 */
@Service
public class QuotaEnforcer {

  /**
   * Grace period percentage for quota overages.
   * Allows 5% overage before hard enforcement.
   */
  private static final double GRACE_PERIOD_PERCENTAGE = 0.05;

  /**
   * Checks if a requested operation is allowed based on current usage and quota limits.
   * 
   * <p>This method performs the following checks:
   * <ul>
   *   <li>Retrieves the quota limit for the metric type from the subscription plan</li>
   *   <li>Compares current usage + requested amount against the limit</li>
   *   <li>Applies grace period (5% overage) if configured</li>
   *   <li>Returns detailed result with quota status</li>
   * </ul>
   * 
   * <p>The check considers:
   * <ul>
   *   <li>Unlimited quotas (always allowed)</li>
   *   <li>Current usage in the billing period</li>
   *   <li>Requested amount for the operation</li>
   *   <li>Grace period for soft limits</li>
   * </ul>
   * 
   * @param subscription the subscription to check
   * @param metricType the type of metric being checked
   * @param requestedQuantity the amount being requested
   * @return a QuotaCheckResult indicating if the operation is allowed
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public QuotaCheckResult checkQuota(
      final Subscription subscription,
      final MetricType metricType,
      final long requestedQuantity) {

    validateSubscription(subscription);
    validateMetricType(metricType);
    validateRequestedQuantity(requestedQuantity);

    // Get the subscription plan and quotas
    var plan = subscription.getPlan();
    var quotas = plan.getQuotas();

    // Get the quota limit for this metric type
    var limit = getQuotaLimit(quotas, metricType);

    // If unlimited, always allow
    if (limit == null) {
      return QuotaCheckResult.unlimited(metricType, 0, requestedQuantity);
    }

    // This would typically come from a usage repository query
    // For now, we'll accept it as a parameter in the overloaded method
    throw new UnsupportedOperationException(
        "Use checkQuota(subscription, metricType, currentUsage, requestedQuantity) instead"
    );
  }

  /**
   * Checks if a requested operation is allowed based on current usage and quota limits.
   * 
   * <p>This overloaded method accepts the current usage as a parameter, allowing
   * the caller to provide the usage data from their repository.
   * 
   * @param subscription the subscription to check
   * @param metricType the type of metric being checked
   * @param currentUsage the current usage amount in the billing period
   * @param requestedQuantity the amount being requested
   * @return a QuotaCheckResult indicating if the operation is allowed
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public QuotaCheckResult checkQuota(
      final Subscription subscription,
      final MetricType metricType,
      final long currentUsage,
      final long requestedQuantity) {

    validateSubscription(subscription);
    validateMetricType(metricType);
    validateCurrentUsage(currentUsage);
    validateRequestedQuantity(requestedQuantity);

    // Get the subscription plan and quotas
    var plan = subscription.getPlan();
    var quotas = plan.getQuotas();

    // Get the quota limit for this metric type
    var limit = getQuotaLimit(quotas, metricType);

    // If unlimited, always allow
    if (limit == null) {
      return QuotaCheckResult.unlimited(metricType, currentUsage, requestedQuantity);
    }

    // Calculate projected usage
    var projectedUsage = currentUsage + requestedQuantity;

    // Check if within limit
    if (projectedUsage <= limit) {
      var remainingQuota = limit - projectedUsage;
      return QuotaCheckResult.allowed(
          metricType,
          currentUsage,
          requestedQuantity,
          limit,
          remainingQuota
      );
    }

    // Check if within grace period (5% overage)
    var graceLimit = calculateGraceLimit(limit);
    if (projectedUsage <= graceLimit) {
      var remainingQuota = graceLimit - projectedUsage;
      return QuotaCheckResult.allowed(
          metricType,
          currentUsage,
          requestedQuantity,
          limit,
          remainingQuota
      );
    }

    // Quota exceeded
    return QuotaCheckResult.denied(
        metricType,
        currentUsage,
        requestedQuantity,
        limit,
        String.format(
            "Quota exceeded for %s. Current: %d, Requested: %d, Limit: %d",
            metricType.name(),
            currentUsage,
            requestedQuantity,
            limit
        )
    );
  }

  /**
   * Enforces quota limits by throwing an exception if the quota is exceeded.
   * 
   * <p>This method performs the same check as {@link #checkQuota} but throws
   * an exception instead of returning a result. Useful for enforcing hard limits.
   * 
   * @param subscription the subscription to check
   * @param metricType the type of metric being checked
   * @param currentUsage the current usage amount in the billing period
   * @param requestedQuantity the amount being requested
   * @throws QuotaExceededException if the quota is exceeded
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public void enforceQuota(
      final Subscription subscription,
      final MetricType metricType,
      final long currentUsage,
      final long requestedQuantity) {

    var result = checkQuota(subscription, metricType, currentUsage, requestedQuantity);

    if (!result.allowed()) {
      throw new QuotaExceededException(
          result.message(),
          metricType.name(),
          result.limit(),
          result.currentUsage()
      );
    }
  }

  /**
   * Checks if a subscription has any quota available for a metric type.
   * 
   * <p>Returns true if the subscription has any remaining quota (including grace period)
   * or if the quota is unlimited.
   * 
   * @param subscription the subscription to check
   * @param metricType the type of metric being checked
   * @param currentUsage the current usage amount in the billing period
   * @return true if quota is available
   */
  public boolean hasQuotaAvailable(
      final Subscription subscription,
      final MetricType metricType,
      final long currentUsage) {

    var result = checkQuota(subscription, metricType, currentUsage, 1);
    return result.allowed();
  }

  /**
   * Calculates the remaining quota for a metric type.
   * 
   * <p>Returns the amount of quota remaining before the limit is reached.
   * Returns Long.MAX_VALUE for unlimited quotas.
   * 
   * @param subscription the subscription to check
   * @param metricType the type of metric being checked
   * @param currentUsage the current usage amount in the billing period
   * @return the remaining quota amount
   */
  public long getRemainingQuota(
      final Subscription subscription,
      final MetricType metricType,
      final long currentUsage) {

    validateSubscription(subscription);
    validateMetricType(metricType);
    validateCurrentUsage(currentUsage);

    var plan = subscription.getPlan();
    var quotas = plan.getQuotas();
    var limit = getQuotaLimit(quotas, metricType);

    // If unlimited, return max value
    if (limit == null) {
      return Long.MAX_VALUE;
    }

    // Calculate remaining quota
    var remaining = limit - currentUsage;
    return Math.max(0, remaining);
  }

  /**
   * Checks if a subscription is approaching its quota limit.
   * 
   * <p>Returns true if current usage is at or above 90% of the quota limit.
   * Useful for sending warning notifications to customers.
   * 
   * @param subscription the subscription to check
   * @param metricType the type of metric being checked
   * @param currentUsage the current usage amount in the billing period
   * @return true if approaching limit (>= 90%)
   */
  public boolean isApproachingLimit(
      final Subscription subscription,
      final MetricType metricType,
      final long currentUsage) {

    validateSubscription(subscription);
    validateMetricType(metricType);
    validateCurrentUsage(currentUsage);

    var plan = subscription.getPlan();
    var quotas = plan.getQuotas();
    var limit = getQuotaLimit(quotas, metricType);

    // If unlimited, never approaching limit
    if (limit == null) {
      return false;
    }

    // Check if at or above 90% of limit
    var threshold = limit * 0.9;
    return currentUsage >= threshold;
  }

  /**
   * Calculates the percentage of quota used.
   * 
   * <p>Returns a value between 0 and 100+ indicating the percentage of the
   * quota that has been consumed. Returns 0 for unlimited quotas.
   * 
   * @param subscription the subscription to check
   * @param metricType the type of metric being checked
   * @param currentUsage the current usage amount in the billing period
   * @return the percentage used (0-100+)
   */
  public double getQuotaUsagePercentage(
      final Subscription subscription,
      final MetricType metricType,
      final long currentUsage) {

    validateSubscription(subscription);
    validateMetricType(metricType);
    validateCurrentUsage(currentUsage);

    var plan = subscription.getPlan();
    var quotas = plan.getQuotas();
    var limit = getQuotaLimit(quotas, metricType);

    // If unlimited, return 0%
    if (limit == null || limit == 0) {
      return 0.0;
    }

    return (currentUsage * 100.0) / limit;
  }

  // Private helper methods

  private void validateSubscription(Subscription subscription) {
    if (subscription == null) {
      throw new IllegalArgumentException("Subscription cannot be null");
    }
    if (subscription.getPlan() == null) {
      throw new IllegalArgumentException("Subscription must have a plan");
    }
  }

  private void validateMetricType(MetricType metricType) {
    if (metricType == null) {
      throw new IllegalArgumentException("Metric type cannot be null");
    }
  }

  private void validateCurrentUsage(long currentUsage) {
    if (currentUsage < 0) {
      throw new IllegalArgumentException("Current usage cannot be negative");
    }
  }

  private void validateRequestedQuantity(long requestedQuantity) {
    if (requestedQuantity < 0) {
      throw new IllegalArgumentException("Requested quantity cannot be negative");
    }
  }

  private Long getQuotaLimit(PlanQuotas quotas, MetricType metricType) {
    if (quotas == null) {
      return null; // Unlimited
    }

    return switch (metricType) {
      case API_CALLS -> quotas.apiCallsPerMonth();
      case STORAGE_GB -> quotas.storageGb();
      case EMAIL_SENDS -> quotas.emailSendsPerMonth();
      case CAMPAIGN_EXECUTIONS -> quotas.campaignExecutionsPerMonth();
      case SCORING_REQUESTS -> quotas.scoringRequestsPerMonth();
      case ACTIVE_USERS -> quotas.maxUsers();
      case CUSTOM_DOMAINS -> quotas.customDomains();
      case DATA_EXPORTS -> quotas.dataExportsPerMonth();
      case CUSTOM -> null; // Custom metrics handled separately
    };
  }

  private long calculateGraceLimit(long limit) {
    return (long) (limit * (1.0 + GRACE_PERIOD_PERCENTAGE));
  }

  /**
   * Exception thrown when a quota is exceeded.
   */
  public static class QuotaExceededException extends RuntimeException {
    private final String metricType;
    private final Long limit;
    private final long currentUsage;

    public QuotaExceededException(
        String message,
        String metricType,
        Long limit,
        long currentUsage) {
      super(message);
      this.metricType = metricType;
      this.limit = limit;
      this.currentUsage = currentUsage;
    }

    public String getMetricType() {
      return metricType;
    }

    public Long getLimit() {
      return limit;
    }

    public long getCurrentUsage() {
      return currentUsage;
    }
  }
}
