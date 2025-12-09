package com.iqscaffold.billingservice.integration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for feature access check.
 *
 * <p>Provides detailed information about feature availability including:
 * <ul>
 *   <li>Whether the feature is available in the current plan</li>
 *   <li>Current subscription status</li>
 *   <li>Plan tier information</li>
 *   <li>Upgrade URL if feature is not available</li>
 * </ul>
 *
 * @param featureCode        the feature code that was checked
 * @param available          whether the feature is available
 * @param subscriptionStatus current subscription status (ACTIVE, TRIAL, EXPIRED, etc.)
 * @param planTier           current plan tier (FREE, PRO, ENTERPRISE)
 * @param upgradeUrl         URL to upgrade page if feature not available (null if available)
 * @param message            human-readable message about feature availability
 */
@Schema(description = "Response indicating feature access availability")
public record FeatureCheckResponse(
    @Schema(description = "Feature code that was checked", example = "CRM.BULK_IMPORT")
    String featureCode,

    @Schema(description = "Whether the feature is available", example = "true")
    boolean available,

    @Schema(description = "Current subscription status", example = "ACTIVE")
    String subscriptionStatus,

    @Schema(description = "Current plan tier", example = "PRO")
    String planTier,

    @Schema(description = "URL to upgrade if feature not available", example = "/billing/portal/upgrade")
    String upgradeUrl,

    @Schema(description = "Human-readable message", example = "Feature available in your PRO plan")
    String message
) {
}
