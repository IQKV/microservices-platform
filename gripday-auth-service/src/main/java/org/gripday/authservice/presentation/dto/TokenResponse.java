package org.gripday.authservice.presentation.dto;

/**
 * Response DTO for JWT token information using Java 21 record.
 * Contains access token, refresh token, and user context.
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserContext user
) {
    // Compact constructor with default token type
    public TokenResponse(String accessToken, String refreshToken, long expiresIn, UserContext user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}