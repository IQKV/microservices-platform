package com.iqscaffold.pipelineservice.shared.exception;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessException extends RuntimeException {

  public BusinessException(final String message) {
    super(message);
  }
}
