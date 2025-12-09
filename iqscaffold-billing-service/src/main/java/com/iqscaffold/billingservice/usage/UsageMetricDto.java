package com.iqscaffold.billingservice.usage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for UsageMetric.
 * Represents aggregated usage for a specific metric type.
 */
@Schema(description = "Aggregated usage for a specific metric type")
public record UsageMetricDto(
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

    @Schema(description = "Total quantity consumed", example = "15000")
    Long quantity,

    @Schema(description = "Unit of measurement", example = "requests")
    String unit,

    @Schema(description = "Quota limit (null means unlimited)", example = "10000")
    Long limit
) {
}
