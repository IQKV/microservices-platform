package com.iqscaffold.leadservice.shared.exception;

/**
 * Exception thrown when a lead note is not found.
 */
public class LeadNoteNotFoundException extends RuntimeException {
  
  public LeadNoteNotFoundException(final String message) {
    super(message);
  }
  
  public LeadNoteNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
