package com.iqscaffold.billingservice.subscription;

import com.iqscaffold.billingservice.shared.specification.Specification;

/**
 * Specification to check if a tenant is eligible for a trial period.
 * 
 * <p>A tenant is considered eligible for a trial if they have never previously
 * used a trial period. This enforces the business rule that each tenant receives
 * only one trial period per lifetime.
 * 
 * <p>Business rules:
 * <ul>
 *   <li>Each tenant is eligible for exactly one trial period</li>
 *   <li>Once a trial has been used (even if canceled), the tenant cannot start another</li>
 *   <li>Trial eligibility is checked before creating trial subscriptions</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * TenantTrialHistory history = trialHistoryRepository.findByTenantId(tenantId);
 * Specification<TenantTrialHistory> eligibilitySpec = new TrialEligibilitySpecification();
 * 
 * if (!eligibilitySpec.isSatisfiedBy(history)) {
 *   throw new TrialNotEligibleException("Tenant has already used their trial period");
 * }
 * 
 * // Proceed with trial subscription creation
 * }</pre>
 * 
 * @see TenantTrialHistory
 * @see Subscription
 */
public class TrialEligibilitySpecification implements Specification<TenantTrialHistory> {

  /**
   * Checks if the tenant is eligible for a trial period.
   * 
   * <p>Returns true if:
   * <ul>
   *   <li>The tenant has never used a trial period (hasUsedTrial is false)</li>
   * </ul>
   * 
   * @param history the tenant's trial history to evaluate
   * @return true if the tenant is eligible for a trial, false otherwise
   * @throws IllegalArgumentException if history is null
   */
  @Override
  public boolean isSatisfiedBy(final TenantTrialHistory history) {
    if (history == null) {
      throw new IllegalArgumentException("Tenant trial history cannot be null");
    }

    return !history.hasUsedTrial();
  }
}
