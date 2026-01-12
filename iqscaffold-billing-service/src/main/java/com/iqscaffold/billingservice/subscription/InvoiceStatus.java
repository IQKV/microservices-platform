package com.iqscaffold.billingservice.subscription;

/**
 * Status of a subscription invoice.
 */
public enum InvoiceStatus {
  /**
   * Invoice is in draft state (not yet finalized).
   */
  DRAFT,

  /**
   * Invoice is open and awaiting payment.
   */
  OPEN,

  /**
   * Invoice has been paid in full.
   */
  PAID,

  /**
   * Invoice is void (canceled/invalidated).
   */
  VOID,

  /**
   * Invoice is uncollectible (written off).
   */
  UNCOLLECTIBLE
}
