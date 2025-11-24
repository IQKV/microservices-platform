package org.gripday.userservice.shared.exception;

/**
 * Exception thrown when email verification operations fail.
 * This includes token validation, rate limiting, and email sending failures.
 */
public class EmailVerificationException extends RuntimeException {

  public EmailVerificationException(final String message) {
    super(message);
  }

  public EmailVerificationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception thrown when verification token is invalid or expired.
   */
  public static class InvalidTokenException extends EmailVerificationException {

    public InvalidTokenException(final String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when rate limit for verification emails is exceeded.
   */
  public static class RateLimitExceededException extends EmailVerificationException {

    private final int maxAttempts;
    private final int currentAttempts;

    public RateLimitExceededException(final String message, final int maxAttempts, final int currentAttempts) {
      super(message);
      this.maxAttempts = maxAttempts;
      this.currentAttempts = currentAttempts;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public int getCurrentAttempts() {
      return currentAttempts;
    }
  }

  /**
   * Exception thrown when email is already verified.
   */
  public static class AlreadyVerifiedException extends EmailVerificationException {

    public AlreadyVerifiedException(final String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when email sending fails.
   */
  public static class EmailSendFailedException extends EmailVerificationException {

    public EmailSendFailedException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
