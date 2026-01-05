package com.iqscaffold.billingservice.infrastructure.messaging;

/**
 * Exception thrown when messaging operations fail
 */
public class MessagingException extends RuntimeException {

  public MessagingException(final String message) {
    super(message);
  }

  public MessagingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}