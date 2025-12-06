package com.iqscaffold.billingservice.portal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for billing dashboard.
 */
@Schema(description = "Billing dashboard data transfer object")
public record BillingDashboardDto(
  @Schema(description = "Dashboard identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
