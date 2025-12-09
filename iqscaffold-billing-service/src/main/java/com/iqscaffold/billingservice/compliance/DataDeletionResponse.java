package com.iqscaffold.billingservice.compliance;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for data deletion request.
 */
@Schema(description = "Response for data deletion request")
public record DataDeletionResponse(

    @Schema(description = "Deletion request ID", example = "del_123456")
    String deletionId,

    @Schema(description = "Deletion status", example = "COMPLETED")
    String status,

    @Schema(description = "Deleted at timestamp")
    LocalDateTime deletedAt,

    @Schema(description = "Items deleted count")
    int itemsDeleted,

    @Schema(description = "Items anonymized count")
    int itemsAnonymized,

    @Schema(description = "Message", example = "Billing data has been deleted successfully")
    String message
) {
}
