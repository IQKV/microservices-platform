package com.iqscaffold.billingservice.compliance;

import com.iqscaffold.billingservice.invoice.InvoiceDto;
import com.iqscaffold.billingservice.payment.PaymentDto;
import com.iqscaffold.billingservice.paymentmethod.PaymentMethodDto;
import com.iqscaffold.billingservice.subscription.SubscriptionDto;
import com.iqscaffold.billingservice.usage.UsageDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete billing data export for a tenant.
 * Contains all billing-related data for GDPR compliance.
 */
@Schema(description = "Complete billing data export for GDPR compliance")
public record BillingDataExport(
    
    @Schema(description = "Tenant ID")
    String tenantId,
    
    @Schema(description = "Export generated at timestamp")
    LocalDateTime exportedAt,
    
    @Schema(description = "Export format", example = "JSON")
    String format,
    
    @Schema(description = "Current subscription")
    SubscriptionDto currentSubscription,
    
    @Schema(description = "Historical subscriptions")
    List<SubscriptionDto> historicalSubscriptions,
    
    @Schema(description = "Invoices")
    List<InvoiceDto> invoices,
    
    @Schema(description = "Payments")
    List<PaymentDto> payments,
    
    @Schema(description = "Payment methods (tokenized, no sensitive data)")
    List<PaymentMethodDto> paymentMethods,
    
    @Schema(description = "Usage records")
    List<UsageDto> usageRecords,
    
    @Schema(description = "Data retention policy")
    DataRetentionPolicy retentionPolicy
) {
}
