package com.iqscaffold.userservice.usermanagement;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating user's preferred locale.
 */
public record UpdateLocaleRequest(
    @NotBlank(message = "Locale is required")
    @ValidLocale
    String locale
) {
}