package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when invoice-related operations fail.
 */
public class InvoiceException extends BillingException {

    public InvoiceException(String message) {
        super(message);
    }

    public InvoiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when an invoice is not found.
     */
    public static class InvoiceNotFoundException extends InvoiceException {
        
        private final String invoiceId;

        public InvoiceNotFoundException(String invoiceId) {
            super("Invoice not found: " + invoiceId);
            this.invoiceId = invoiceId;
        }

        public InvoiceNotFoundException(String invoiceId, Throwable cause) {
            super("Invoice not found: " + invoiceId, cause);
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

        public InvoiceAlreadyPaidException(String invoiceId) {
            super("Invoice already paid: " + invoiceId);
            this.invoiceId = invoiceId;
        }

        public InvoiceAlreadyPaidException(String invoiceId, Throwable cause) {
            super("Invoice already paid: " + invoiceId, cause);
            this.invoiceId = invoiceId;
        }

        public String getInvoiceId() {
            return invoiceId;
        }
    }
}
