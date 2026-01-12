package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing subscription plans.
 * <p>
 * Provides operations for creating, updating, and managing platform-wide subscription plans.
 */
public interface SubscriptionPlanService {

  /**
   * Create a new subscription plan.
   *
   * @param plan The plan to create
   * @return The created plan
   */
  SubscriptionPlan createPlan(SubscriptionPlan plan);

  /**
   * Update an existing subscription plan.
   *
   * @param id   The plan ID
   * @param plan The updated plan data
   * @return The updated plan
   */
  SubscriptionPlan updatePlan(UUID id, SubscriptionPlan plan);

  /**
   * Get a subscription plan by ID.
   *
   * @param id The plan ID
   * @return The plan
   */
  SubscriptionPlan getPlan(UUID id);

  /**
   * Get all active subscription plans.
   *
   * @return List of active plans
   */
  List<SubscriptionPlan> getActivePlans();

  /**
   * Get all subscription plans.
   *
   * @return List of all plans
   */
  List<SubscriptionPlan> getAllPlans();

  /**
   * Deactivate a subscription plan.
   *
   * @param id The plan ID
   */
  void deactivatePlan(UUID id);

  /**
   * Activate a subscription plan.
   *
   * @param id The plan ID
   */
  void activatePlan(UUID id);

  /**
   * Sync a plan with Stripe (create Product and Price if not exists).
   *
   * @param id The plan ID
   * @return The synchronized plan
   */
  SubscriptionPlan syncPlanWithStripe(UUID id);

  /**
   * Find a plan by Stripe price ID.
   *
   * @param stripePriceId Stripe price ID
   * @return The plan
   */
  SubscriptionPlan findByStripePriceId(String stripePriceId);
}
