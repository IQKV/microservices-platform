package com.iqscaffold.userservice.tenancy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iqscaffold.userservice.security.ValidPassword;
import com.iqscaffold.userservice.security.ValidUsername;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Self-service tenant signup request combining tenant, organization, and admin user creation.
 * This DTO is used for public tenant provisioning without requiring existing authentication.
 */
@Schema(
    name = "SelfServiceSignupRequest",
    description = "Self-service tenant provisioning request with organization and admin user details",
    example = """
        {
          "organizationName": "ACME Corporation",
          "adminUsername": "john.doe",
          "adminEmail": "john.doe@acme.com",
          "adminPassword": "SecurePassword123!",
          "adminFirstName": "John",
          "adminLastName": "Doe",
          "tenantId": "acme-corp",
          "domain": "acme.example.com"
        }
        """
)
public record SelfServiceSignupRequest(
    @Schema(
        description = "Organization name (will be used to generate tenant ID if not provided)",
        example = "ACME Corporation",
        minLength = 2,
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
    String organizationName,

    @Schema(
        description = "Admin username for the tenant (3-50 characters, alphanumeric and underscores only)",
        example = "john.doe",
        minLength = 3,
        maxLength = 50,
        pattern = "^[a-zA-Z0-9_]+$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Admin username is required")
    @Size(min = 3, max = 50, message = "Admin username must be between 3 and 50 characters")
    @ValidUsername
    String adminUsername,

    @Schema(
        description = "Admin email address for account verification and communication",
        example = "john.doe@acme.com",
        format = "email",
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    @Size(max = 255, message = "Admin email must not exceed 255 characters")
    String adminEmail,

    @Schema(
        description = "Admin password (8-100 characters, must include uppercase, lowercase, number, and special character)",
        example = "SecurePassword123!",
        minLength = 8,
        maxLength = 100,
        format = "password",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Admin password is required")
    @Size(min = 8, max = 100, message = "Admin password must be between 8 and 100 characters")
    @ValidPassword
    String adminPassword,

    @Schema(
        description = "Admin first name",
        example = "John",
        minLength = 1,
        maxLength = 100,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Admin first name is required")
    @Size(min = 1, max = 100, message = "Admin first name must be between 1 and 100 characters")
    String adminFirstName,

    @Schema(
        description = "Admin last name",
        example = "Doe",
        minLength = 1,
        maxLength = 100,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Admin last name is required")
    @Size(min = 1, max = 100, message = "Admin last name must be between 1 and 100 characters")
    String adminLastName,

    @Schema(
        description = "Optional tenant ID (will be auto-generated from organization name if not provided)",
        example = "acme-corp",
        minLength = 3,
        maxLength = 100
    )
    @Size(min = 3, max = 100, message = "Tenant ID must be between 3 and 100 characters")
    String tenantId,

    @Schema(
        description = "Optional custom domain for the tenant",
        example = "acme.example.com",
        maxLength = 255
    )
    @Size(max = 255, message = "Domain must not exceed 255 characters")
    String domain
) {

  // Compact constructor for input normalization and sanitization
  public SelfServiceSignupRequest {
    // Normalize and trim all string inputs
    organizationName = organizationName != null ? organizationName.trim() : null;
    adminUsername = adminUsername != null ? adminUsername.trim() : null;
    adminEmail = adminEmail != null ? adminEmail.trim().toLowerCase(java.util.Locale.ROOT) : null;
    adminFirstName = adminFirstName != null ? adminFirstName.trim() : null;
    adminLastName = adminLastName != null ? adminLastName.trim() : null;

    // Normalize optional fields
    tenantId = tenantId != null && !tenantId.trim().isEmpty() ? tenantId.trim().toLowerCase(java.util.Locale.ROOT) : null;
    domain = domain != null && !domain.trim().isEmpty() ? domain.trim().toLowerCase(java.util.Locale.ROOT) : null;
  }
}
