package com.iqscaffold.billingservice.subscription;

import com.iqscaffold.billingservice.plan.BillingCycle;
import com.iqscaffold.billingservice.plan.PlanTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data transfer object for Subscription.
 * Represents a tenant's subscription to a plan with full lifecycle information.
 */
@Schema(description = "Subscription data transfer object with complete lifecycle information")
public record SubscriptionDto(
  @Schema(description = "Subscription unique identifier", example = "1")
  Long id,

  @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  UUID tenantId,

  @Schema(description = "User who created the subscription", example = "550e8400-e29b-41d4-a716-446655440001")
  UUID userId,

  @Schema(description = "Subscription plan code", example = "PRO_MONTHLY")
  String planCode,

  @Schema(description = "Subscription plan name", example = "Professional Monthly")
  String planName,

  @Schema(description = "Plan tier", example = "PRO")
  PlanTier planTier,

  @Schema(description = "Billing cycle", example = "MONTHLY")
  BillingCycle billingCycle,

  @Schema(description = "Plan base price", example = "49.99")
  BigDecimal planPrice,

  @Schema(description = "Currency code", example = "USD")
  String currency,

  @Schema(
    description = "Current subscription status",
    example = "ACTIVE",
    allowableValues = {"TRIAL", "ACTIVE", "PAST_DUE", "CANCELED", "EXPIRED", "SUSPENDED", "INCOMPLETE"}
  )
  SubscriptionStatus status,

  @Schema(description = "Current billing period start date", example = "2024-01-01T00:00:00")
  LocalDateTime currentPeriodStart,

  @Schema(description = "Current billing period end date", example = "2024-02-01T00:00:00")
  LocalDateTime currentPeriodEnd,

  @Schema(description = "Trial period start date", example = "2024-01-01T00:00:00")
  LocalDateTime trialStart,

  @Schema(description = "Trial period end date", example = "2024-01-15T00:00:00")
  LocalDateTime trialEnd,

  @Schema(description = "Date when subscription was canceled", example = "2024-01-20T10:30:00")
  LocalDateTime canceledAt,

  @Schema(description = "Whether subscription cancels at period end", example = "false")
  Boolean cancelAtPeriodEnd,

  @Schema(
    description = "Additional metadata for the subscription",
    example = """
      {
        "source": "web_signup",
        "campaign": "summer_promo"
      }
      """
  )
  Map<String, Object> metadata,

  @Schema(description = "Subscription creation timestamp", example = "2024-01-01T00:00:00")
  LocalDateTime createdAt,

  @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
  LocalDateTime updatedAt
) {}
