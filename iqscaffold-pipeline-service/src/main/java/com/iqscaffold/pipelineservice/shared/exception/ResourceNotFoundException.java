package com.iqscaffold.pipelineservice.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(final String message) {
    super(message);
  }

  public ResourceNotFoundException(final String resourceName, final String fieldName, final Object fieldValue) {
    super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
  }
}
