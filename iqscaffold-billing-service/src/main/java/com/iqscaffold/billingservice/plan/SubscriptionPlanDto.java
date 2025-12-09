package com.iqscaffold.billingservice.plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for SubscriptionPlan.
 * Represents a complete subscription plan with pricing, features, and quotas.
 */
@Schema(description = "Subscription plan with complete pricing, features, and quota information")
public record SubscriptionPlanDto(
    @Schema(description = "Plan unique identifier", example = "1")
    Long id,

    @Schema(description = "Unique plan code", example = "PRO_MONTHLY")
    String planCode,

    @Schema(description = "Plan display name", example = "Professional Monthly")
    String name,

    @Schema(description = "Detailed plan description", example = "Perfect for growing teams with advanced features")
    String description,

    @Schema(description = "Plan tier", example = "PRO", allowableValues = {"FREE", "PRO", "ENTERPRISE"})
    PlanTier tier,

    @Schema(description = "Billing cycle", example = "MONTHLY", allowableValues = {"MONTHLY", "YEARLY", "LIFETIME"})
    BillingCycle billingCycle,

    @Schema(description = "Base price per billing cycle", example = "49.99")
    BigDecimal basePrice,

    @Schema(description = "Currency code", example = "USD")
    String currency,

    @Schema(
        description = "Feature flags and capabilities",
        example = """
            {
              "advanced_analytics": true,
              "custom_branding": true,
              "api_access": true,
              "priority_support": true,
              "sso": false
            }
            """
    )
    Map<String, Object> features,

    @Schema(description = "Resource quotas and limits")
    PlanQuotas quotas,

    @Schema(description = "Trial period duration in days", example = "14")
    Integer trialDays,

    @Schema(description = "Whether the plan is active and available", example = "true")
    Boolean active,

    @Schema(description = "Whether the plan is publicly visible", example = "true")
    Boolean publicPlan,

    @Schema(description = "Plan creation timestamp", example = "2024-01-01T00:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
    LocalDateTime updatedAt
) {
}
