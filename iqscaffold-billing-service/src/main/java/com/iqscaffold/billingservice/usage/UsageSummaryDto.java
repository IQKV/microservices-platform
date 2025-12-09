package com.iqscaffold.billingservice.usage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for UsageSummary.
 * Represents aggregated usage across all metrics for a time period.
 */
@Schema(description = "Aggregated usage summary for a tenant across all metrics")
public record UsageSummaryDto(
    @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID tenantId,

    @Schema(description = "Summary period start date", example = "2024-01-01T00:00:00")
    LocalDateTime periodStart,

    @Schema(description = "Summary period end date", example = "2024-02-01T00:00:00")
    LocalDateTime periodEnd,

    @Schema(description = "Usage metrics by type")
    List<UsageMetricDto> metrics,

    @Schema(description = "Total number of usage records in the period", example = "1523")
    Integer totalRecords
) {
}
