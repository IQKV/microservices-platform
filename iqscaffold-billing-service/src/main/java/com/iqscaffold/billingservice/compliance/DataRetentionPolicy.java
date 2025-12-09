package com.iqscaffold.billingservice.compliance;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data retention policy for billing data.
 * Defines how long different types of billing data are retained.
 */
@Schema(description = "Data retention policy for billing data")
public record DataRetentionPolicy(

    @Schema(description = "Subscription data retention in days", example = "2555")
    int subscriptionRetentionDays,

    @Schema(description = "Invoice data retention in days (indefinite for tax compliance)",
            example = "-1")
    int invoiceRetentionDays,

    @Schema(description = "Payment data retention in days", example = "2555")
    int paymentRetentionDays,

    @Schema(description = "Usage data retention in days", example = "365")
    int usageRetentionDays,

    @Schema(description = "Payment method retention in days after deletion", example = "90")
    int paymentMethodRetentionDays,

    @Schema(description = "Audit log retention in days", example = "2555")
    int auditLogRetentionDays,

    @Schema(description = "Policy description")
    String description
) {
  /**
   * Default retention policy following GDPR and tax compliance requirements.
   */
  public static DataRetentionPolicy defaultPolicy() {
    return new DataRetentionPolicy(
        2555,  // 7 years for subscriptions
        -1,    // Indefinite for invoices (tax compliance)
        2555,  // 7 years for payments
        365,   // 1 year for usage data
        90,    // 90 days for deleted payment methods
        2555,  // 7 years for audit logs
        """
            Default retention policy:
            - Invoices: Retained indefinitely for tax and audit compliance
            - Subscriptions, Payments: 7 years (standard business practice)
            - Usage Records: 1 year (operational data)
            - Payment Methods: 90 days after deletion (fraud prevention)
            - Audit Logs: 7 years (compliance requirement)
            """
    );
  }
}
