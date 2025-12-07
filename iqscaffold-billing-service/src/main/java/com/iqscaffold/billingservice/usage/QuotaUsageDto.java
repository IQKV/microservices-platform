package com.iqscaffold.billingservice.usage;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Data transfer object for quota usage information.
 * Shows current usage against quota limits for a specific metric.
 */
@Schema(description = "Quota usage information showing current usage vs limits")
public record QuotaUsageDto(
  @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  UUID tenantId,

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
  Long quotaLimit,

  @Schema(description = "Remaining quota (null if unlimited)", example = "1500")
  Long remaining,

  @Schema(description = "Usage percentage (0-100, null if unlimited)", example = "85.0")
  Double usagePercentage,

  @Schema(description = "Whether quota is exceeded", example = "false")
  Boolean exceeded,

  @Schema(description = "Whether quota is unlimited", example = "false")
  Boolean unlimited,

  @Schema(description = "Unit of measurement", example = "requests")
  String unit
) {}