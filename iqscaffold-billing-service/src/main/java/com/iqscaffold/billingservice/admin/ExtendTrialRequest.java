package com.iqscaffold.billingservice.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for extending a subscription's trial period.
 *
 * <p>This record encapsulates the required parameters for an admin to
 * extend a trial period by a specified number of days.
 *
 * @param additionalDays number of days to add to the trial period
 * @param reason         reason for the extension (required for audit trail)
 */
@Schema(description = "Request to extend a subscription's trial period")
public record ExtendTrialRequest(
    @Schema(
        description = "Number of days to add to the trial period",
        example = "7",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Positive(message = "Additional days must be positive")
    int additionalDays,

    @Schema(
        description = "Reason for the trial extension (required for audit trail)",
        example = "Customer requested more time to evaluate features",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Extension reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    String reason
) {
}
