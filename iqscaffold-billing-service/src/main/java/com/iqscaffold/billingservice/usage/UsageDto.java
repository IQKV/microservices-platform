package com.iqscaffold.billingservice.usage;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data transfer object for UsageRecord.
 * Represents a single usage measurement for billing and quota tracking.
 */
@Schema(description = "Usage record for resource consumption tracking")
public record UsageDto(
  @Schema(description = "Usage record unique identifier", example = "1")
  Long id,

  @Schema(description = "Subscription ID this usage belongs to", example = "1")
  Long subscriptionId,

  @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  UUID tenantId,

  @Schema(
    description = "Type of metric being tracked",
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

  @Schema(description = "Quantity of usage", example = "1000")
  Long quantity,

  @Schema(description = "Unit of measurement", example = "requests")
  String unit,

  @Schema(description = "When the usage was recorded", example = "2024-01-15T10:30:00")
  LocalDateTime recordedAt,

  @Schema(description = "Billing period start date", example = "2024-01-01T00:00:00")
  LocalDateTime billingPeriodStart,

  @Schema(description = "Billing period end date", example = "2024-02-01T00:00:00")
  LocalDateTime billingPeriodEnd,

  @Schema(
    description = "Additional usage metadata",
    example = """
      {
        "service": "crm-service",
        "endpoint": "/api/v1/contacts",
        "user_id": "550e8400-e29b-41d4-a716-446655440001"
      }
      """
  )
  Map<String, Object> metadata,

  @Schema(description = "Usage record creation timestamp", example = "2024-01-15T10:30:00")
  LocalDateTime createdAt
) {}
