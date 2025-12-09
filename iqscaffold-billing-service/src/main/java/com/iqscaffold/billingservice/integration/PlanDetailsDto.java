package com.iqscaffold.billingservice.integration;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for subscription plan details.
 *
 * <p>Provides comprehensive plan information including features and quotas.
 * Used by business microservices to understand what capabilities are available.
 *
 * @param planCode     plan code (e.g., "PRO_MONTHLY")
 * @param name         plan name (e.g., "Professional Monthly")
 * @param tier         plan tier (FREE, PRO, ENTERPRISE)
 * @param billingCycle billing cycle (MONTHLY, YEARLY, LIFETIME)
 * @param basePrice    base price
 * @param currency     currency code (e.g., "USD")
 * @param features     map of feature codes to enabled status
 * @param quotas       map of quota names to limits
 */
@Schema(description = "Subscription plan details with features and quotas")
public record PlanDetailsDto(
    @Schema(description = "Plan code", example = "PRO_MONTHLY")
    String planCode,

    @Schema(description = "Plan name", example = "Professional Monthly")
    String name,

    @Schema(description = "Plan tier", example = "PRO")
    String tier,

    @Schema(description = "Billing cycle", example = "MONTHLY")
    String billingCycle,

    @Schema(description = "Base price", example = "49.99")
    BigDecimal basePrice,

    @Schema(description = "Currency code", example = "USD")
    String currency,

    @Schema(description = "Map of feature codes to enabled status",
            example = "{\"CRM.BULK_IMPORT\": true, \"EMAIL.CUSTOM_TEMPLATES\": true}")
    Map<String, Boolean> features,

    @Schema(description = "Map of quota names to limits",
            example = "{\"apiCallsPerMonth\": 10000, \"storageGb\": 100}")
    Map<String, Long> quotas
) {
}
