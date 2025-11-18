package org.gripday.userservice.emailverification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for resending email verification. Contains email address with validation constraints.
 */
@Schema(
    name = "ResendVerificationRequest",
    description = "Request to resend email verification link",
    example = """
        {
          "email": "john.doe@example.com"
        }
        """
)
public record ResendVerificationRequest(
    @Schema(
        description = "Email address to resend verification link to",
        example = "john.doe@example.com",
        format = "email",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email
) {

  // Compact constructor for input sanitization
  public ResendVerificationRequest {
    // Trim and normalize email case
    email = email != null ? email.trim().toLowerCase(java.util.Locale.ROOT) : null;
  }
}
