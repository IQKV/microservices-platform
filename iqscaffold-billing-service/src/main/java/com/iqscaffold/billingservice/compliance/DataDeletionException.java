package com.iqscaffold.billingservice.compliance;

/**
 * Exception thrown when data deletion fails.
 */
public class DataDeletionException extends RuntimeException {

  public DataDeletionException(final String message) {
    super(message);
  }

  public DataDeletionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
