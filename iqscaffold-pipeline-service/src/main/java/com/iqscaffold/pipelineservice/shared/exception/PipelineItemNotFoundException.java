package com.iqscaffold.pipelineservice.shared.exception;

/**
 * Exception thrown when a pipeline item is not found.
 */
public class PipelineItemNotFoundException extends ResourceNotFoundException {

  public PipelineItemNotFoundException(final String message) {
    super(message);
  }

  public PipelineItemNotFoundException(final String fieldName, final Object fieldValue) {
    super("PipelineItem", fieldName, fieldValue);
  }
}