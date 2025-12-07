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

    public SubscriptionException(String errorCode, String message) {
        super(errorCode, message);
    }

    public SubscriptionException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Exception thrown when a subscription is not found.
     */
    public static class SubscriptionNotFoundException extends SubscriptionException {
        
        private final String subscriptionId;

        public SubscriptionNotFoundException(String subscriptionId) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.SUBSCRIPTION_NOT_FOUND, 
                  "Subscription not found: " + subscriptionId);
            this.subscriptionId = subscriptionId;
        }

        public SubscriptionNotFoundException(String subscriptionId, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.SUBSCRIPTION_NOT_FOUND, 
                  "Subscription not found: " + subscriptionId, cause);
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
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.SUBSCRIPTION_ALREADY_EXISTS, 
                  "Subscription already exists for tenant: " + tenantId);
            this.tenantId = tenantId;
        }

        public SubscriptionAlreadyExistsException(String tenantId, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.SUBSCRIPTION_ALREADY_EXISTS, 
                  "Subscription already exists for tenant: " + tenantId, cause);
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
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVALID_SUBSCRIPTION_STATE, 
                  "Invalid subscription state transition from " + currentState + " to " + targetState);
            this.currentState = currentState;
            this.targetState = targetState;
        }

        public InvalidSubscriptionStateException(String currentState, String targetState, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVALID_SUBSCRIPTION_STATE, 
                  "Invalid subscription state transition from " + currentState + " to " + targetState, cause);
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

    /**
     * Exception thrown when a feature is not available in the current subscription plan.
     */
    public static class FeatureNotAvailableException extends SubscriptionException {
        
        private final String featureName;
        private final String currentPlan;

        public FeatureNotAvailableException(String featureName, String currentPlan) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.FEATURE_NOT_AVAILABLE, 
                  "Feature '" + featureName + "' is not available in plan: " + currentPlan);
            this.featureName = featureName;
            this.currentPlan = currentPlan;
        }

        public FeatureNotAvailableException(String featureName, String currentPlan, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.FEATURE_NOT_AVAILABLE, 
                  "Feature '" + featureName + "' is not available in plan: " + currentPlan, cause);
            this.featureName = featureName;
            this.currentPlan = currentPlan;
        }

        public String getFeatureName() {
            return featureName;
        }

        public String getCurrentPlan() {
            return currentPlan;
        }
    }

    /**
     * Exception thrown when a payment is required to perform an operation.
     */
    public static class PaymentRequiredException extends SubscriptionException {
        
        private final String operation;
        private final String reason;

        public PaymentRequiredException(String operation, String reason) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_REQUIRED, 
                  "Payment required for operation '" + operation + "': " + reason);
            this.operation = operation;
            this.reason = reason;
        }

        public PaymentRequiredException(String operation, String reason, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_REQUIRED, 
                  "Payment required for operation '" + operation + "': " + reason, cause);
            this.operation = operation;
            this.reason = reason;
        }

        public String getOperation() {
            return operation;
        }

        public String getReason() {
            return reason;
        }
    }
}
