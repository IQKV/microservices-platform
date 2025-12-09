package com.iqscaffold.billingservice.integration;

import java.time.LocalDateTime;
import java.util.UUID;

import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for subscription status information.
 *
 * <p>Provides essential subscription information for integration with business microservices.
 * Used to determine if a tenant has an active subscription and what features are available.
 *
 * @param tenantId         tenant identifier
 * @param subscriptionId   subscription identifier
 * @param status           subscription status (ACTIVE, TRIAL, EXPIRED, etc.)
 * @param planCode         plan code (e.g., "PRO_MONTHLY")
 * @param planTier         plan tier (FREE, PRO, ENTERPRISE)
 * @param currentPeriodEnd when the current billing period ends
 * @param trialEnd         when trial ends (null if not in trial)
 * @param active           whether subscription is active (ACTIVE, TRIAL, or PAST_DUE)
 */
@Schema(description = "Subscription status information")
public record SubscriptionStatusDto(
    @Schema(description = "Tenant identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID tenantId,

    @Schema(description = "Subscription identifier", example = "1")
    Long subscriptionId,

    @Schema(description = "Subscription status", example = "ACTIVE")
    SubscriptionStatus status,

    @Schema(description = "Plan code", example = "PRO_MONTHLY")
    String planCode,

    @Schema(description = "Plan tier", example = "PRO")
    String planTier,

    @Schema(description = "Current billing period end", example = "2024-02-01T00:00:00")
    LocalDateTime currentPeriodEnd,

    @Schema(description = "Trial end date (null if not in trial)", example = "2024-01-22T00:00:00")
    LocalDateTime trialEnd,

    @Schema(description = "Whether subscription is active", example = "true")
    boolean active
) {
}
