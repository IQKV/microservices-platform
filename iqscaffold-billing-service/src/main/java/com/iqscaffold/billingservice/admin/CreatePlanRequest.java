package com.iqscaffold.billingservice.admin;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanQuotas;
import com.iqscaffold.billingservice.plan.PlanTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for creating a new subscription plan.
 * 
 * <p>This record encapsulates all required and optional parameters for
 * creating a subscription plan via the admin API.
 * 
 * @param planCode unique plan identifier (e.g., "PRO_MONTHLY_V2")
 * @param name display name for the plan
 * @param description detailed description of the plan
 * @param tier plan tier (FREE, PRO, ENTERPRISE)
 * @param billingCycle billing frequency (MONTHLY, YEARLY, LIFETIME)
 * @param basePrice price per billing cycle
 * @param currency currency code (e.g., "USD")
 * @param features feature flags map (e.g., {"api_access": true})
 * @param quotas resource quotas and limits
 * @param trialDays trial period in days (0 for no trial)
 * @param publicPlan whether the plan is publicly visible
 */
@Schema(description = "Request to create a new subscription plan")
public record CreatePlanRequest(
    @Schema(
      description = "Unique plan code identifier",
      example = "PRO_MONTHLY_V2",
      requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Plan code is required")
    @Size(max = 50, message = "Plan code must not exceed 50 characters")
    String planCode,

    @Schema(
      description = "Display name for the plan",
      example = "Professional Monthly V2",
      requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Plan name is required")
    @Size(max = 100, message = "Plan name must not exceed 100 characters")
    String name,

    @Schema(
      description = "Detailed description of the plan",
      example = "Enhanced professional plan with new AI features and increased quotas"
    )
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @Schema(
      description = "Plan tier",
      example = "PRO",
      requiredMode = Schema.RequiredMode.REQUIRED,
      allowableValues = {"FREE", "PRO", "ENTERPRISE"}
    )
    @NotNull(message = "Plan tier is required")
    PlanTier tier,

    @Schema(
      description = "Billing cycle frequency",
      example = "MONTHLY",
      requiredMode = Schema.RequiredMode.REQUIRED,
      allowableValues = {"MONTHLY", "YEARLY", "LIFETIME"}
    )
    @NotNull(message = "Billing cycle is required")
    BillingCycle billingCycle,

    @Schema(
      description = "Base price per billing cycle",
      example = "59.99",
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
    String currency,

    @Schema(
      description = "Feature flags map",
      example = """
        {
          "advanced_analytics": true,
          "api_access": true,
          "priority_support": true,
          "custom_branding": true,
          "ai_features": true
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
      description = "Trial period in days (0 for no trial)",
      example = "14"
    )
    @PositiveOrZero(message = "Trial days must be non-negative")
    Integer trialDays,

    @Schema(
      description = "Whether the plan is publicly visible",
      example = "true"
    )
    Boolean publicPlan
) {
  /**
   * Constructor with default values for optional fields.
   */
  public CreatePlanRequest {
    // Set defaults for optional fields
    if (trialDays == null) {
      trialDays = 0;
    }
    if (publicPlan == null) {
      publicPlan = true;
    }
  }
}
