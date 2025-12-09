package com.iqscaffold.billingservice.compliance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to delete billing data for GDPR compliance.
 * Supports right to erasure (GDPR Article 17).
 */
@Schema(description = "Request to delete billing data for GDPR compliance")
public record DataDeletionRequest(
    
    @Schema(description = "Confirmation phrase to prevent accidental deletion", 
            example = "DELETE MY DATA")
    @NotBlank(message = "Confirmation is required")
    String confirmation,
    
    @Schema(description = "Reason for deletion", example = "No longer using the service")
    String reason,
    
    @Schema(description = "Delete payment methods", example = "true")
    Boolean deletePaymentMethods,
    
    @Schema(description = "Anonymize instead of delete (preserves invoices for compliance)", 
            example = "false")
    Boolean anonymize
) {
    public DataDeletionRequest {
        if (deletePaymentMethods == null) {
            deletePaymentMethods = true;
        }
        if (anonymize == null) {
            anonymize = false;
        }
    }
}
