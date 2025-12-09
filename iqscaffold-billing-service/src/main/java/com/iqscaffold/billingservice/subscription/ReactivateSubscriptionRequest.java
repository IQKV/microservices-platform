package com.iqscaffold.billingservice.subscription;

import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for reactivating a canceled subscription.
 *
 * <p>This record encapsulates the data required to reactivate a subscription
 * that was previously canceled but is still within the billing period.
 *
 * <p>Reactivation is only allowed for:
 * <ul>
 *   <li>Subscriptions with cancel_at_period_end flag set</li>
 *   <li>Subscriptions in CANCELED status before period end</li>
 * </ul>
 *
 * @param reason   the reason for reactivation (optional)
 * @param userId   the user ID requesting the reactivation (optional)
 * @param metadata additional metadata for the reactivation (optional)
 */
@Schema(description = "Request to reactivate a canceled subscription")
public record ReactivateSubscriptionRequest(
    @Schema(
        description = "Reason for reactivation",
        example = "Customer changed their mind"
    )
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    String reason,

    @Schema(
        description = "User ID requesting the reactivation",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    UUID userId,

    @Schema(
        description = "Additional metadata for the reactivation",
        example = """
            {
              "feedback": "Decided to continue using the service",
              "retention_offer": "10% discount applied"
            }
            """
    )
    Map<String, String> metadata
) {
  /**
   * Creates a reactivation request with default values.
   *
   * @return a new reactivation request with default reason
   */
  public static ReactivateSubscriptionRequest withDefaults() {
    return new ReactivateSubscriptionRequest("Customer request", null, null);
  }

  /**
   * Creates a reactivation request with a specific reason.
   *
   * @param reason the reason for reactivation
   * @return a new reactivation request
   */
  public static ReactivateSubscriptionRequest withReason(String reason) {
    return new ReactivateSubscriptionRequest(reason, null, null);
  }
}
