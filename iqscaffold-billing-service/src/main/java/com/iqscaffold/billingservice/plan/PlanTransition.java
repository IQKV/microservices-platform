package com.iqscaffold.billingservice.plan;

/**
 * Value object representing a subscription plan transition (upgrade or downgrade).
 * 
 * <p>Encapsulates the source and target plans for a plan change operation.
 * This immutable value object is used by specifications and domain services to
 * validate whether a plan transition is allowed according to business rules.
 * 
 * <p>Business rules for plan transitions:
 * <ul>
 *   <li>Cannot transition to the same plan</li>
 *   <li>Can upgrade from any tier to a higher tier</li>
 *   <li>Can downgrade from any tier to a lower tier</li>
 *   <li>Cannot transition from/to inactive plans</li>
 *   <li>Billing cycle changes may have restrictions</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * PlanTransition transition = new PlanTransition(currentPlan, newPlan);
 * ValidPlanTransitionSpecification spec = new ValidPlanTransitionSpecification();
 * 
 * if (!spec.isSatisfiedBy(transition)) {
 *   throw new InvalidPlanTransitionException("Invalid plan transition");
 * }
 * }</pre>
 * 
 * @param fromPlan the current subscription plan
 * @param toPlan the target subscription plan
 */
public record PlanTransition(
    SubscriptionPlan fromPlan,
    SubscriptionPlan toPlan
) {

  /**
   * Creates a new plan transition with validation.
   * 
   * @param fromPlan the current subscription plan
   * @param toPlan the target subscription plan
   * @throws IllegalArgumentException if either plan is null
   */
  public PlanTransition {
    if (fromPlan == null) {
      throw new IllegalArgumentException("From plan cannot be null");
    }
    if (toPlan == null) {
      throw new IllegalArgumentException("To plan cannot be null");
    }
  }

  /**
   * Checks if this is an upgrade (moving to a higher tier).
   * 
   * @return true if moving to a higher tier
   */
  public boolean isUpgrade() {
    return toPlan.getTier().ordinal() > fromPlan.getTier().ordinal();
  }

  /**
   * Checks if this is a downgrade (moving to a lower tier).
   * 
   * @return true if moving to a lower tier
   */
  public boolean isDowngrade() {
    return toPlan.getTier().ordinal() < fromPlan.getTier().ordinal();
  }

  /**
   * Checks if this is a lateral move (same tier, different billing cycle).
   * 
   * @return true if staying in the same tier
   */
  public boolean isLateralMove() {
    return toPlan.getTier() == fromPlan.getTier();
  }

  /**
   * Checks if the transition is to the same plan.
   * 
   * @return true if from and to plans are the same
   */
  public boolean isSamePlan() {
    return fromPlan.getId().equals(toPlan.getId());
  }

  /**
   * Checks if the billing cycle is changing.
   * 
   * @return true if billing cycles are different
   */
  public boolean isBillingCycleChange() {
    return fromPlan.getBillingCycle() != toPlan.getBillingCycle();
  }

  /**
   * Gets the tier difference (positive for upgrade, negative for downgrade).
   * 
   * @return the difference in tier ordinals
   */
  public int tierDifference() {
    return toPlan.getTier().ordinal() - fromPlan.getTier().ordinal();
  }

  /**
   * Gets a human-readable description of the transition type.
   * 
   * @return description of the transition (e.g., "Upgrade", "Downgrade", "Lateral Move")
   */
  public String transitionType() {
    if (isSamePlan()) {
      return "No Change";
    } else if (isUpgrade()) {
      return "Upgrade";
    } else if (isDowngrade()) {
      return "Downgrade";
    } else {
      return "Lateral Move";
    }
  }
}
