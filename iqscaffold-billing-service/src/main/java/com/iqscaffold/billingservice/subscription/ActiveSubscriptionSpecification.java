package com.iqscaffold.billingservice.subscription;

import java.time.LocalDateTime;

import com.iqscaffold.billingservice.shared.specification.Specification;

/**
 * Specification to check if a subscription is active and valid.
 *
 * <p>A subscription is considered active if:
 * <ul>
 *   <li>Status is ACTIVE</li>
 *   <li>Current period end is in the future</li>
 * </ul>
 *
 * <p>This specification encapsulates the business rule for determining whether
 * a subscription provides active access to platform features. It can be used
 * for validation, filtering, and business logic decisions.
 *
 * <p>Example usage:
 * <pre>{@code
 * Specification<Subscription> activeSpec = new ActiveSubscriptionSpecification();
 * if (activeSpec.isSatisfiedBy(subscription)) {
 *   // Grant access to features
 * }
 * }</pre>
 *
 * @see Subscription
 * @see SubscriptionStatus
 */
public class ActiveSubscriptionSpecification implements Specification<Subscription> {

  /**
   * Checks if the subscription is active and valid.
   *
   * <p>Validates that:
   * <ol>
   *   <li>The subscription status is ACTIVE</li>
   *   <li>The current period end date is after the current time</li>
   * </ol>
   *
   * @param subscription the subscription to evaluate
   * @return true if the subscription is active and valid, false otherwise
   * @throws IllegalArgumentException if subscription is null
   */
  @Override
  public boolean isSatisfiedBy(final Subscription subscription) {
    if (subscription == null) {
      throw new IllegalArgumentException("Subscription cannot be null");
    }

    return subscription.getStatus() == SubscriptionStatus.ACTIVE
           && subscription.getCurrentPeriodEnd() != null
           && subscription.getCurrentPeriodEnd().isAfter(LocalDateTime.now());
  }
}
