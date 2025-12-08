package com.iqscaffold.userservice.tenancy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating tenant subscription information from billing service.
 * This is used by the internal billing integration endpoint.
 */
public record TenantSubscriptionUpdateRequest(
    @NotNull(message = "Subscription ID is required")
    Long subscriptionId,

    @NotBlank(message = "Subscription status is required")
    String subscriptionStatus,

    @NotBlank(message = "Subscription plan code is required")
    String subscriptionPlanCode
) {
}
