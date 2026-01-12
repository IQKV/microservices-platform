package com.iqscaffold.billingservice.subscription;

/**
 * Exception thrown when an invalid subscription state transition is attempted.
 */
public class InvalidSubscriptionStateException extends RuntimeException {

  public InvalidSubscriptionStateException(String message) {
    super(message);
  }

  public InvalidSubscriptionStateException(String message, Throwable cause) {
    super(message, cause);
  }
}
