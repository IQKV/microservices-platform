package org.gripday.gatewayservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("InvalidJwtTokenException Tests")
class InvalidJwtTokenExceptionTest {

  @Test
  @DisplayName("Should create exception with expired token error type")
  void shouldCreateExceptionWithExpiredTokenErrorType() {
    // Arrange
    var reason = "Token expired at 2024-01-15T10:30:00Z";
    var errorType = InvalidJwtTokenException.TokenErrorType.EXPIRED;

    // Act
    var exception = new InvalidJwtTokenException(reason, errorType);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exception.getReason()).contains("JWT token has expired");
    assertThat(exception.getReason()).contains(reason);
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
    assertThat(exception.getErrorType()).isEqualTo(errorType);
  }

  @Test
  @DisplayName("Should create exception with malformed token error type")
  void shouldCreateExceptionWithMalformedTokenErrorType() {
    // Arrange
    var reason = "Invalid JWT structure";
    var errorType = InvalidJwtTokenException.TokenErrorType.MALFORMED;

    // Act
    var exception = new InvalidJwtTokenException(reason, errorType);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exception.getReason()).contains("JWT token is malformed");
    assertThat(exception.getReason()).contains(reason);
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
    assertThat(exception.getErrorType()).isEqualTo(errorType);
  }

  @Test
  @DisplayName("Should create exception with invalid signature error type")
  void shouldCreateExceptionWithInvalidSignatureErrorType() {
    // Arrange
    var reason = "Signature verification failed";
    var errorType = InvalidJwtTokenException.TokenErrorType.INVALID_SIGNATURE;

    // Act
    var exception = new InvalidJwtTokenException(reason, errorType);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exception.getReason()).contains("JWT token has invalid signature");
    assertThat(exception.getReason()).contains(reason);
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
    assertThat(exception.getErrorType()).isEqualTo(errorType);
  }

  @Test
  @DisplayName("Should create exception with missing claims error type")
  void shouldCreateExceptionWithMissingClaimsErrorType() {
    // Arrange
    var reason = "Required claim 'sub' is missing";
    var errorType = InvalidJwtTokenException.TokenErrorType.MISSING_CLAIMS;

    // Act
    var exception = new InvalidJwtTokenException(reason, errorType);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exception.getReason()).contains("JWT token is missing required claims");
    assertThat(exception.getReason()).contains(reason);
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
    assertThat(exception.getErrorType()).isEqualTo(errorType);
  }

  @Test
  @DisplayName("Should create exception with default invalid error type")
  void shouldCreateExceptionWithDefaultInvalidErrorType() {
    // Arrange
    var reason = "Token validation failed";

    // Act
    var exception = new InvalidJwtTokenException(reason);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exception.getReason()).contains("JWT token is invalid");
    assertThat(exception.getReason()).contains(reason);
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
    assertThat(exception.getErrorType()).isEqualTo(InvalidJwtTokenException.TokenErrorType.INVALID);
  }

  @Test
  @DisplayName("Should create exception with generic invalid error type")
  void shouldCreateExceptionWithGenericInvalidErrorType() {
    // Arrange
    var reason = "Unknown token error";
    var errorType = InvalidJwtTokenException.TokenErrorType.INVALID;

    // Act
    var exception = new InvalidJwtTokenException(reason, errorType);

    // Assert
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exception.getReason()).contains("JWT token is invalid");
    assertThat(exception.getReasonDetail()).isEqualTo(reason);
    assertThat(exception.getErrorType()).isEqualTo(errorType);
  }
}
