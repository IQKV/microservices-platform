package com.iqscaffold.billingservice.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for upgrading or downgrading a subscription.
 * Used in the customer billing portal for self-service plan changes.
 */
@Schema(description = "Request to upgrade or downgrade a subscription plan")
public record UpgradeDowngradeRequest(
    @Schema(
        description = "Tenant unique identifier",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Tenant ID is required")
    UUID tenantId,

    @Schema(
        description = "New plan code to switch to",
        example = "ENTERPRISE_YEARLY",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "New plan code is required")
    String newPlanCode,

    @Schema(
        description = "Whether to apply the change immediately or at period end",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Immediate flag is required")
    Boolean immediate,

    @Schema(
        description = "Reason for the plan change",
        example = "Need more users and storage capacity"
    )
    String reason,

    @Schema(
        description = "Payment method ID to use for upgrade (if different from default)",
        example = "1"
    )
    Long paymentMethodId
) {
}
