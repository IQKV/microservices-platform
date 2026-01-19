package com.iqscaffold.billingservice.feature.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO containing user's subscription features and plan information.
 */
@Schema(description = "User's subscription features and plan information")
public record UserFeaturesResponse(
    @Schema(description = "List of features available to the user")
    List<FeatureDto> enabledFeatures,
    
    @Schema(description = "All available features with their status")
    List<FeatureDto> allFeatures,
    
    @Schema(description = "Current subscription plan name", example = "Pro Plan")
    String planName,
    
    @Schema(description = "Subscription status", example = "ACTIVE")
    String subscriptionStatus,
    
    @Schema(description = "Subscription expiry date (null if no expiry)")
    Instant subscriptionExpiresAt,
    
    @Schema(description = "Whether the subscription is in trial period", example = "false")
    boolean isTrialPeriod,
    
    @Schema(description = "Trial expiry date (null if not in trial)")
    Instant trialExpiresAt,
    
    @Schema(description = "Tenant ID", example = "tenant-123")
    String tenantId
) {
    
    /**
     * Creates a response with only enabled features (for lightweight responses).
     */
    public static UserFeaturesResponse withEnabledOnly(
            List<FeatureDto> enabledFeatures,
            String planName,
            String subscriptionStatus,
            String tenantId) {
        return new UserFeaturesResponse(
            enabledFeatures,
            enabledFeatures, // Same as enabled for lightweight response
            planName,
            subscriptionStatus,
            null,
            false,
            null,
            tenantId
        );
    }
}