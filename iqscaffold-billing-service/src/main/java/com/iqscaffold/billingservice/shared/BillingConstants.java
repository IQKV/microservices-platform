package com.iqscaffold.billingservice.shared;

public final class BillingConstants {
  private BillingConstants() {
  }

  public static final class PaymentStatus {
    private PaymentStatus() {
    }

    public static final String PENDING = "PENDING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";
    public static final String PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";
    public static final String PROCESSING = "PROCESSING";
  }

  public static final class MDC {
    private MDC() {
    }

    public static final String USER_ID = "user_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String PAYMENT_ID = "payment_id";
  }

  public static final class ErrorKeys {
    private ErrorKeys() {
    }

    public static final String PAYMENT_NOT_FOUND = "payment.not.found";
    public static final String INVALID_STATUS_TRANSITION = "payment.invalid.status.transition";
    public static final String MERCHANT_CONFIG_NOT_FOUND = "merchant.config.not.found";
    public static final String STRIPE_ERROR = "payment.stripe.error";
  }
}
