package com.iqscaffold.billingservice.usage;

import com.iqscaffold.billingservice.shared.specification.Specification;

/**
 * Specification to check if usage has exceeded the quota limit.
 * 
 * <p>A quota is considered exceeded if the current usage is greater than or equal
 * to the quota limit. This specification is used for quota enforcement and
 * validation before allowing operations that consume resources.
 * 
 * <p>Special cases:
 * <ul>
 *   <li>Unlimited quotas (Long.MAX_VALUE) are never exceeded</li>
 *   <li>Zero quotas are always exceeded (unless usage is also zero)</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * UsageContext context = new UsageContext(MetricType.API_CALLS, 1050L, 1000L);
 * Specification<UsageContext> quotaSpec = new QuotaExceededSpecification();
 * 
 * if (quotaSpec.isSatisfiedBy(context)) {
 *   throw new QuotaExceededException("API call quota exceeded");
 * }
 * }</pre>
 * 
 * @see UsageContext
 * @see MetricType
 */
public class QuotaExceededSpecification implements Specification<UsageContext> {

  /**
   * Checks if the usage has exceeded the quota limit.
   * 
   * <p>Returns true if:
   * <ul>
   *   <li>Current usage is greater than or equal to quota limit</li>
   *   <li>Quota is not unlimited (Long.MAX_VALUE)</li>
   * </ul>
   * 
   * @param context the usage context to evaluate
   * @return true if quota is exceeded, false otherwise
   * @throws IllegalArgumentException if context is null
   */
  @Override
  public boolean isSatisfiedBy(final UsageContext context) {
    if (context == null) {
      throw new IllegalArgumentException("Usage context cannot be null");
    }

    // Unlimited quotas are never exceeded
    if (context.isUnlimited()) {
      return false;
    }

    return context.currentUsage() >= context.quotaLimit();
  }
}
