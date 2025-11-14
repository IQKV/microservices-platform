package org.gripday.userservice.presentation.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for email verification status using Java 21 record. Contains user email verification status and related information.
 */
@Schema(
    name = "VerificationStatusResponse",
    description = "Email verification status response with user details"
)
public record VerificationStatusResponse(
    @Schema(
        description = "Email address being checked",
        example = "john.doe@example.com",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email,

    @Schema(
        description = "Whether the email address has been verified",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean emailVerified,

    @Schema(
        description = "Timestamp when the user account was registered",
        example = "2024-01-15T10:30:00",
        format = "date-time",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    LocalDateTime registrationDate,

    @Schema(
        description = "Status message with next steps or information",
        example = "Email verification pending. Please check your inbox.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message
) {

}