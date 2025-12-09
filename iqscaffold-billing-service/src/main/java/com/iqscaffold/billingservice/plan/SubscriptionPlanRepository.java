package com.iqscaffold.billingservice.plan;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for SubscriptionPlan aggregate.
 *
 * <p>This repository provides methods to query and persist subscription plan aggregates.
 * Plans are stored in the public schema and shared across all tenants.
 *
 * <p>Uses text blocks (Java 21) for multi-line JPQL queries to improve readability.
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

  /**
   * Finds a subscription plan by its unique plan code.
   *
   * @param planCode unique plan identifier
   * @return optional containing the plan, or empty if not found
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.planCode = :planCode
      """)
  Optional<SubscriptionPlan> findByPlanCode(@Param("planCode") String planCode);

  /**
   * Finds all active public subscription plans.
   * Returns plans that are both active and publicly visible.
   *
   * @return list of active public plans ordered by tier and price
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.active = true
        AND p.publicPlan = true
      ORDER BY p.tier ASC, p.basePrice ASC
      """)
  List<SubscriptionPlan> findActivePublicPlans();

  /**
   * Finds all active subscription plans (public and private).
   *
   * @return list of active plans ordered by tier and price
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.active = true
      ORDER BY p.tier ASC, p.basePrice ASC
      """)
  List<SubscriptionPlan> findActivePlans();

  /**
   * Finds subscription plans by tier.
   *
   * @param tier plan tier (FREE, PRO, ENTERPRISE)
   * @return list of plans with the specified tier
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.tier = :tier
        AND p.active = true
      ORDER BY p.basePrice ASC
      """)
  List<SubscriptionPlan> findByTier(@Param("tier") PlanTier tier);

  /**
   * Finds subscription plans by billing cycle.
   *
   * @param billingCycle billing frequency (MONTHLY, YEARLY, LIFETIME)
   * @return list of plans with the specified billing cycle
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.billingCycle = :billingCycle
        AND p.active = true
      ORDER BY p.tier ASC, p.basePrice ASC
      """)
  List<SubscriptionPlan> findByBillingCycle(@Param("billingCycle") BillingCycle billingCycle);

  /**
   * Finds subscription plans by tier and billing cycle.
   *
   * @param tier         plan tier
   * @param billingCycle billing frequency
   * @return list of plans matching criteria
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.tier = :tier
        AND p.billingCycle = :billingCycle
        AND p.active = true
      ORDER BY p.basePrice ASC
      """)
  List<SubscriptionPlan> findByTierAndBillingCycle(
      @Param("tier") PlanTier tier,
      @Param("billingCycle") BillingCycle billingCycle
  );

  /**
   * Finds plans with trial periods.
   * Returns plans where trial days > 0.
   *
   * @return list of plans offering trials
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.trialDays > 0
        AND p.active = true
      ORDER BY p.tier ASC, p.basePrice ASC
      """)
  List<SubscriptionPlan> findPlansWithTrial();

  /**
   * Finds free tier plans.
   *
   * @return list of FREE tier plans
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      WHERE p.tier = 'FREE'
        AND p.active = true
      ORDER BY p.createdAt ASC
      """)
  List<SubscriptionPlan> findFreePlans();

  /**
   * Checks if a plan code already exists.
   *
   * @param planCode plan code to check
   * @return true if plan code exists
   */
  @Query("""
      SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
      FROM SubscriptionPlan p
      WHERE p.planCode = :planCode
      """)
  boolean existsByPlanCode(@Param("planCode") String planCode);

  /**
   * Counts active subscriptions for a plan.
   * Used to prevent deletion of plans with active subscriptions.
   *
   * @param planId plan identifier
   * @return number of active subscriptions
   */
  @Query("""
      SELECT COUNT(s) FROM Subscription s
      WHERE s.plan.id = :planId
        AND s.status IN ('ACTIVE', 'TRIAL', 'PAST_DUE')
      """)
  long countActiveSubscriptions(@Param("planId") Long planId);

  /**
   * Finds all plans (active and inactive) for admin purposes.
   *
   * @return list of all plans ordered by tier and creation date
   */
  @Query("""
      SELECT p FROM SubscriptionPlan p
      ORDER BY p.tier ASC, p.createdAt DESC
      """)
  List<SubscriptionPlan> findAllPlans();
}
