package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SubscriptionPlan} entities.
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

  /**
   * Find all active subscription plans.
   *
   * @return List of active plans
   */
  List<SubscriptionPlan> findByIsActiveTrue();

  /**
   * Find a plan by its Stripe price ID.
   *
   * @param stripePriceId Stripe price ID
   * @return Optional plan
   */
  Optional<SubscriptionPlan> findByStripePriceId(String stripePriceId);

  /**
   * Find a plan by its Stripe product ID.
   *
   * @param stripeProductId Stripe product ID
   * @return Optional plan
   */
  Optional<SubscriptionPlan> findByStripeProductId(String stripeProductId);

  /**
   * Check if a plan with the given name exists.
   *
   * @param name Plan name
   * @return true if exists
   */
  boolean existsByName(String name);

  /**
   * Find plans by currency.
   *
   * @param currency Currency code
   * @return List of plans
   */
  List<SubscriptionPlan> findByCurrency(String currency);

  /**
   * Find active plans by interval.
   *
   * @param interval Billing interval
   * @return List of plans
   */
  @Query("SELECT p FROM SubscriptionPlan p WHERE p.interval = :interval AND p.isActive = true")
  List<SubscriptionPlan> findActiveByInterval(SubscriptionInterval interval);
}
