package com.iqscaffold.billingservice.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for churn rate analytics.
 *
 * <p>Provides comprehensive churn metrics including customer churn rate,
 * revenue churn rate, churn breakdown by tier and reason, and historical trends.
 */
@Schema(description = "Churn rate analytics and customer retention metrics")
public record ChurnAnalysisDto(
    @Schema(
        description = "Customer churn rate as percentage",
        example = "3.5",
        minimum = "0",
        maximum = "100"
    )
    BigDecimal churnRate,

    @Schema(
        description = "Number of subscriptions canceled in the period",
        example = "42"
    )
    Long churnedCount,

    @Schema(
        description = "Total active subscriptions at start of period",
        example = "1200"
    )
    Long totalActiveStart,

    @Schema(
        description = "Revenue churn rate as percentage (weighted by subscription value)",
        example = "4.2",
        minimum = "0",
        maximum = "100"
    )
    BigDecimal revenueChurnRate,

    @Schema(
        description = "Total revenue lost from churned subscriptions",
        example = "5250.00"
    )
    BigDecimal churnedRevenue,

    @Schema(
        description = "Churn breakdown by plan tier",
        example = """
            {
              "FREE": 15,
              "PRO": 22,
              "ENTERPRISE": 5
            }
            """
    )
    Map<String, Long> churnByTier,

    @Schema(
        description = "Churn reasons breakdown",
        example = """
            {
              "price": 18,
              "features": 12,
              "support": 5,
              "other": 7
            }
            """
    )
    Map<String, Long> churnReasons,

    @Schema(
        description = "Timestamp when analytics were calculated",
        example = "2024-01-15T10:30:00Z"
    )
    Instant calculatedAt,

    @Schema(
        description = "Analysis period start date",
        example = "2024-01-01T00:00:00Z"
    )
    Instant periodStart,

    @Schema(
        description = "Analysis period end date",
        example = "2024-01-31T23:59:59Z"
    )
    Instant periodEnd
) {
}
