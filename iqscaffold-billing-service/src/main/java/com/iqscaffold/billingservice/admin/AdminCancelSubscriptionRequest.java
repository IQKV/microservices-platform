package com.iqscaffold.billingservice.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for admin force cancellation of a subscription.
 * 
 * <p>This record encapsulates the required parameters for an admin to
 * force cancel a subscription, bypassing normal cancellation rules.
 * 
 * @param reason reason for the cancellation (required for audit trail)
 */
@Schema(description = "Request to force cancel a subscription (admin only)")
public record AdminCancelSubscriptionRequest(
    @Schema(
      description = "Reason for the cancellation (required for audit trail)",
      example = "Policy violation - Terms of Service breach",
      requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    String reason
) {}
