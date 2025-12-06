package com.iqscaffold.billingservice.payment;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for Payment.
 */
@Schema(description = "Payment data transfer object")
public record PaymentDto(
  @Schema(description = "Payment unique identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
