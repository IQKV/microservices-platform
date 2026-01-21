package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service demonstrating optimal usage of entity graphs for Subscription operations.
 * 
 * <p>This service showcases how to leverage entity graphs to optimize query performance
 * for subscription-related operations by eagerly loading specific relationships based
 * on business requirements.
 *
 * <h3>Entity Graph Usage Patterns</h3>
 * <ul>
 *   <li><strong>Billing Operations</strong> - Load subscription with plan for billing calculations</li>
 *   <li><strong>Feature Access</strong> - Load subscription with plan and features for access control</li>
 *   <li><strong>Plan Management</strong> - Load plans with features for feature configuration</li>
 * </ul>
 *
 * <h3>Performance Benefits</h3>
 * <ul>
 *   <li>Eliminates N+1 query problems in subscription feature checks</li>
 *   <li>Reduces database round trips for billing operations</li>
 *   <li>Optimizes memory usage by loading only needed associations</li>
 *   <li>Leverages Hibernate second-level cache effectively</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionEntityGraphService {

  private final TenantSubscriptionRepository tenantSubscriptionRepository;
  private final SubscriptionPlanRepository subscriptionPlanRepository;

  public SubscriptionEntityGraphService(
      final TenantSubscriptionRepository tenantSubscriptionRepository,
      final SubscriptionPlanRepository subscriptionPlanRepository) {
    this.tenantSubscriptionRepository = tenantSubscriptionRepository;
    this.subscriptionPlanRepository = subscriptionPlanRepository;
  }

  /**
   * Find active subscription for billing operations with plan loaded.
   * Use this when you need subscription and plan details for billing calculations.
   *
   * @return Optional containing active subscription with plan if found
   */
  public Optional<TenantSubscription> findActiveSubscriptionForBilling() {
    return tenantSubscriptionRepository.findActiveWithPlan();
  }

  /**
   * Find active subscription for feature access checks with complete feature context.
   * Use this when you need to check feature availability and limits.
   *
   * @return Optional containing subscription with plan and features if found
   */
  public Optional<TenantSubscription> findActiveSubscriptionForFeatureAccess() {
    return tenantSubscriptionRepository.findActiveWithPlanAndFeatures();
  }

  /**
   * Find subscription by ID for billing operations with plan loaded.
   * Use this when displaying subscription details or processing billing.
   *
   * @param subscriptionId the subscription ID
   * @return Optional containing subscription with plan if found
   */
  public Optional<TenantSubscription> findSubscriptionForBilling(final UUID subscriptionId) {
    return tenantSubscriptionRepository.findByIdWithPlan(subscriptionId);
  }

  /**
   * Find subscription by Stripe ID for webhook processing with plan loaded.
   * Use this when processing Stripe webhooks that need subscription context.
   *
   * @param stripeSubscriptionId the Stripe subscription ID
   * @return Optional containing subscription with plan if found
   */
  public Optional<TenantSubscription> findSubscriptionForWebhook(final String stripeSubscriptionId) {
    return tenantSubscriptionRepository.findByStripeSubscriptionIdWithPlan(stripeSubscriptionId);
  }

  /**
   * Find all active plans for plan selection with features loaded.
   * Use this when displaying available plans with their feature sets.
   *
   * @return List of active plans with features loaded
   */
  public List<SubscriptionPlan> findActivePlansWithFeatures() {
    return subscriptionPlanRepository.findByIsActiveTrueWithFeatures();
  }

  /**
   * Find plan by ID for feature management with complete feature context.
   * Use this when managing plan features or displaying detailed plan information.
   *
   * @param planId the plan ID
   * @return Optional containing plan with features and definitions if found
   */
  public Optional<SubscriptionPlan> findPlanForFeatureManagement(final UUID planId) {
    return subscriptionPlanRepository.findByIdWithFeaturesAndDefinitions(planId);
  }

  /**
   * Find plan by Stripe price ID for Stripe integration with features loaded.
   * Use this when processing Stripe events that reference price IDs.
   *
   * @param stripePriceId the Stripe price ID
   * @return Optional containing plan with features if found
   */
  public Optional<SubscriptionPlan> findPlanByStripePriceId(final String stripePriceId) {
    return subscriptionPlanRepository.findByStripePriceIdWithFeatures(stripePriceId);
  }

  /**
   * Get subscription billing summary with optimized loading.
   * Loads subscription with plan for billing display purposes.
   *
   * @return SubscriptionBillingSummary containing billing details, or null if not found
   */
  public SubscriptionBillingSummary getSubscriptionBillingSummary() {
    return tenantSubscriptionRepository.findActiveWithPlan()
        .map(subscription -> new SubscriptionBillingSummary(
            subscription.getId(),
            subscription.getStatus(),
            subscription.getPlan().getName(),
            subscription.getPlan().getAmount(),
            subscription.getPlan().getCurrency(),
            subscription.getPlan().getInterval(),
            subscription.getCurrentPeriodStart(),
            subscription.getCurrentPeriodEnd(),
            subscription.getTrialEnd(),
            subscription.getCancelAt()
        ))
        .orElse(null);
  }

  /**
   * Check if specific feature is enabled for current tenant.
   * Optimized method that loads subscription with plan and features.
   *
   * @param featureKey the feature key to check
   * @return true if feature is enabled, false otherwise
   */
  public boolean isFeatureEnabled(final String featureKey) {
    return tenantSubscriptionRepository.findActiveWithPlanAndFeatures()
        .map(subscription -> subscription.getPlan().getPlanFeatures().stream()
            .anyMatch(planFeature -> 
                planFeature.getFeature().getFeatureKey().equals(featureKey) 
                    && planFeature.isEnabled()))
        .orElse(false);
  }

  /**
   * Get feature quota for current tenant.
   * Loads subscription with complete feature context for quota checking.
   *
   * @param featureKey the feature key to get quota for
   * @return feature quota, or null if feature not found or not quota-based
   */
  public Long getFeatureQuota(final String featureKey) {
    return tenantSubscriptionRepository.findActiveWithPlanAndFeatures()
        .flatMap(subscription -> subscription.getPlan().getPlanFeatures().stream()
            .filter(planFeature -> planFeature.getFeature().getFeatureKey().equals(featureKey))
            .findFirst()
            .map(planFeature -> planFeature.getQuota()))
        .orElse(null);
  }

  /**
   * Data transfer object for subscription billing information.
   */
  public record SubscriptionBillingSummary(
      UUID subscriptionId,
      SubscriptionStatus status,
      String planName,
      java.math.BigDecimal planAmount,
      String currency,
      SubscriptionInterval interval,
      java.time.Instant currentPeriodStart,
      java.time.Instant currentPeriodEnd,
      java.time.Instant trialEnd,
      java.time.Instant cancelAt
  ) {}
}