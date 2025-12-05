package com.iqscaffold.userservice.infrastructure.messaging;

/**
 * Exception thrown when messaging operations fail
 */
public class MessagingException extends RuntimeException {

  public MessagingException(String message) {
    super(message);
  }

  public MessagingException(String message, Throwable cause) {
    super(message, cause);
  }
}
