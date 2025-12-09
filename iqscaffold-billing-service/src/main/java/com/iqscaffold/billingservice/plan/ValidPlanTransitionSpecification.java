package com.iqscaffold.billingservice.plan;

import com.iqscaffold.billingservice.shared.specification.Specification;

/**
 * Specification to validate subscription plan transitions (upgrades and downgrades).
 *
 * <p>A plan transition is considered valid if:
 * <ul>
 *   <li>Both plans are active</li>
 *   <li>The transition is not to the same plan</li>
 *   <li>The target plan is not a FREE tier (downgrades to FREE require special handling)</li>
 *   <li>LIFETIME plans cannot be changed to non-LIFETIME plans</li>
 * </ul>
 *
 * <p>This specification encapsulates the business rules for plan changes and ensures
 * that only valid transitions are allowed. It prevents invalid operations like
 * transitioning to the same plan or changing from a lifetime plan.
 *
 * <p>Example usage:
 * <pre>{@code
 * PlanTransition transition = new PlanTransition(currentPlan, newPlan);
 * Specification<PlanTransition> validTransition = new ValidPlanTransitionSpecification();
 *
 * if (!validTransition.isSatisfiedBy(transition)) {
 *   throw new InvalidPlanTransitionException(
 *     "Cannot transition from " + currentPlan.getName() + " to " + newPlan.getName()
 *   );
 * }
 * }</pre>
 *
 * @see PlanTransition
 * @see SubscriptionPlan
 * @see PlanTier
 */
public class ValidPlanTransitionSpecification implements Specification<PlanTransition> {

  /**
   * Checks if the plan transition is valid according to business rules.
   *
   * <p>Validates that:
   * <ol>
   *   <li>Both source and target plans are active</li>
   *   <li>The transition is not to the same plan</li>
   *   <li>The target plan is not FREE tier (requires special cancellation flow)</li>
   *   <li>If source plan is LIFETIME, target must also be LIFETIME</li>
   * </ol>
   *
   * @param transition the plan transition to evaluate
   * @return true if the transition is valid, false otherwise
   * @throws IllegalArgumentException if transition is null
   */
  @Override
  public boolean isSatisfiedBy(final PlanTransition transition) {
    if (transition == null) {
      throw new IllegalArgumentException("Plan transition cannot be null");
    }

    // Both plans must be active
    if (!transition.fromPlan().getActive() || !transition.toPlan().getActive()) {
      return false;
    }

    // Cannot transition to the same plan
    if (transition.isSamePlan()) {
      return false;
    }

    // Cannot downgrade to FREE tier (requires cancellation instead)
    if (transition.toPlan().getTier() == PlanTier.FREE) {
      return false;
    }

    // LIFETIME plans cannot be changed to non-LIFETIME plans
    if (transition.fromPlan().getBillingCycle() == BillingCycle.LIFETIME
        && transition.toPlan().getBillingCycle() != BillingCycle.LIFETIME) {
      return false;
    }

    // All other transitions are valid
    return true;
  }
}
