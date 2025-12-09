package com.iqscaffold.billingservice.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for Invoice.
 * Represents a complete invoice with line items and payment information.
 */
@Schema(description = "Invoice with complete billing information and line items")
public record InvoiceDto(
    @Schema(description = "Invoice unique identifier", example = "1")
    Long id,

    @Schema(description = "Subscription ID this invoice belongs to", example = "1")
    Long subscriptionId,

    @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID tenantId,

    @Schema(description = "Unique invoice number", example = "INV-202401-00123")
    String invoiceNumber,

    @Schema(
        description = "Invoice status",
        example = "OPEN",
        allowableValues = {"DRAFT", "OPEN", "PAID", "VOID", "UNCOLLECTIBLE"}
    )
    InvoiceStatus status,

    @Schema(description = "Subtotal before tax", example = "49.99")
    BigDecimal subtotal,

    @Schema(description = "Tax amount", example = "5.00")
    BigDecimal tax,

    @Schema(description = "Total amount due", example = "54.99")
    BigDecimal total,

    @Schema(description = "Currency code", example = "USD")
    String currency,

    @Schema(description = "Billing period start date", example = "2024-01-01T00:00:00")
    LocalDateTime periodStart,

    @Schema(description = "Billing period end date", example = "2024-02-01T00:00:00")
    LocalDateTime periodEnd,

    @Schema(description = "Payment due date", example = "2024-02-08T00:00:00")
    LocalDateTime dueDate,

    @Schema(description = "Date when invoice was paid", example = "2024-02-05T14:30:00")
    LocalDateTime paidAt,

    @Schema(description = "Payment method ID used for payment", example = "1")
    Long paymentMethodId,

    @Schema(description = "Provider invoice ID (e.g., Stripe invoice ID)", example = "in_1234567890")
    String providerInvoiceId,

    @Schema(description = "Invoice line items")
    List<InvoiceLineItemDto> lineItems,

    @Schema(
        description = "Additional invoice metadata",
        example = """
            {
              "billing_reason": "subscription_cycle",
              "payment_intent_id": "pi_1234567890"
            }
            """
    )
    Map<String, Object> metadata,

    @Schema(description = "Invoice creation timestamp", example = "2024-01-01T00:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2024-02-05T14:30:00")
    LocalDateTime updatedAt
) {
}
