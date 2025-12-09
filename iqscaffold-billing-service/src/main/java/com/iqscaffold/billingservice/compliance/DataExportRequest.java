package com.iqscaffold.billingservice.compliance;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to export billing data for GDPR compliance.
 * Supports right to data portability (GDPR Article 20).
 */
@Schema(description = "Request to export billing data for GDPR compliance")
public record DataExportRequest(

    @Schema(description = "Email address to send the export to", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @Schema(description = "Export format", example = "JSON", allowableValues = {"JSON", "CSV"})
    String format,

    @Schema(description = "Include historical data", example = "true")
    Boolean includeHistorical
) {
  public DataExportRequest {
    if (format == null || format.isBlank()) {
      format = "JSON";
    }
    if (includeHistorical == null) {
      includeHistorical = true;
    }
  }
}
