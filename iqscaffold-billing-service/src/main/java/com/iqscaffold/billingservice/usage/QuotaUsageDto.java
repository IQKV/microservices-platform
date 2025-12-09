package com.iqscaffold.billingservice.usage;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for quota usage information.
 * Shows current usage against quota limits for a specific metric.
 */
@Schema(description = "Quota usage information showing current usage vs limits")
public record QuotaUsageDto(
    @Schema(
        description = "Type of metric",
        example = "API_CALLS",
        allowableValues = {
            "API_CALLS",
            "STORAGE_GB",
            "EMAIL_SENDS",
            "CAMPAIGN_EXECUTIONS",
            "SCORING_REQUESTS",
            "ACTIVE_USERS",
            "CUSTOM_DOMAINS",
            "DATA_EXPORTS",
            "CUSTOM"
        }
    )
    MetricType metricType,

    @Schema(description = "Current usage quantity", example = "8500")
    Long currentUsage,

    @Schema(description = "Quota limit (null means unlimited)", example = "10000")
    Long limit,

    @Schema(description = "Remaining quota", example = "1500")
    Long remainingQuota,

    @Schema(description = "Whether the operation is allowed", example = "true")
    Boolean allowed,

    @Schema(description = "Usage percentage (0-100+)", example = "85.0")
    Double percentageUsed,

    @Schema(description = "When the quota resets (end of billing period)", example = "2024-02-01T00:00:00")
    LocalDateTime resetDate
) {
}
