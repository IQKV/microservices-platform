package com.iqscaffold.billingservice.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for revenue reports.
 */
@Schema(description = "Revenue report data transfer object")
public record RevenueReportDto(
  @Schema(description = "Report identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
