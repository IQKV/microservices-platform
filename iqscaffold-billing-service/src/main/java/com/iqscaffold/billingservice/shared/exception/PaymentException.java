package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when payment-related operations fail.
 */
public class PaymentException extends BillingException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentException(String errorCode, String message) {
        super(errorCode, message);
    }

    public PaymentException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Exception thrown when a payment fails.
     */
    public static class PaymentFailedException extends PaymentException {
        
        private final String paymentId;
        private final String reason;

        public PaymentFailedException(String paymentId, String reason) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_FAILED, 
                  "Payment failed: " + paymentId + " - " + reason);
            this.paymentId = paymentId;
            this.reason = reason;
        }

        public PaymentFailedException(String paymentId, String reason, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_FAILED, 
                  "Payment failed: " + paymentId + " - " + reason, cause);
            this.paymentId = paymentId;
            this.reason = reason;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Exception thrown when a payment method is not found.
     */
    public static class PaymentMethodNotFoundException extends PaymentException {
        
        private final String paymentMethodId;

        public PaymentMethodNotFoundException(String paymentMethodId) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_METHOD_NOT_FOUND, 
                  "Payment method not found: " + paymentMethodId);
            this.paymentMethodId = paymentMethodId;
        }

        public PaymentMethodNotFoundException(String paymentMethodId, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_METHOD_NOT_FOUND, 
                  "Payment method not found: " + paymentMethodId, cause);
            this.paymentMethodId = paymentMethodId;
        }

        public String getPaymentMethodId() {
            return paymentMethodId;
        }
    }

    /**
     * Exception thrown when a payment method is invalid.
     */
    public static class InvalidPaymentMethodException extends PaymentException {
        
        private final String paymentMethodId;
        private final String reason;

        public InvalidPaymentMethodException(String paymentMethodId, String reason) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVALID_PAYMENT_METHOD, 
                  "Invalid payment method: " + paymentMethodId + " - " + reason);
            this.paymentMethodId = paymentMethodId;
            this.reason = reason;
        }

        public InvalidPaymentMethodException(String paymentMethodId, String reason, Throwable cause) {
            super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVALID_PAYMENT_METHOD, 
                  "Invalid payment method: " + paymentMethodId + " - " + reason, cause);
            this.paymentMethodId = paymentMethodId;
            this.reason = reason;
        }

        public String getPaymentMethodId() {
            return paymentMethodId;
        }

        public String getReason() {
            return reason;
        }
    }
}
