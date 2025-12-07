package com.iqscaffold.billingservice.portal;

import com.iqscaffold.billingservice.invoice.InvoiceDto;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto;
import com.iqscaffold.billingservice.subscription.SubscriptionDto;
import com.iqscaffold.billingservice.usage.QuotaUsageDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Data transfer object for billing dashboard.
 * Provides a comprehensive view of subscription, usage, invoices, and payment methods.
 */
@Schema(description = "Comprehensive billing dashboard with subscription, usage, invoices, and payment information")
public record BillingDashboardDto(
  @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  UUID tenantId,

  @Schema(description = "Current subscription information")
  SubscriptionDto subscription,

  @Schema(description = "Quota usage for all metrics")
  List<QuotaUsageDto> quotaUsage,

  @Schema(description = "Recent invoices (last 12 months)")
  List<InvoiceDto> recentInvoices,

  @Schema(description = "Upcoming invoice amount estimate", example = "49.99")
  BigDecimal upcomingInvoiceAmount,

  @Schema(description = "Payment methods on file")
  List<PaymentMethodDto> paymentMethods,

  @Schema(description = "Total amount paid to date", example = "599.88")
  BigDecimal totalPaid,

  @Schema(description = "Number of days remaining in current period", example = "15")
  Long daysRemainingInPeriod,

  @Schema(description = "Whether subscription is in trial", example = "false")
  Boolean inTrial,

  @Schema(description = "Whether subscription is past due", example = "false")
  Boolean pastDue,

  @Schema(description = "Whether subscription is canceled", example = "false")
  Boolean canceled,

  @Schema(description = "Available upgrade plans")
  List<UpgradePlanOption> availableUpgrades,

  @Schema(description = "Available downgrade plans")
  List<DowngradePlanOption> availableDowngrades
) {

  /**
   * Represents an available upgrade plan option.
   */
  @Schema(description = "Available upgrade plan option")
  public record UpgradePlanOption(
    @Schema(description = "Plan code", example = "ENTERPRISE_YEARLY")
    String planCode,

    @Schema(description = "Plan name", example = "Enterprise Yearly")
    String planName,

    @Schema(description = "Plan price", example = "999.00")
    BigDecimal price,

    @Schema(description = "Proration amount for immediate upgrade", example = "850.00")
    BigDecimal prorationAmount,

    @Schema(description = "Key features of this plan")
    List<String> keyFeatures
  ) {}

  /**
   * Represents an available downgrade plan option.
   */
  @Schema(description = "Available downgrade plan option")
  public record DowngradePlanOption(
    @Schema(description = "Plan code", example = "FREE")
    String planCode,

    @Schema(description = "Plan name", example = "Free Plan")
    String planName,

    @Schema(description = "Plan price", example = "0.00")
    BigDecimal price,

    @Schema(description = "Credit amount for downgrade", example = "25.00")
    BigDecimal creditAmount,

    @Schema(description = "Key limitations of this plan")
    List<String> limitations
  ) {}
}
