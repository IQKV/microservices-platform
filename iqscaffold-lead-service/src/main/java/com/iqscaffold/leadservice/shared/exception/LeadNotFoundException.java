package com.iqscaffold.leadservice.shared.exception;

public class LeadNotFoundException extends RuntimeException {
  
  public LeadNotFoundException(final String message) {
    super(message);
  }
  
  public LeadNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
