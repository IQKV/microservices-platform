package org.gripday.authservice.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.gripday.authservice.presentation.validation.ValidPassword;
import org.gripday.authservice.presentation.validation.ValidUsername;

/**
 * Request DTO for user registration using Java 21 record.
 * Contains comprehensive validation annotations for input validation and security.
 */
@Schema(
    name = "SignupRequest",
    description = "User registration request with comprehensive validation",
    example = """
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "password": "SecurePassword123!",
      "firstName": "John",
      "lastName": "Doe",
      "tenantId": "tenant-123"
    }
    """
)
public record SignupRequest(
    @Schema(
        description = "Unique username for the account (3-50 characters, alphanumeric and underscores only)",
        example = "john.doe",
        minLength = 3,
        maxLength = 50,
        pattern = "^[a-zA-Z0-9_]+$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @ValidUsername
    String username,
    
    @Schema(
        description = "Valid email address for account verification and communication",
        example = "john.doe@example.com",
        format = "email",
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,
    
    @Schema(
        description = "Strong password (8-100 characters, must include uppercase, lowercase, number, and special character)",
        example = "SecurePassword123!",
        minLength = 8,
        maxLength = 100,
        format = "password",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @ValidPassword
    String password,
    
    @Schema(
        description = "User's first name",
        example = "John",
        minLength = 1,
        maxLength = 100,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName,
    
    @Schema(
        description = "User's last name",
        example = "Doe",
        minLength = 1,
        maxLength = 100,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName,
    
    @Schema(
        description = "Tenant identifier for multi-tenant isolation (defaults to 'default' if not provided)",
        example = "tenant-123",
        maxLength = 100,
        defaultValue = "default"
    )
    @Size(max = 100, message = "Tenant ID must not exceed 100 characters")
    String tenantId
) {
    // Compact constructor for additional validation and sanitization
    public SignupRequest {
        // Normalize tenant ID - use default if null or empty
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = "default";
        }
        
        // Trim all string inputs to prevent whitespace issues
        username = username != null ? username.trim() : null;
        email = email != null ? email.trim().toLowerCase() : null;
        firstName = firstName != null ? firstName.trim() : null;
        lastName = lastName != null ? lastName.trim() : null;
        tenantId = tenantId.trim();
    }
}