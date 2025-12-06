package com.iqscaffold.billingservice.payment;

/**
 * Payment status enumeration.
 * Represents the lifecycle states of a payment transaction.
 */
public enum PaymentStatus {
  /**
   * Payment is pending processing.
   */
  PENDING,

  /**
   * Payment was successfully processed.
   */
  SUCCEEDED,

  /**
   * Payment processing failed.
   */
  FAILED,

  /**
   * Payment was refunded (partially or fully).
   */
  REFUNDED
}
