package com.iqscaffold.userservice.authentication;

/**
 * Custom exception for email verification required scenarios.
 *
 * <p>This exception is thrown when a user attempts to authenticate
 * but their email address has not been verified yet. This is part
 * of the security flow to ensure valid email addresses.
 *
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
public class EmailVerificationRequiredException extends RuntimeException {

  public EmailVerificationRequiredException(final String message) {
    super(message);
  }
}
