package org.gripday.userservice.presentation.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for user registration using Java 21 record. Contains user details and registration status information.
 */
@Schema(
    name = "UserRegistrationResponse",
    description = "User registration response with account details and status"
)
public record UserRegistrationResponse(
    @Schema(
        description = "Unique identifier for the newly created user",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Long userId,

    @Schema(
        description = "Username of the registered user",
        example = "john.doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String username,

    @Schema(
        description = "Email address of the registered user",
        example = "john.doe@example.com",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email,

    @Schema(
        description = "First name of the registered user",
        example = "John",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String firstName,

    @Schema(
        description = "Last name of the registered user",
        example = "Doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String lastName,

    @Schema(
        description = "Whether the user's email address has been verified",
        example = "false",
        defaultValue = "false"
    )
    boolean emailVerified,

    @Schema(
        description = "Timestamp when the user account was created",
        example = "2024-01-15T10:30:00",
        format = "date-time",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    LocalDateTime createdAt,

    @Schema(
        description = "Registration status message with next steps",
        example = "User registered successfully. Please verify your email.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message
) {

}