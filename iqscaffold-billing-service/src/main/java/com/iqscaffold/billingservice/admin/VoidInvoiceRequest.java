package com.iqscaffold.billingservice.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to void an invoice.
 *
 * <p>This request is used by administrators to void unpaid invoices,
 * preventing payment and marking them as canceled.
 */
@Schema(description = "Request to void an invoice with reason")
public record VoidInvoiceRequest(
    @Schema(
        description = "Reason for voiding the invoice (required for audit trail)",
        example = "Customer requested cancellation due to billing error",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    String reason
) {
}
