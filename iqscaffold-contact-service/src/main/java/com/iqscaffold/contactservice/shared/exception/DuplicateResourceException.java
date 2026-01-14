package com.iqscaffold.contactservice.shared.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Results in HTTP 409 Conflict response.
 */
public class DuplicateResourceException extends RuntimeException {

  public DuplicateResourceException(final String message) {
    super(message);
  }

  public DuplicateResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
