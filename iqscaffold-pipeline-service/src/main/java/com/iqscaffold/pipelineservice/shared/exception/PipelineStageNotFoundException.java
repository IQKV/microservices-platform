package com.iqscaffold.pipelineservice.shared.exception;

/**
 * Exception thrown when a pipeline stage is not found.
 */
public class PipelineStageNotFoundException extends ResourceNotFoundException {

  public PipelineStageNotFoundException(final String message) {
    super(message);
  }

  public PipelineStageNotFoundException(final String fieldName, final Object fieldValue) {
    super("PipelineStage", fieldName, fieldValue);
  }
}