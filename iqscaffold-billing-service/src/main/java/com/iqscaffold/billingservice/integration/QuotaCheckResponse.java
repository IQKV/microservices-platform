package com.iqscaffold.billingservice.integration;

import java.time.LocalDateTime;

import com.iqscaffold.billingservice.usage.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for quota check.
 *
 * <p>Provides detailed information about quota availability including:
 * <ul>
 *   <li>Whether the requested operation is allowed</li>
 *   <li>Current usage and quota limits</li>
 *   <li>Remaining quota</li>
 *   <li>Percentage used</li>
 *   <li>When the quota resets (end of billing period)</li>
 * </ul>
 *
 * @param metricType     type of metric checked
 * @param currentUsage   current usage in the billing period
 * @param limit          quota limit for the billing period
 * @param remainingQuota remaining quota available
 * @param allowed        whether the requested operation is allowed
 * @param percentageUsed percentage of quota used (0-100)
 * @param resetsAt       when the quota resets (end of billing period)
 * @param message        human-readable message about quota status
 */
@Schema(description = "Response indicating quota availability")
public record QuotaCheckResponse(
    @Schema(description = "Type of metric checked", example = "API_CALLS")
    MetricType metricType,

    @Schema(description = "Current usage in billing period", example = "750")
    long currentUsage,

    @Schema(description = "Quota limit for billing period", example = "1000")
    long limit,

    @Schema(description = "Remaining quota available", example = "250")
    long remainingQuota,

    @Schema(description = "Whether the requested operation is allowed", example = "true")
    boolean allowed,

    @Schema(description = "Percentage of quota used", example = "75.0")
    double percentageUsed,

    @Schema(description = "When quota resets (end of billing period)", example = "2024-02-01T00:00:00")
    LocalDateTime resetsAt,

    @Schema(description = "Human-readable message", example = "250 API calls remaining (75% used)")
    String message
) {
}
