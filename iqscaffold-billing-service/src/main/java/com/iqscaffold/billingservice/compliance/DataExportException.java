package com.iqscaffold.billingservice.compliance;

/**
 * Exception thrown when data export fails.
 */
public class DataExportException extends RuntimeException {

  public DataExportException(final String message) {
    super(message);
  }

  public DataExportException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
