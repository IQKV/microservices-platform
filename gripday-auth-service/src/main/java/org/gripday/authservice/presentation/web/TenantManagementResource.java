package org.gripday.authservice.presentation.web;

import jakarta.validation.Valid;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.authservice.domain.service.TenantManagementService;
import org.gripday.authservice.infrastructure.repository.dto.TenantDto.CreateTenantRequest;
import org.gripday.authservice.infrastructure.repository.dto.TenantDto.TenantResponse;
import org.gripday.authservice.infrastructure.repository.dto.TenantDto.TenantStatistics;
import org.gripday.authservice.infrastructure.repository.dto.TenantDto.TenantSummary;
import org.gripday.authservice.infrastructure.repository.dto.TenantDto.UpdateTenantRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant management operations. Provides admin-only APIs for tenant CRUD operations and management. All endpoints require SUPER_ADMIN role for access.
 */
@RestController
@RequestMapping("/api/v1/admin/tenants")
@Tag(name = "Tenant Management", description = "Admin-only tenant management operations")
@SecurityRequirement(name = "bearerAuth")
public class TenantManagementResource {

  private static final Logger logger = LoggerFactory.getLogger(TenantManagementResource.class);

  private final TenantManagementService tenantManagementService;

  public TenantManagementResource(final TenantManagementService tenantManagementService) {
    this.tenantManagementService = tenantManagementService;
  }

  /**
   * Create a new tenant. Requires SUPER_ADMIN role for access.
   */
  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Create new tenant",
      description = "Create a new tenant with specified configuration. Requires SUPER_ADMIN role."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Tenant created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data or tenant already exists"),
      @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN role required"),
      @ApiResponse(responseCode = "409", description = "Tenant with same ID, domain, or subdomain already exists")
  })
  public ResponseEntity<TenantResponse> createTenant(
      @Valid @RequestBody CreateTenantRequest request,
      Authentication authentication) {

    logger.info("Creating tenant: {} by user: {}", request.tenantId(), authentication.getName());

    try {
      var createdBy = authentication.getName();
      var tenantResponse = tenantManagementService.createTenant(request, createdBy);

      logger.info("Successfully created tenant: {}", tenantResponse.tenantId());
      return ResponseEntity.status(HttpStatus.CREATED).body(tenantResponse);

    } catch (IllegalArgumentException e) {
      logger.warn("Failed to create tenant {}: {}", request.tenantId(), e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  /**
   * Get tenant by ID. Requires SUPER_ADMIN role for access.
   */
  @GetMapping("/{tenantId}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Get tenant by ID",
      description = "Retrieve tenant information by tenant ID. Requires SUPER_ADMIN role."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant found"),
      @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN role required"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantResponse> getTenant(
      @Parameter(description = "Tenant ID", required = true)
      @PathVariable String tenantId) {

    logger.debug("Retrieving tenant: {}", tenantId);

    try {
      var tenantResponse = tenantManagementService.getTenant(tenantId);
      return ResponseEntity.ok(tenantResponse);

    } catch (IllegalArgumentException e) {
      logger.warn("Tenant not found: {}", tenantId);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Get all tenants with optional filtering. Requires SUPER_ADMIN role for access.
   */
  @GetMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Get all tenants",
      description = "Retrieve all tenants with optional filtering. Requires SUPER_ADMIN role."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenants retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN role required")
  })
  public ResponseEntity<List<TenantSummary>> getAllTenants(
      @Parameter(description = "Filter to show only enabled tenants")
      @RequestParam(defaultValue = "false") boolean enabledOnly) {

    logger.debug("Retrieving all tenants, enabledOnly: {}", enabledOnly);

    var tenants = tenantManagementService.getAllTenants(enabledOnly);
    return ResponseEntity.ok(tenants);
  }

  /**
   * Update tenant information. Requires SUPER_ADMIN role for access.
   */
  @PutMapping("/{tenantId}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Update tenant",
      description = "Update tenant information. Requires SUPER_ADMIN role."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN role required"),
      @ApiResponse(responseCode = "404", description = "Tenant not found"),
      @ApiResponse(responseCode = "409", description = "Domain or subdomain already exists")
  })
  public ResponseEntity<TenantResponse> updateTenant(
      @Parameter(description = "Tenant ID", required = true)
      @PathVariable String tenantId,
      @Valid @RequestBody UpdateTenantRequest request,
      Authentication authentication) {

    logger.info("Updating tenant: {} by user: {}", tenantId, authentication.getName());

    try {
      var tenantResponse = tenantManagementService.updateTenant(tenantId, request);

      logger.info("Successfully updated tenant: {}", tenantId);
      return ResponseEntity.ok(tenantResponse);

    } catch (IllegalArgumentException e) {
      logger.warn("Failed to update tenant {}: {}", tenantId, e.getMessage());

      // Determine appropriate status code based on error message
      if (e.getMessage().contains("not found")) {
        return ResponseEntity.notFound().build();
      } else if (e.getMessage().contains("already exists")) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      } else {
        return ResponseEntity.badRequest().build();
      }
    }
  }

  /**
   * Enable or disable a tenant. Requires SUPER_ADMIN role for access.
   */
  @PatchMapping("/{tenantId}/enabled")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Enable or disable tenant",
      description = "Enable or disable a tenant. Requires SUPER_ADMIN role."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tenant status updated successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN role required"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<TenantResponse> setTenantEnabled(
      @Parameter(description = "Tenant ID", required = true)
      @PathVariable String tenantId,
      @Parameter(description = "Enabled status", required = true)
      @RequestParam boolean enabled,
      Authentication authentication) {

    logger.info("Setting tenant {} enabled status to {} by user: {}",
        tenantId, enabled, authentication.getName());

    try {
      var tenantResponse = tenantManagementService.setTenantEnabled(tenantId, enabled);

      logger.info("Successfully updated tenant {} enabled status to: {}", tenantId, enabled);
      return ResponseEntity.ok(tenantResponse);

    } catch (IllegalArgumentException e) {
      logger.warn("Tenant not found: {}", tenantId);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Delete a tenant. Requires SUPER_ADMIN role for access.
   */
  @DeleteMapping("/{tenantId}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Delete tenant",
      description = "Delete a tenant and all associated data. Requires SUPER_ADMIN role."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Tenant deleted successfully"),
      @ApiResponse(responseCode = "400", description = "Cannot delete tenant with existing users"),
      @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN role required"),
      @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  public ResponseEntity<Void> deleteTenant(
      @Parameter(description = "Tenant ID", required = true)
      @PathVariable String tenantId,
      Authentication authentication) {

    logger.warn("Deleting tenant: {} by user: {}", tenantId, authentication.getName());

    try {
      tenantManagementService.deleteTenant(tenantId);

      logger.warn("Successfully deleted tenant: {}", tenantId);
      return ResponseEntity.noContent().build();

    } catch (IllegalArgumentException e) {
      logger.warn("Tenant not found: {}", tenantId);
      return ResponseEntity.notFound().build();

    } catch (IllegalStateException e) {
      logger.warn("Cannot delete tenant {}: {}", tenantId, e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get tenant statistics. Requires SUPER_ADMIN role for access.
   */
  @GetMapping("/statistics")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Get tenant statistics",
      description = "Retrieve tenant statistics including user counts and quota utilization. Requires SUPER_ADMIN role."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN role required")
  })
  public ResponseEntity<List<TenantStatistics>> getTenantStatistics() {

    logger.debug("Retrieving tenant statistics");

    var statistics = tenantManagementService.getTenantStatistics();
    return ResponseEntity.ok(statistics);
  }
}