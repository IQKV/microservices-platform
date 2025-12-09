package com.iqscaffold.billingservice.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

import com.iqscaffold.billingservice.plan.PlanQuotas;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for updating an existing subscription plan.
 *
 * <p>This record encapsulates all updatable parameters for a subscription plan.
 * Note that plan code, tier, and billing cycle cannot be changed after creation.
 *
 * @param name        display name for the plan
 * @param description detailed description of the plan
 * @param features    feature flags map (e.g., {"api_access": true})
 * @param quotas      resource quotas and limits
 * @param basePrice   price per billing cycle
 * @param currency    currency code (e.g., "USD")
 */
@Schema(description = "Request to update an existing subscription plan")
public record UpdatePlanRequest(
    @Schema(
        description = "Display name for the plan",
        example = "Professional Monthly V2 - Updated",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Plan name is required")
    @Size(max = 100, message = "Plan name must not exceed 100 characters")
    String name,

    @Schema(
        description = "Detailed description of the plan",
        example = "Enhanced professional plan with updated features and pricing"
    )
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @Schema(
        description = "Feature flags map",
        example = """
            {
              "advanced_analytics": true,
              "api_access": true,
              "priority_support": true,
              "custom_branding": true,
              "ai_features": true,
              "white_label": false
            }
            """,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Features map is required")
    Map<String, Object> features,

    @Schema(
        description = "Resource quotas and limits",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Quotas are required")
    @Valid
    PlanQuotas quotas,

    @Schema(
        description = "Base price per billing cycle",
        example = "64.99",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Base price is required")
    @PositiveOrZero(message = "Base price must be non-negative")
    BigDecimal basePrice,

    @Schema(
        description = "Currency code",
        example = "USD",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency
) {
}
