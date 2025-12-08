package com.iqscaffold.userservice.tenancy;

import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.TenantResponse;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST resource for billing service integration.
 * Provides endpoints for the billing service to update tenant subscription information.
 * These endpoints are intended for service-to-service communication only.
 */
@RestController
@RequestMapping("/api/v1/internal/billing")
@Tag(name = "Internal Billing Integration", description = "Internal endpoints for billing service integration (service-to-service only)")
public class InternalBillingRestResource {

  private static final Logger logger = LoggerFactory.getLogger(InternalBillingRestResource.class);

  private final TenantManagementService tenantManagementService;

  public InternalBillingRestResource(final TenantManagementService tenantManagementService) {
    this.tenantManagementService = tenantManagementService;
  }

  /**
   * Update tenant subscription information.
   * Called by billing service when subscription status changes.
   *
   * @param tenantId the tenant ID
   * @param request  the subscription update request
   * @return the updated tenant response
   */
  @PutMapping("/tenants/{tenantId}/subscription")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'SYSTEM')")
  @Timed(value = "internal.billing.update_subscription", description = "Time taken to update tenant subscription")
  @Operation(
      summary = "Update tenant subscription",
      description = """
          Updates tenant subscription information from billing service.
          
          ## Purpose
          This endpoint is called by the billing service when:
          - A subscription is created
          - Subscription status changes (ACTIVE, TRIAL, EXPIRED, etc.)
          - Subscription plan is upgraded or downgraded
          
          ## Security
          - Requires ADMIN, SUPER_ADMIN, or SYSTEM authority
          - Intended for service-to-service communication only
          - Should be called with service account JWT token
          
          ## Side Effects
          - Updates tenant subscription fields
          - Evicts tenant cache entries
          - New JWT tokens will include updated subscription context
          """,
      security = @SecurityRequirement(name = "bearer-jwt")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Subscription updated successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TenantResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data"
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Access denied - insufficient permissions"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Tenant not found"
      )
  })
  public ResponseEntity<TenantResponse> updateTenantSubscription(
      @Parameter(description = "Tenant ID", required = true, example = "tenant-abc-123")
      @PathVariable String tenantId,
      
      @Parameter(description = "Subscription update request", required = true)
      @Valid @RequestBody TenantSubscriptionUpdateRequest request
  ) {
    logger.info("Received subscription update request for tenant: {} from billing service", tenantId);
    
    var response = tenantManagementService.updateTenantSubscription(tenantId, request);
    
    logger.info("Successfully updated subscription for tenant: {}", tenantId);
    
    return ResponseEntity.ok(response);
  }
}
