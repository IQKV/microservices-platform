package com.iqscaffold.userservice.authentication;

/**
 * Custom exception for authentication errors.
 *
 * <p>This exception is thrown when authentication fails due to various reasons such as:
 * <ul>
 *   <li>Invalid credentials (username/password)</li>
 *   <li>Account disabled or locked</li>
 *   <li>Token validation failures</li>
 *   <li>System errors during authentication</li>
 * </ul>
 *
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
public class AuthenticationException extends RuntimeException {

  public AuthenticationException(final String message) {
    super(message);
  }

  public AuthenticationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
