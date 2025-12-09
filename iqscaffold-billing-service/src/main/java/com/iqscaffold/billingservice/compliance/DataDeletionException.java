package com.iqscaffold.billingservice.compliance;

/**
 * Exception thrown when data deletion fails.
 */
public class DataDeletionException extends RuntimeException {
    
    public DataDeletionException(String message) {
        super(message);
    }
    
    public DataDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
