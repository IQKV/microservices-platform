package com.iqscaffold.billingservice.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Request DTO for updating an existing subscription.
 * Supports plan changes (upgrades/downgrades) and metadata updates.
 */
@Schema(description = "Request to update an existing subscription")
public record UpdateSubscriptionRequest(
  @Schema(
    description = "New subscription plan code for upgrade/downgrade",
    example = "ENTERPRISE_YEARLY"
  )
  @NotBlank(message = "Plan code cannot be blank")
  String newPlanCode,

  @Schema(
    description = "Whether to apply the change immediately or at period end",
    example = "true"
  )
  Boolean immediate,

  @Schema(
    description = "Additional metadata to add or update",
    example = """
      {
        "upgrade_reason": "need_more_users",
        "requested_by": "admin@company.com"
      }
      """
  )
  Map<String, Object> metadata
) {}