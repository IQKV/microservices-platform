package com.iqscaffold.billingservice.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for merchant onboarding.
 */
public record OnboardingRequest(
    @NotNull(message = "Organization ID is required")
    Long organizationId,

    @NotBlank(message = "Refresh URL is required")
    String refreshUrl,

    @NotBlank(message = "Return URL is required")
    String returnUrl
) {
}
