package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when plan-related operations fail.
 */
public class PlanException extends BillingException {

  public PlanException(String message) {
    super(message);
  }

  public PlanException(String message, Throwable cause) {
    super(message, cause);
  }

  public PlanException(String errorCode, String message) {
    super(errorCode, message);
  }

  public PlanException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  /**
   * Exception thrown when a plan is not found.
   */
  public static class PlanNotFoundException extends PlanException {

    private final String planId;

    public PlanNotFoundException(String planId) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PLAN_NOT_FOUND,
          "Plan not found: " + planId);
      this.planId = planId;
    }

    public PlanNotFoundException(String planId, Throwable cause) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PLAN_NOT_FOUND,
          "Plan not found: " + planId, cause);
      this.planId = planId;
    }

    public String getPlanId() {
      return planId;
    }
  }

  /**
   * Exception thrown when a plan transition is invalid.
   */
  public static class InvalidPlanTransitionException extends PlanException {

    private final String fromPlan;
    private final String toPlan;

    public InvalidPlanTransitionException(String fromPlan, String toPlan) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVALID_PLAN_TRANSITION,
          "Invalid plan transition from " + fromPlan + " to " + toPlan);
      this.fromPlan = fromPlan;
      this.toPlan = toPlan;
    }

    public InvalidPlanTransitionException(String fromPlan, String toPlan, Throwable cause) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVALID_PLAN_TRANSITION,
          "Invalid plan transition from " + fromPlan + " to " + toPlan, cause);
      this.fromPlan = fromPlan;
      this.toPlan = toPlan;
    }

    public String getFromPlan() {
      return fromPlan;
    }

    public String getToPlan() {
      return toPlan;
    }
  }
}
