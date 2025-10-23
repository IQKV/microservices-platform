package org.gripday.authservice.presentation.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for user registration using Java 21 record.
 * Contains user details and registration status information.
 */
public record UserRegistrationResponse(
    Long userId,
    String username,
    String email,
    String firstName,
    String lastName,
    boolean emailVerified,
    LocalDateTime createdAt,
    String message
) {}