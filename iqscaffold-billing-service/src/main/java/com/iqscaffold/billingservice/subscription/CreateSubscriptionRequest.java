package com.iqscaffold.billingservice.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for creating a new subscription.
 * Contains all required information to create a subscription for a tenant.
 */
@Schema(description = "Request to create a new subscription")
public record CreateSubscriptionRequest(
    @Schema(
        description = "Tenant unique identifier",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Tenant ID is required")
    UUID tenantId,

    @Schema(
        description = "User creating the subscription",
        example = "550e8400-e29b-41d4-a716-446655440001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "User ID is required")
    UUID userId,

    @Schema(
        description = "Subscription plan code",
        example = "PRO_MONTHLY",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Plan code is required")
    String planCode,

    @Schema(
        description = "Whether to start with trial period if available",
        example = "true"
    )
    Boolean startTrial,

    @Schema(
        description = "Payment method ID for paid subscriptions",
        example = "1"
    )
    Long paymentMethodId,

    @Schema(
        description = "Additional metadata for the subscription",
        example = """
            {
              "source": "web_signup",
              "campaign": "summer_promo",
              "referral_code": "FRIEND2024"
            }
            """
    )
    Map<String, Object> metadata
) {
}
