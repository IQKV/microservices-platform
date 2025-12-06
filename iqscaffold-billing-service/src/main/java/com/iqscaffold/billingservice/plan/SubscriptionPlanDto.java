package com.iqscaffold.billingservice.plan;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for SubscriptionPlan.
 */
@Schema(description = "Subscription plan data transfer object")
public record SubscriptionPlanDto(
  @Schema(description = "Plan unique identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
