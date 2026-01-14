package com.iqscaffold.pipelineservice.shared.exception;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate name).
 */
public class ConflictException extends RuntimeException {

  public ConflictException(final String message) {
    super(message);
  }

  public ConflictException(final String resourceName, final String fieldName, final Object fieldValue) {
    super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
  }
}
