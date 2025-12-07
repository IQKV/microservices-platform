package com.iqscaffold.billingservice.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data transfer object for Payment.
 * Represents a payment transaction with complete status and refund information.
 */
@Schema(description = "Payment transaction with status and refund information")
public record PaymentDto(
  @Schema(description = "Payment unique identifier", example = "1")
  Long id,

  @Schema(description = "Invoice ID this payment is for", example = "1")
  Long invoiceId,

  @Schema(description = "Tenant unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  UUID tenantId,

  @Schema(description = "Payment amount", example = "54.99")
  BigDecimal amount,

  @Schema(description = "Currency code", example = "USD")
  String currency,

  @Schema(
    description = "Payment status",
    example = "SUCCEEDED",
    allowableValues = {"PENDING", "SUCCEEDED", "FAILED", "REFUNDED"}
  )
  PaymentStatus status,

  @Schema(description = "Payment method ID used", example = "1")
  Long paymentMethodId,

  @Schema(description = "Payment method type", example = "CARD")
  String paymentMethodType,

  @Schema(description = "Last 4 digits of payment method", example = "4242")
  String paymentMethodLast4,

  @Schema(description = "Provider payment ID (e.g., Stripe payment intent ID)", example = "pi_1234567890")
  String providerPaymentId,

  @Schema(description = "Failure reason if payment failed", example = "Insufficient funds")
  String failureReason,

  @Schema(description = "Amount refunded", example = "0.00")
  BigDecimal refundedAmount,

  @Schema(
    description = "Additional payment metadata",
    example = """
      {
        "payment_method_brand": "visa",
        "receipt_url": "https://example.com/receipt"
      }
      """
  )
  Map<String, Object> metadata,

  @Schema(description = "Payment creation timestamp", example = "2024-02-05T14:30:00")
  LocalDateTime createdAt,

  @Schema(description = "Last update timestamp", example = "2024-02-05T14:30:15")
  LocalDateTime updatedAt
) {}
