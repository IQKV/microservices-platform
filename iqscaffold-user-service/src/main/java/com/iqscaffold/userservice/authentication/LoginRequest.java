package com.iqscaffold.userservice.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for user authentication. Supports login with either username or email with enhanced validation.
 */
@Schema(
    name = "LoginRequest",
    description = "User login credentials for authentication",
    example = """
        {
          "username": "john.doe",
          "password": "securePassword123",
          "rememberMe": true
        }
        """
)
public record LoginRequest(
    @Schema(
        description = "Username or email address for authentication",
        example = "john.doe",
        minLength = 3,
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Username or email is required")
    @Size(min = 3, max = 255, message = "Username or email must be between 3 and 255 characters")
    String username,

    @Schema(
        description = "User password",
        example = "securePassword123",
        minLength = 1,
        maxLength = 100,
        format = "password",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 100, message = "Password must not exceed 100 characters")
    String password,

    @Schema(
        description = "Whether to extend session duration for longer-lived tokens",
        example = "true",
        defaultValue = "false"
    )
    boolean rememberMe
) {

  // Compact constructor for input sanitization
  public LoginRequest {
    // Trim inputs and normalize email case
    username = username != null ? username.trim().toLowerCase(java.util.Locale.ROOT) : null;
    // Note: Don't trim password as it might be intentionally padded
  }
}
