package com.iqscaffold.billingservice.invoice;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object for Invoice.
 */
@Schema(description = "Invoice data transfer object")
public record InvoiceDto(
  @Schema(description = "Invoice unique identifier", example = "1") Long id
  // Additional fields will be added in subsequent tasks
) {}
