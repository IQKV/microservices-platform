package com.iqscaffold.billingservice.compliance;

/**
 * Exception thrown when data export fails.
 */
public class DataExportException extends RuntimeException {
    
    public DataExportException(String message) {
        super(message);
    }
    
    public DataExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
