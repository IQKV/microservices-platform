package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when invoice-related operations fail.
 */
public class InvoiceException extends BillingException {

  public InvoiceException(final String message) {
    super(message);
  }

  public InvoiceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public InvoiceException(final String errorCode, final String message) {
    super(errorCode, message);
  }

  public InvoiceException(final String errorCode, final String message, final Throwable cause) {
    super(errorCode, message, cause);
  }

  /**
   * Exception thrown when an invoice is not found.
   */
  public static class InvoiceNotFoundException extends InvoiceException {

    private final String invoiceId;

    public InvoiceNotFoundException(final String invoiceId) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVOICE_NOT_FOUND,
          "Invoice not found: " + invoiceId);
      this.invoiceId = invoiceId;
    }

    public InvoiceNotFoundException(final String invoiceId, final Throwable cause) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVOICE_NOT_FOUND,
          "Invoice not found: " + invoiceId, cause);
      this.invoiceId = invoiceId;
    }

    public String getInvoiceId() {
      return invoiceId;
    }
  }

  /**
   * Exception thrown when attempting to modify an invoice that is already paid.
   */
  public static class InvoiceAlreadyPaidException extends InvoiceException {

    private final String invoiceId;

    public InvoiceAlreadyPaidException(final String invoiceId) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVOICE_ALREADY_PAID,
          "Invoice already paid: " + invoiceId);
      this.invoiceId = invoiceId;
    }

    public InvoiceAlreadyPaidException(final String invoiceId, final Throwable cause) {
      super(com.iqscaffold.billingservice.shared.BillingConstants.ErrorCodes.INVOICE_ALREADY_PAID,
          "Invoice already paid: " + invoiceId, cause);
      this.invoiceId = invoiceId;
    }

    public String getInvoiceId() {
      return invoiceId;
    }
  }

  /**
   * Exception thrown when subscription is not found for invoice operations.
   */
  public static class SubscriptionNotFoundException extends InvoiceException {

    public SubscriptionNotFoundException(final String message) {
      super(message);
    }

    public SubscriptionNotFoundException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Exception thrown when invoice is in an invalid state for the requested operation.
   */
  public static class InvalidInvoiceStateException extends InvoiceException {

    private final String currentState;
    private final String requiredState;

    public InvalidInvoiceStateException(final String message, final String currentState, final String requiredState) {
      super(message);
      this.currentState = currentState;
      this.requiredState = requiredState;
    }

    public String getCurrentState() {
      return currentState;
    }

    public String getRequiredState() {
      return requiredState;
    }
  }
}
