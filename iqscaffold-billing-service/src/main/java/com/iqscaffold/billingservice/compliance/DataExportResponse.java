package com.iqscaffold.billingservice.compliance;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for data export request.
 */
@Schema(description = "Response for data export request")
public record DataExportResponse(

    @Schema(description = "Export request ID", example = "exp_123456")
    String exportId,

    @Schema(description = "Export status", example = "PROCESSING")
    String status,

    @Schema(description = "Requested at timestamp")
    LocalDateTime requestedAt,

    @Schema(description = "Email where export will be sent", example = "user@example.com")
    String email,

    @Schema(description = "Message", example = "Data export request received. You will receive an email when ready.")
    String message
) {
}
