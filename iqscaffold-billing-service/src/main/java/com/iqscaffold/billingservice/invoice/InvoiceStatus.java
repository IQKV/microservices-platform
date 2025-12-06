package com.iqscaffold.billingservice.invoice;

/**
 * Enumeration of possible invoice statuses in the billing lifecycle.
 * 
 * <p>Invoice status transitions follow a specific lifecycle:
 * <ul>
 *   <li>DRAFT → OPEN (when finalized)</li>
 *   <li>OPEN → PAID (when payment succeeds)</li>
 *   <li>OPEN → VOID (when voided by admin)</li>
 *   <li>OPEN → UNCOLLECTIBLE (when payment fails permanently)</li>
 * </ul>
 * 
 * <p>Status meanings:
 * <ul>
 *   <li>DRAFT: Invoice is being prepared, not yet finalized</li>
 *   <li>OPEN: Invoice is finalized and awaiting payment</li>
 *   <li>PAID: Invoice has been paid successfully</li>
 *   <li>VOID: Invoice has been voided and will not be collected</li>
 *   <li>UNCOLLECTIBLE: Invoice payment failed permanently after retries</li>
 * </ul>
 */
public enum InvoiceStatus {
  /**
   * Invoice is being prepared and can still be modified.
   * Not yet sent to customer.
   */
  DRAFT,

  /**
   * Invoice is finalized and awaiting payment.
   * Customer has been notified.
   */
  OPEN,

  /**
   * Invoice has been paid successfully.
   * No further action needed.
   */
  PAID,

  /**
   * Invoice has been voided by administrator.
   * Will not be collected.
   */
  VOID,

  /**
   * Invoice payment failed permanently after all retry attempts.
   * Requires manual intervention.
   */
  UNCOLLECTIBLE;

  /**
   * Checks if the invoice can be modified.
   * Only DRAFT invoices can be modified.
   * 
   * @return true if invoice can be modified
   */
  public boolean canModify() {
    return this == DRAFT;
  }

  /**
   * Checks if the invoice can be finalized.
   * Only DRAFT invoices can be finalized.
   * 
   * @return true if invoice can be finalized
   */
  public boolean canFinalize() {
    return this == DRAFT;
  }

  /**
   * Checks if the invoice can be paid.
   * Only OPEN invoices can be paid.
   * 
   * @return true if invoice can be paid
   */
  public boolean canPay() {
    return this == OPEN;
  }

  /**
   * Checks if the invoice can be voided.
   * Only DRAFT and OPEN invoices can be voided.
   * 
   * @return true if invoice can be voided
   */
  public boolean canVoid() {
    return this == DRAFT || this == OPEN;
  }

  /**
   * Checks if the invoice is in a final state.
   * PAID, VOID, and UNCOLLECTIBLE are final states.
   * 
   * @return true if invoice is in a final state
   */
  public boolean isFinal() {
    return this == PAID || this == VOID || this == UNCOLLECTIBLE;
  }

  /**
   * Checks if the invoice requires payment.
   * Only OPEN invoices require payment.
   * 
   * @return true if invoice requires payment
   */
  public boolean requiresPayment() {
    return this == OPEN;
  }
}
