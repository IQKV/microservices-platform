package org.gripday.userservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for JWT token refresh using Java 21 record.
 */
@Schema(
    name = "RefreshTokenRequest",
    description = "Request to refresh JWT access token using refresh token",
    example = """
        {
          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        }
        """
)
public record RefreshTokenRequest(
    @Schema(
        description = "Valid JWT refresh token obtained from login or previous refresh",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidHlwZSI6InJlZnJlc2gifQ...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {

}