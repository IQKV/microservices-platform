package com.iqscaffold.billingservice.shared.exception;

/**
 * Base exception for all billing-related exceptions.
 *
 * <p>This is the root exception class for the billing service domain.
 * All domain-specific exceptions should extend this class to provide
 * a consistent exception hierarchy.
 *
 * <p>Each exception can optionally include an error code for easier
 * identification and handling by clients.
 */
public class BillingException extends RuntimeException {

  private final String errorCode;

  /**
   * Constructs a new billing exception with the specified detail message.
   *
   * @param message the detail message
   */
  public BillingException(String message) {
    super(message);
    this.errorCode = null;
  }

  /**
   * Constructs a new billing exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause   the cause of this exception
   */
  public BillingException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = null;
  }

  /**
   * Constructs a new billing exception with the specified error code and detail message.
   *
   * @param errorCode the error code
   * @param message   the detail message
   */
  public BillingException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Constructs a new billing exception with the specified error code, detail message, and cause.
   *
   * @param errorCode the error code
   * @param message   the detail message
   * @param cause     the cause of this exception
   */
  public BillingException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * Gets the error code associated with this exception.
   *
   * @return the error code, or null if not set
   */
  public String getErrorCode() {
    return errorCode;
  }
}
