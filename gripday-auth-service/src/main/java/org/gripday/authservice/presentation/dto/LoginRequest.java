package org.gripday.authservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user authentication using Java 21 record.
 * Supports login with either username or email.
 */
public record LoginRequest(
    @NotBlank(message = "Username or email is required")
    String username,
    
    @NotBlank(message = "Password is required")
    String password,
    
    boolean rememberMe
) {}