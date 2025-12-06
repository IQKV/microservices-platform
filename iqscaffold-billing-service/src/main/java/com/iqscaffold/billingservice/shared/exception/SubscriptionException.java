package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when subscription-related operations fail.
 */
public class SubscriptionException extends BillingException {

    public SubscriptionException(String message) {
        super(message);
    }

    public SubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when a subscription is not found.
     */
    public static class SubscriptionNotFoundException extends SubscriptionException {
        
        private final String subscriptionId;

        public SubscriptionNotFoundException(String subscriptionId) {
            super("Subscription not found: " + subscriptionId);
            this.subscriptionId = subscriptionId;
        }

        public SubscriptionNotFoundException(String subscriptionId, Throwable cause) {
            super("Subscription not found: " + subscriptionId, cause);
            this.subscriptionId = subscriptionId;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }
    }

    /**
     * Exception thrown when attempting to create a subscription that already exists.
     */
    public static class SubscriptionAlreadyExistsException extends SubscriptionException {
        
        private final String tenantId;

        public SubscriptionAlreadyExistsException(String tenantId) {
            super("Subscription already exists for tenant: " + tenantId);
            this.tenantId = tenantId;
        }

        public SubscriptionAlreadyExistsException(String tenantId, Throwable cause) {
            super("Subscription already exists for tenant: " + tenantId, cause);
            this.tenantId = tenantId;
        }

        public String getTenantId() {
            return tenantId;
        }
    }

    /**
     * Exception thrown when a subscription state transition is invalid.
     */
    public static class InvalidSubscriptionStateException extends SubscriptionException {
        
        private final String currentState;
        private final String targetState;

        public InvalidSubscriptionStateException(String currentState, String targetState) {
            super("Invalid subscription state transition from " + currentState + " to " + targetState);
            this.currentState = currentState;
            this.targetState = targetState;
        }

        public InvalidSubscriptionStateException(String currentState, String targetState, Throwable cause) {
            super("Invalid subscription state transition from " + currentState + " to " + targetState, cause);
            this.currentState = currentState;
            this.targetState = targetState;
        }

        public String getCurrentState() {
            return currentState;
        }

        public String getTargetState() {
            return targetState;
        }
    }

    /**
     * Exception thrown when a tenant is not eligible for a trial period.
     */
    public static class TrialNotEligibleException extends SubscriptionException {
        
        public TrialNotEligibleException(String message) {
            super(message);
        }

        public TrialNotEligibleException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when a plan does not offer a trial period.
     */
    public static class PlanDoesNotOfferTrialException extends SubscriptionException {
        
        public PlanDoesNotOfferTrialException(String message) {
            super(message);
        }

        public PlanDoesNotOfferTrialException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when a payment method is invalid.
     */
    public static class InvalidPaymentMethodException extends SubscriptionException {
        
        public InvalidPaymentMethodException(String message) {
            super(message);
        }

        public InvalidPaymentMethodException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when a payment method does not belong to the tenant.
     */
    public static class PaymentMethodMismatchException extends SubscriptionException {
        
        public PaymentMethodMismatchException(String message) {
            super(message);
        }

        public PaymentMethodMismatchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
