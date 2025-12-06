package com.iqscaffold.billingservice.shared.exception;

/**
 * Base exception for all billing-related exceptions.
 * 
 * <p>This is the root exception class for the billing service domain.
 * All domain-specific exceptions should extend this class to provide
 * a consistent exception hierarchy.
 */
public class BillingException extends RuntimeException {

    /**
     * Constructs a new billing exception with the specified detail message.
     *
     * @param message the detail message
     */
    public BillingException(String message) {
        super(message);
    }

    /**
     * Constructs a new billing exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}
