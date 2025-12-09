package com.iqscaffold.billingservice.compliance;

import jakarta.validation.Valid;

import com.iqscaffold.billingservice.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for GDPR compliance operations.
 * Provides endpoints for data export, deletion, and retention policy management.
 */
@RestController
@RequestMapping("/api/v1/billing/compliance")
@Tag(name = "GDPR Compliance", description = "GDPR compliance operations for billing data")
@SecurityRequirement(name = "bearer-jwt")
public class GdprComplianceRestResource {

  private final GdprComplianceService gdprComplianceService;

  public GdprComplianceRestResource(GdprComplianceService gdprComplianceService) {
    this.gdprComplianceService = gdprComplianceService;
  }

  @PostMapping("/export")
  @Operation(
      summary = "Request data export",
      description = """
          Request export of all billing data for the current tenant.
          Implements GDPR Article 20 (Right to Data Portability).
          
          The export includes:
          - Current and historical subscriptions
          - Invoices and payments
          - Payment methods (tokenized, no sensitive data)
          - Usage records
          - Data retention policy
          
          The export will be sent to the specified email address.
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Export request accepted",
          content = @Content(schema = @Schema(implementation = DataExportResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Export failed")
  })
  public ResponseEntity<DataExportResponse> requestDataExport(
      @Valid @RequestBody DataExportRequest request
  ) {
    String tenantId = TenantContext.getCurrentTenantId();
    DataExportResponse response = gdprComplianceService.requestDataExport(tenantId, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/data")
  @Operation(
      summary = "Request data deletion",
      description = """
          Request deletion or anonymization of billing data for the current tenant.
          Implements GDPR Article 17 (Right to Erasure).
          
          Important notes:
          - Active subscriptions must be canceled before deletion
          - Invoices are retained indefinitely for tax compliance
          - Payments are retained for audit trail
          - Usage records can be deleted
          - Payment methods can be deleted or anonymized
          
          Requires confirmation phrase: "DELETE MY DATA"
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Deletion completed",
          content = @Content(schema = @Schema(implementation = DataDeletionResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request or confirmation"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "409", description = "Active subscription exists"),
      @ApiResponse(responseCode = "500", description = "Deletion failed")
  })
  public ResponseEntity<DataDeletionResponse> requestDataDeletion(
      @Valid @RequestBody DataDeletionRequest request
  ) {
    String tenantId = TenantContext.getCurrentTenantId();
    DataDeletionResponse response = gdprComplianceService.requestDataDeletion(tenantId, request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/retention-policy")
  @Operation(
      summary = "Get data retention policy",
      description = """
          Get the current data retention policy for billing data.
          
          The policy defines how long different types of billing data are retained:
          - Invoices: Indefinite (tax compliance)
          - Subscriptions: 7 years
          - Payments: 7 years
          - Usage records: 1 year
          - Payment methods: 90 days after deletion
          - Audit logs: 7 years
          """
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Retention policy retrieved",
          content = @Content(schema = @Schema(implementation = DataRetentionPolicy.class))
      ),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<DataRetentionPolicy> getRetentionPolicy() {
    DataRetentionPolicy policy = gdprComplianceService.getRetentionPolicy();
    return ResponseEntity.ok(policy);
  }

  @PostMapping("/apply-retention")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  @Operation(
      summary = "Apply retention policies (Admin only)",
      description = """
          Manually trigger application of data retention policies.
          This is normally run as a scheduled job.
          
          Admin only operation.
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Retention policies applied"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
  })
  public ResponseEntity<Void> applyRetentionPolicies() {
    gdprComplianceService.applyRetentionPolicies();
    return ResponseEntity.ok().build();
  }
}
