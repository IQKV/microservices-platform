package com.iqscaffold.billingservice.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new invoice.
 * Contains all required information to generate an invoice for a subscription.
 */
@Schema(description = "Request to create a new invoice")
public record CreateInvoiceRequest(
  @Schema(
    description = "Subscription ID this invoice is for",
    example = "1",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Subscription ID is required")
  Long subscriptionId,

  @Schema(
    description = "Tenant unique identifier",
    example = "550e8400-e29b-41d4-a716-446655440000",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Tenant ID is required")
  UUID tenantId,

  @Schema(
    description = "Currency code",
    example = "USD",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank(message = "Currency is required")
  String currency,

  @Schema(
    description = "Billing period start date",
    example = "2024-01-01T00:00:00",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Period start is required")
  LocalDateTime periodStart,

  @Schema(
    description = "Billing period end date",
    example = "2024-02-01T00:00:00",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Period end is required")
  LocalDateTime periodEnd,

  @Schema(
    description = "Number of days until payment is due",
    example = "7",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Due days is required")
  @Positive(message = "Due days must be positive")
  Integer dueDays,

  @Schema(
    description = "Invoice line items",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotEmpty(message = "Line items cannot be empty")
  @Valid
  List<InvoiceLineItemDto> lineItems,

  @Schema(description = "Tax amount", example = "5.00")
  BigDecimal tax,

  @Schema(description = "Payment method ID to use", example = "1")
  Long paymentMethodId,

  @Schema(
    description = "Additional invoice metadata",
    example = """
      {
        "billing_reason": "subscription_cycle",
        "notes": "Generated automatically"
      }
      """
  )
  Map<String, Object> metadata
) {}