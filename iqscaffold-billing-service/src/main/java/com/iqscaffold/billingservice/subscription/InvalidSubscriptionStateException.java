package com.iqscaffold.billingservice.subscription;

/**
 * Exception thrown when an invalid subscription state transition is attempted.
 */
public class InvalidSubscriptionStateException extends RuntimeException {

  public InvalidSubscriptionStateException(final String message) {
    super(message);
  }

  public InvalidSubscriptionStateException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
