package com.iqscaffold.billingservice.subscription;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for Subscription.
 */
@Schema(description = "Subscription data transfer object")
public record SubscriptionDto(
  @Schema(description = "Subscription unique identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
