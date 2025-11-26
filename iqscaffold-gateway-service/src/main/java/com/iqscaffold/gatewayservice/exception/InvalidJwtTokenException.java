package com.iqscaffold.gatewayservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when JWT token is invalid or malformed.
 */
public final class InvalidJwtTokenException extends ResponseStatusException {

  private final String reason;
  private final TokenErrorType errorType;

  public InvalidJwtTokenException(final String reason, final TokenErrorType errorType) {
    super(HttpStatus.UNAUTHORIZED, buildMessage(reason, errorType));
    this.reason = reason;
    this.errorType = errorType;
  }

  public InvalidJwtTokenException(final String reason) {
    this(reason, TokenErrorType.INVALID);
  }

  private static String buildMessage(String reason, TokenErrorType errorType) {
    return switch (errorType) {
      case EXPIRED -> "JWT token has expired: " + reason;
      case MALFORMED -> "JWT token is malformed: " + reason;
      case INVALID_SIGNATURE -> "JWT token has invalid signature: " + reason;
      case MISSING_CLAIMS -> "JWT token is missing required claims: " + reason;
      case INVALID -> "JWT token is invalid: " + reason;
    };
  }

  public String getReasonDetail() {
    return reason;
  }

  public TokenErrorType getErrorType() {
    return errorType;
  }

  public enum TokenErrorType {
    EXPIRED,
    MALFORMED,
    INVALID_SIGNATURE,
    MISSING_CLAIMS,
    INVALID
  }
}
