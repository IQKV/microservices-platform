package com.iqscaffold.billingservice.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Data transfer object for InvoiceLineItem.
 * Represents a single line item on an invoice.
 */
@Schema(description = "Invoice line item representing a charge or credit")
public record InvoiceLineItemDto(
  @Schema(
    description = "Line item type",
    example = "SUBSCRIPTION_FEE",
    allowableValues = {"SUBSCRIPTION_FEE", "USAGE_CHARGE", "PRORATION_CREDIT", "PRORATION_CHARGE", "DISCOUNT", "TAX"}
  )
  InvoiceLineItem.LineItemType type,

  @Schema(description = "Line item description", example = "Professional Monthly Subscription")
  String description,

  @Schema(description = "Quantity of units", example = "1")
  Long quantity,

  @Schema(description = "Price per unit", example = "49.99")
  BigDecimal unitPrice,

  @Schema(description = "Total amount for this line item", example = "49.99")
  BigDecimal amount
) {}