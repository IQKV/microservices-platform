package com.iqscaffold.billingservice.paymentmethod;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data transfer object for PaymentMethod.
 * Represents a stored payment method with display information (PCI DSS compliant).
 */
@Schema(description = "Payment method with display information (PCI DSS compliant - no full card numbers)")
public record PaymentMethodDto(
  @Schema(description = "Payment method unique identifier", example = "1")
  Long id,

  @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  UUID tenantId,

  @Schema(description = "User who added the payment method", example = "550e8400-e29b-41d4-a716-446655440001")
  UUID userId,

  @Schema(
    description = "Payment method type",
    example = "CARD",
    allowableValues = {"CARD", "BANK_ACCOUNT", "PAYPAL"}
  )
  PaymentMethodType type,

  @Schema(description = "Provider payment method ID", example = "pm_1234567890")
  String providerPaymentMethodId,

  @Schema(description = "Last 4 digits (for cards/bank accounts)", example = "4242")
  String last4,

  @Schema(description = "Card brand (for cards)", example = "Visa")
  String brand,

  @Schema(description = "Expiry month (for cards)", example = "12")
  Integer expiryMonth,

  @Schema(description = "Expiry year (for cards)", example = "2025")
  Integer expiryYear,

  @Schema(description = "Whether this is the default payment method", example = "true")
  Boolean isDefault,

  @Schema(description = "Whether the payment method is active", example = "true")
  Boolean active,

  @Schema(description = "Display name for the payment method", example = "Visa ending in 4242")
  String displayName,

  @Schema(description = "Status message", example = "Default payment method")
  String statusMessage,

  @Schema(description = "Whether the payment method is expired", example = "false")
  Boolean expired,

  @Schema(description = "Payment method creation timestamp", example = "2024-01-01T00:00:00")
  LocalDateTime createdAt,

  @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
  LocalDateTime updatedAt
) {}
