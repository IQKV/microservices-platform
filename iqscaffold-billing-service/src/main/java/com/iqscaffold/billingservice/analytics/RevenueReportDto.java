package com.iqscaffold.billingservice.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Data transfer object for Monthly Recurring Revenue (MRR) analytics.
 * 
 * <p>Provides comprehensive MRR metrics including current MRR, growth rate,
 * breakdown by plan tier, and MRR movement components (new, churned, expansion, contraction).
 */
@Schema(description = "Monthly Recurring Revenue (MRR) analytics report")
public record RevenueReportDto(
  @Schema(
    description = "Current Monthly Recurring Revenue",
    example = "125000.00"
  )
  BigDecimal currentMrr,

  @Schema(
    description = "Previous month's MRR for comparison",
    example = "118000.00"
  )
  BigDecimal previousMrr,

  @Schema(
    description = "MRR growth rate as percentage (month-over-month)",
    example = "5.93",
    minimum = "-100"
  )
  BigDecimal growthRate,

  @Schema(
    description = "New MRR from new subscriptions",
    example = "12000.00"
  )
  BigDecimal newMrr,

  @Schema(
    description = "Churned MRR from canceled subscriptions",
    example = "3500.00"
  )
  BigDecimal churnedMrr,

  @Schema(
    description = "Expansion MRR from upgrades",
    example = "4500.00"
  )
  BigDecimal expansionMrr,

  @Schema(
    description = "Contraction MRR from downgrades",
    example = "1000.00"
  )
  BigDecimal contractionMrr,

  @Schema(
    description = "MRR breakdown by plan tier",
    example = """
      {
        "FREE": 0.00,
        "PRO": 85000.00,
        "ENTERPRISE": 40000.00
      }
      """
  )
  Map<String, BigDecimal> mrrByTier,

  @Schema(
    description = "Timestamp when MRR was calculated",
    example = "2024-01-15T10:30:00Z"
  )
  Instant calculatedAt
) {}
