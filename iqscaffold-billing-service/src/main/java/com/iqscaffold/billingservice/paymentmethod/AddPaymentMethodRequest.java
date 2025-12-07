package com.iqscaffold.billingservice.paymentmethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for adding a new payment method.
 * Contains payment method details from the payment provider.
 */
@Schema(description = "Request to add a new payment method")
public record AddPaymentMethodRequest(
  @Schema(
    description = "Tenant unique identifier",
    example = "550e8400-e29b-41d4-a716-446655440000",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "Tenant ID is required")
  UUID tenantId,

  @Schema(
    description = "User adding the payment method",
    example = "550e8400-e29b-41d4-a716-446655440001",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull(message = "User ID is required")
  UUID userId,

  @Schema(
    description = "Payment method type",
    example = "CARD",
    requiredMode = Schema.RequiredMode.REQUIRED,
    allowableValues = {"CARD", "BANK_ACCOUNT", "PAYPAL"}
  )
  @NotNull(message = "Payment method type is required")
  PaymentMethodType type,

  @Schema(
    description = "Provider payment method ID (e.g., Stripe payment method ID)",
    example = "pm_1234567890",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank(message = "Provider payment method ID is required")
  String providerPaymentMethodId,

  @Schema(description = "Last 4 digits (for cards/bank accounts)", example = "4242")
  String last4,

  @Schema(description = "Card brand (for cards)", example = "Visa")
  String brand,

  @Schema(description = "Expiry month (for cards, 1-12)", example = "12")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  Integer expiryMonth,

  @Schema(description = "Expiry year (for cards, 4 digits)", example = "2025")
  @Min(value = 2000, message = "Expiry year must be between 2000 and 2100")
  @Max(value = 2100, message = "Expiry year must be between 2000 and 2100")
  Integer expiryYear,

  @Schema(description = "Whether to set as default payment method", example = "true")
  Boolean setAsDefault
) {}