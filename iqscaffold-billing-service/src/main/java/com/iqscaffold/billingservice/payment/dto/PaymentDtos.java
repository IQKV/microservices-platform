package com.iqscaffold.billingservice.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentDtos {
  private PaymentDtos() {
  }

  public record CreatePaymentRequest(
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotBlank String currency,
      @NotBlank String description, // Order ID or similar
      String customerEmail, // For creating Stripe Customer
      String customerName,
      java.util.Map<String, String> metadata) {
  }

  public record PaymentResponse(
      UUID id,
      String clientSecret,
      BigDecimal amount,
      String currency,
      String status,
      Instant createdAt) {
  }
}
