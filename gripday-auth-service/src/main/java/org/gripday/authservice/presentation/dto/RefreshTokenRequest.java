package org.gripday.authservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for JWT token refresh using Java 21 record.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}