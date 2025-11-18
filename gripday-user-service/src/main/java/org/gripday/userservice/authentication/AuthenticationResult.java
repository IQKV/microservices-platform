package org.gripday.userservice.authentication;

import java.time.Instant;

import org.gripday.userservice.usermanagement.UserContext;

/**
 * Simple authentication result classes using Java 21 records.
 */
public sealed interface AuthenticationResult
    permits AuthenticationResult.Success, AuthenticationResult.Failure {

  /**
   * Successful authentication result.
   */
  record Success(
      UserContext user,
      String accessToken,
      String refreshToken,
      String correlationId,
      Instant timestamp
  ) implements AuthenticationResult {

  }

  /**
   * Failed authentication result.
   */
  record Failure(
      String reason,
      String errorCode,
      String correlationId,
      Instant timestamp
  ) implements AuthenticationResult {

  }
}
