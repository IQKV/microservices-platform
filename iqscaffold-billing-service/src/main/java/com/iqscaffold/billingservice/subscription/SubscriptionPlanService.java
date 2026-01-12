package com.iqscaffold.billingservice.subscription;

import java.util.List;
import java.util.UUID;

import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing subscription plans.
 * <p>
 * Provides operations for creating, updating, and managing platform-wide subscription plans.
 */
public interface SubscriptionPlanService {

  /**
   * Create a new subscription plan.
   *
   * @param request The plan creation request
   * @return The created plan response
   */
  SubscriptionDtos.PlanResponse createPlan(SubscriptionDtos.UpsertPlanRequest request);

  /**
   * Update an existing subscription plan.
   *
   * @param id      The plan ID
   * @param request The plan update request
   * @return The updated plan response
   */
  SubscriptionDtos.PlanResponse updatePlan(UUID id, SubscriptionDtos.UpsertPlanRequest request);

  /**
   * Get a subscription plan by ID.
   *
   * @param id The plan ID
   * @return The plan response
   */
  SubscriptionDtos.PlanResponse getPlan(UUID id);

  /**
   * Get all active subscription plans.
   *
   * @return List of active plan responses
   */
  List<SubscriptionDtos.PlanResponse> getActivePlans();

  /**
   * Get all subscription plans with pagination.
   *
   * @param pageable Pagination parameters
   * @return Page of plan responses
   */
  Page<SubscriptionDtos.PlanResponse> getAllPlans(Pageable pageable);

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
   */
  void syncPlanWithStripe(UUID id);

  /**
   * Find a plan by Stripe price ID.
   *
   * @param stripePriceId Stripe price ID
   * @return The plan
   */
  SubscriptionPlan findByStripePriceId(String stripePriceId);
}
