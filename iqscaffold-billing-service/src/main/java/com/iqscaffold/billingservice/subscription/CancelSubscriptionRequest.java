package com.iqscaffold.billingservice.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for canceling a subscription.
 * 
 * <p>This record encapsulates the data required to cancel a subscription,
 * including whether the cancellation should be immediate or at period end.
 * 
 * <p>Cancellation options:
 * <ul>
 *   <li>Immediate: Subscription is canceled immediately, access revoked</li>
 *   <li>Period-end: Subscription remains active until current period ends</li>
 * </ul>
 * 
 * @param immediate whether to cancel immediately (true) or at period end (false)
 * @param reason the reason for cancellation (required)
 * @param userId the user ID requesting the cancellation (optional)
 * @param metadata additional metadata for the cancellation (optional)
 */
@Schema(description = "Request to cancel a subscription")
public record CancelSubscriptionRequest(
    @Schema(
        description = "Whether to cancel immediately (true) or at period end (false)",
        example = "false",
        defaultValue = "false"
    )
    Boolean immediate,

    @Schema(
        description = "Reason for cancellation",
        example = "Customer request",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    String reason,

    @Schema(
        description = "User ID requesting the cancellation",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    UUID userId,

    @Schema(
        description = "Additional metadata for the cancellation",
        example = """
            {
              "feedback": "Too expensive",
              "alternative_provider": "Competitor X"
            }
            """
    )
    Map<String, String> metadata
) {
  /**
   * Creates a cancellation request with default values.
   * 
   * @param reason the reason for cancellation
   * @return a new cancellation request with immediate=false
   */
  public static CancelSubscriptionRequest atPeriodEnd(String reason) {
    return new CancelSubscriptionRequest(false, reason, null, null);
  }

  /**
   * Creates an immediate cancellation request.
   * 
   * @param reason the reason for cancellation
   * @return a new cancellation request with immediate=true
   */
  public static CancelSubscriptionRequest immediately(String reason) {
    return new CancelSubscriptionRequest(true, reason, null, null);
  }
}
