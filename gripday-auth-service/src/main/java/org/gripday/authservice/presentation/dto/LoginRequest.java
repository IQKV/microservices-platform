package org.gripday.authservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user authentication using Java 21 record.
 * Supports login with either username or email with enhanced validation.
 */
public record LoginRequest(
    @NotBlank(message = "Username or email is required")
    @Size(min = 3, max = 255, message = "Username or email must be between 3 and 255 characters")
    String username,
    
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 100, message = "Password must not exceed 100 characters")
    String password,
    
    boolean rememberMe
) {
    // Compact constructor for input sanitization
    public LoginRequest {
        // Trim inputs and normalize email case
        username = username != null ? username.trim().toLowerCase() : null;
        // Note: Don't trim password as it might be intentionally padded
    }
}