package com.iqscaffold.billingservice.shared.exception;

/**
 * Base exception for tenant context related errors.
 */
public class TenantContextException extends RuntimeException {

  public TenantContextException(String message) {
    super(message);
  }

  public TenantContextException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception thrown when an invalid tenant ID is provided.
   */
  public static class InvalidTenantIdException extends TenantContextException {
    public InvalidTenantIdException(String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when tenant context is required but not set.
   */
  public static class MissingTenantContextException extends TenantContextException {
    public MissingTenantContextException(String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when attempting to access another tenant's data.
   */
  public static class TenantAccessDeniedException extends TenantContextException {
    public TenantAccessDeniedException(String message) {
      super(message);
    }
  }
}
