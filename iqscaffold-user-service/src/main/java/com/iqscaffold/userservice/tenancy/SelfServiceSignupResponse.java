package com.iqscaffold.userservice.tenancy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Response DTO for self-service tenant signup operations.
 * Contains information about the created tenant, organization, and admin user.
 */
@Schema(
    name = "SelfServiceSignupResponse",
    description = "Response containing provisioned tenant, organization, and admin user details"
)
public record SelfServiceSignupResponse(
    @Schema(description = "Created tenant ID", example = "acme-corp-a1b2")
    String tenantId,

    @Schema(description = "Organization name", example = "ACME Corporation")
    String organizationName,

    @Schema(description = "Organization database ID", example = "1")
    Long organizationId,

    @Schema(description = "Admin user ID", example = "1")
    Long adminUserId,

    @Schema(description = "Admin username", example = "john.doe")
    String adminUsername,

    @Schema(description = "Admin email", example = "john.doe@acme.com")
    String adminEmail,

    @Schema(description = "Admin first name", example = "John")
    String adminFirstName,

    @Schema(description = "Admin last name", example = "Doe")
    String adminLastName,

    @Schema(description = "Email verification required", example = "true")
    Boolean emailVerificationRequired,

    @Schema(description = "Tenant creation timestamp")
    LocalDateTime createdAt,

    @Schema(description = "Success message with next steps")
    String message,

    @Schema(description = "Additional instructions for the user")
    String nextSteps
) {

  /**
   * Factory method for creating a successful signup response.
   */
  public static SelfServiceSignupResponse success(
      String tenantId,
      String organizationName,
      Long organizationId,
      Long adminUserId,
      String adminUsername,
      String adminEmail,
      String adminFirstName,
      String adminLastName,
      LocalDateTime createdAt) {

    var message = "Tenant provisioned successfully! Your organization '%s' is ready to use.".formatted(organizationName);
    var nextSteps = """
        1. Check your email (%s) for a verification link
        2. Click the verification link to activate your account
        3. Log in with your username (%s) and password
        4. Start inviting team members to your organization
        """.formatted(adminEmail, adminUsername);

    return new SelfServiceSignupResponse(
        tenantId,
        organizationName,
        organizationId,
        adminUserId,
        adminUsername,
        adminEmail,
        adminFirstName,
        adminLastName,
        true,
        createdAt,
        message,
        nextSteps
    );
  }
}
