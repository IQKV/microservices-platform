package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TenantSubscription} entities.
 * <p>
 * All queries are automatically scoped to the current tenant's schema.
 * No tenant_id filtering needed - schema isolation provides tenant context.
 */
@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {

  /**
   * Find active subscription for current tenant.
   * Schema routing ensures this only searches current tenant's data.
   *
   * @return Optional active subscription
   */
  @Query("SELECT s FROM TenantSubscription s WHERE s.status = 'ACTIVE'")
  Optional<TenantSubscription> findActive();

  /**
   * Find active subscription by tenant ID (for cross-tenant operations).
   * This method is used by the feature enablement service which may need
   * to check subscriptions across tenant boundaries.
   *
   * @param tenantId the tenant identifier
   * @return Optional active subscription
   */
  @Query("SELECT s FROM TenantSubscription s WHERE s.status = 'ACTIVE'")
  Optional<TenantSubscription> findActiveSubscriptionByTenantId(@Param("tenantId") String tenantId);

  /**
   * Find a subscription by Stripe subscription ID.
   *
   * @param stripeSubscriptionId Stripe subscription ID
   * @return Optional subscription
   */
  Optional<TenantSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

  /**
   * Find subscriptions by organization ID.
   *
   * @param organizationId Organization ID
   * @return List of subscriptions
   */
  List<TenantSubscription> findByOrganizationId(Long organizationId);

  /**
   * Find subscriptions by status.
   *
   * @param status Subscription status
   * @return List of subscriptions
   */
  List<TenantSubscription> findByStatus(SubscriptionStatus status);

  /**
   * Find subscriptions by plan ID.
   *
   * @param planId Plan ID
   * @return List of subscriptions
   */
  @Query("SELECT s FROM TenantSubscription s WHERE s.plan.id = :planId")
  List<TenantSubscription> findByPlanId(@Param("planId") UUID planId);

  /**
   * Check if current tenant has an active subscription.
   *
   * @return true if has active subscription
   */
  @Query("SELECT COUNT(s) > 0 FROM TenantSubscription s WHERE s.status = 'ACTIVE'")
  boolean hasActiveSubscription();

  /**
   * Find all subscriptions that are in trial and trial end is approaching.
   * Note: This query runs in current tenant's schema only.
   *
   * @return List of subscriptions with trial ending soon
   */
  @Query("SELECT s FROM TenantSubscription s WHERE s.status = 'TRIALING' AND s.trialEnd < CURRENT_TIMESTAMP + 3 DAY")
  List<TenantSubscription> findTrialsEndingSoon();

  /**
   * Find all past due subscriptions in current tenant's schema.
   *
   * @return List of past due subscriptions
   */
  @Query("SELECT s FROM TenantSubscription s WHERE s.status = 'PAST_DUE'")
  List<TenantSubscription> findPastDueSubscriptions();
}
