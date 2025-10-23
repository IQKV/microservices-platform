package org.gripday.authservice.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration using Java 21 record.
 * Contains validation annotations for input validation.
 */
public record SignupRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String password,
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,
    
    @Size(max = 100, message = "Tenant ID must not exceed 100 characters")
    String tenantId
) {
    // Compact constructor for additional validation
    public SignupRequest {
        // Normalize tenant ID - use default if null or empty
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = "default";
        }
    }
}