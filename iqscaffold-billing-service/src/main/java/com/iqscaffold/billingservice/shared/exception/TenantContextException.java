package com.iqscaffold.billingservice.shared.exception;

/**
 * Exception thrown when tenant context operations fail.
 * This includes invalid tenant ID, missing context, or context manipulation errors.
 */
public class TenantContextException extends RuntimeException {

  public TenantContextException(final String message) {
    super(message);
  }

  public TenantContextException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception thrown when tenant ID is null or empty.
   */
  public static class InvalidTenantIdException extends TenantContextException {

    public InvalidTenantIdException(final String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when tenant context is missing when required.
   */
  public static class MissingTenantContextException extends TenantContextException {

    public MissingTenantContextException(final String message) {
      super(message);
    }
  }
}
