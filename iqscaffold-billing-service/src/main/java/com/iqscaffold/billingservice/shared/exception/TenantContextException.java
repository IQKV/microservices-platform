package com.iqscaffold.billingservice.shared.exception;

/**
 * Base exception for tenant context related errors.
 */
public class TenantContextException extends RuntimeException {

  public TenantContextException(final String message) {
    super(message);
  }

  public TenantContextException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception thrown when an invalid tenant ID is provided.
   */
  public static class InvalidTenantIdException extends TenantContextException {
    public InvalidTenantIdException(final String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when tenant context is required but not set.
   */
  public static class MissingTenantContextException extends TenantContextException {
    public MissingTenantContextException(final String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when attempting to access another tenant's data.
   */
  public static class TenantAccessDeniedException extends TenantContextException {
    public TenantAccessDeniedException(final String message) {
      super(message);
    }
  }
}
