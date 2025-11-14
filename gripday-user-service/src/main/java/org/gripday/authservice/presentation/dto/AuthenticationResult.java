package org.gripday.authservice.presentation.dto;

import java.time.Instant;

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