package org.gripday.userservice.presentation.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for email verification operations using Java 21 record. Contains verification status and user information.
 */
@Schema(
    name = "EmailVerificationResponse",
    description = "Email verification response with status and user details"
)
public record EmailVerificationResponse(
    @Schema(
        description = "Whether the email verification was successful",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean success,

    @Schema(
        description = "Status message describing the verification result",
        example = "Email verified successfully",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message,

    @Schema(
        description = "Username of the verified user",
        example = "john.doe"
    )
    String username,

    @Schema(
        description = "Timestamp when the email was verified",
        example = "2024-01-15T10:30:00",
        format = "date-time"
    )
    LocalDateTime verifiedAt
) {

}