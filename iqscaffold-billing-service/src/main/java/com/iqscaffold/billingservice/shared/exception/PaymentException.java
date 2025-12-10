package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when payment-related operations fail.
 */
public class PaymentException extends BillingException {

  public PaymentException(final String message) {
    super(message);
  }

  public PaymentException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public PaymentException(final String errorCode, final String message) {
    super(errorCode, message);
  }

  public PaymentException(final String errorCode, final String message, final Throwable cause) {
    super(errorCode, message, cause);
  }

  /**
   * Exception thrown when a payment fails.
   */
  public static class PaymentFailedException extends PaymentException {

    private final String paymentId;
    private final String reason;

    public PaymentFailedException(final String paymentId, final String reason) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_FAILED,
          "Payment failed: " + paymentId + " - " + reason);
      this.paymentId = paymentId;
      this.reason = reason;
    }

    public PaymentFailedException(final String paymentId, final String reason, final Throwable cause) {
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

    public PaymentMethodNotFoundException(final String paymentMethodId) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.PAYMENT_METHOD_NOT_FOUND,
          "Payment method not found: " + paymentMethodId);
      this.paymentMethodId = paymentMethodId;
    }

    public PaymentMethodNotFoundException(final String paymentMethodId, final Throwable cause) {
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

    public InvalidPaymentMethodException(final String paymentMethodId, final String reason) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVALID_PAYMENT_METHOD,
          "Invalid payment method: " + paymentMethodId + " - " + reason);
      this.paymentMethodId = paymentMethodId;
      this.reason = reason;
    }

    public InvalidPaymentMethodException(final String paymentMethodId, final String reason, final Throwable cause) {
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
