package com.iqscaffold.userservice.authentication;

import com.iqscaffold.userservice.usermanagement.UserContext;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for JWT token information. Contains access token, refresh token, and user context.
 */
@Schema(
    name = "TokenResponse",
    description = "JWT token response with user information and authentication details"
)
public record TokenResponse(
    @Schema(
        description = "JWT access token for API authentication",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJqb2huLmRvZSJ9...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String accessToken,

    @Schema(
        description = "JWT refresh token for obtaining new access tokens",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidHlwZSI6InJlZnJlc2gifQ...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String refreshToken,

    @Schema(
        description = "Token type identifier",
        example = "Bearer",
        allowableValues = {"Bearer"},
        defaultValue = "Bearer"
    )
    String tokenType,

    @Schema(
        description = "Token expiration time in seconds from issuance",
        example = "900",
        minimum = "1"
    )
    long expiresIn,

    @Schema(
        description = "Authenticated user information and context",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UserContext user,

    @Schema(
        description = "Session identifier for session management",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    String sessionId
) {

  // Compact constructor with default token type
  public TokenResponse(final String accessToken, final String refreshToken, final long expiresIn, final UserContext user) {
    this(accessToken, refreshToken, "Bearer", expiresIn, user, null);
  }

  // Constructor with session ID
  public TokenResponse(final String accessToken, final String refreshToken, final long expiresIn, final UserContext user, final String sessionId) {
    this(accessToken, refreshToken, "Bearer", expiresIn, user, sessionId);
  }
}
