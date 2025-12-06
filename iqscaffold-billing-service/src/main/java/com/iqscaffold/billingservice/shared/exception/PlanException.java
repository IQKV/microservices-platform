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

    /**
     * Exception thrown when a plan is not found.
     */
    public static class PlanNotFoundException extends PlanException {
        
        private final String planId;

        public PlanNotFoundException(String planId) {
            super("Plan not found: " + planId);
            this.planId = planId;
        }

        public PlanNotFoundException(String planId, Throwable cause) {
            super("Plan not found: " + planId, cause);
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
            super("Invalid plan transition from " + fromPlan + " to " + toPlan);
            this.fromPlan = fromPlan;
            this.toPlan = toPlan;
        }

        public InvalidPlanTransitionException(String fromPlan, String toPlan, Throwable cause) {
            super("Invalid plan transition from " + fromPlan + " to " + toPlan, cause);
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
