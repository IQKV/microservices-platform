package com.iqscaffold.billingservice.paymentmethod;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for PaymentMethod.
 */
@Schema(description = "Payment method data transfer object")
public record PaymentMethodDto(
  @Schema(description = "Payment method unique identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
