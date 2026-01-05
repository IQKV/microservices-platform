package com.iqscaffold.userservice.usermanagement;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Request DTO for updating users by admin. Contains validation annotations for input validation.
 */
public record UpdateUserRequest(
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @Email(message = "Email must be valid")
    String email,

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String password,

    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,

    Boolean enabled,
    Boolean emailVerified,
    Set<String> authorities
) {
}
