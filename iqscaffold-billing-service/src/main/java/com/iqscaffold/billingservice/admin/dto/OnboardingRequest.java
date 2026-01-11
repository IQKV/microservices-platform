package com.iqscaffold.billingservice.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;

/**
 * Request DTO for merchant onboarding with multi-gateway support.
 */
public record OnboardingRequest(
    @NotNull(message = "Organization ID is required")
    Long organizationId,

    @NotNull(message = "Gateway provider is required")
    PaymentGatewayProvider gatewayProvider,

    @NotBlank(message = "Refresh URL is required")
    String refreshUrl,

    @NotBlank(message = "Return URL is required")
    String returnUrl
) {
}
