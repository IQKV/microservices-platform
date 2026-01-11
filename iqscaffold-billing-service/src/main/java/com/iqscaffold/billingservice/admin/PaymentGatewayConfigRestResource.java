package com.iqscaffold.billingservice.admin;

import jakarta.validation.Valid;
import java.util.List;

import com.iqscaffold.billingservice.admin.dto.GatewayConfigDtos;
import com.iqscaffold.billingservice.shared.PaymentGatewayProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for managing payment gateway configurations per tenant.
 * <p>
 * All endpoints require administrative privileges (SUPER_ADMIN, TENANT_OWNER, or BILLING_ADMIN).
 * These endpoints allow tenants to configure their payment gateway credentials
 * and settings independently.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin/billing/gateway-config")
@Tag(name = "Payment Gateway Configuration", description = "Admin APIs for managing payment gateway configurations per tenant")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentGatewayConfigRestResource {

  private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayConfigRestResource.class);

  private final PaymentGatewayConfigService gatewayConfigService;

  public PaymentGatewayConfigRestResource(final PaymentGatewayConfigService gatewayConfigService) {
    this.gatewayConfigService = gatewayConfigService;
  }

  /**
   * Create a new payment gateway configuration for the current tenant.
   *
   * @param request Gateway configuration request
   * @return Created gateway configuration
   */
  @PostMapping
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  @Operation(
      summary = "Create gateway configuration",
      description = "Creates a new payment gateway configuration for the current tenant. " +
                    "The configuration data (API keys, secrets) is encrypted before storage."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Gateway configuration created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data or configuration already exists"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  })
  public ResponseEntity<GatewayConfigDtos.GatewayConfigResponse> createGatewayConfig(
      @Valid @RequestBody GatewayConfigDtos.CreateGatewayConfigRequest request) {
    
    logger.info("Creating gateway configuration for provider {}", request.gatewayProvider());
    
    GatewayConfigDtos.GatewayConfigResponse response = gatewayConfigService.createGatewayConfig(request);
    
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Update an existing payment gateway configuration.
   *
   * @param provider Gateway provider to update
   * @param request  Update request
   * @return Updated gateway configuration
   */
  @PutMapping("/{provider}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  @Operation(
      summary = "Update gateway configuration",
      description = "Updates an existing payment gateway configuration. " +
                    "Only provided fields will be updated."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Gateway configuration updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Gateway configuration not found")
  })
  public ResponseEntity<GatewayConfigDtos.GatewayConfigResponse> updateGatewayConfig(
      @Parameter(description = "Payment gateway provider", required = true)
      @PathVariable PaymentGatewayProvider provider,
      @Valid @RequestBody GatewayConfigDtos.UpdateGatewayConfigRequest request) {
    
    logger.info("Updating gateway configuration for provider {}", provider);
    
    GatewayConfigDtos.GatewayConfigResponse response = gatewayConfigService.updateGatewayConfig(provider, request);
    
    return ResponseEntity.ok(response);
  }

  /**
   * Get a specific gateway configuration.
   *
   * @param provider Gateway provider
   * @return Gateway configuration (with masked sensitive data)
   */
  @GetMapping("/{provider}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  @Operation(
      summary = "Get gateway configuration",
      description = "Retrieves a payment gateway configuration. Sensitive data is masked in the response."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Gateway configuration retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Gateway configuration not found")
  })
  public ResponseEntity<GatewayConfigDtos.GatewayConfigResponse> getGatewayConfig(
      @Parameter(description = "Payment gateway provider", required = true)
      @PathVariable PaymentGatewayProvider provider) {
    
    logger.debug("Retrieving gateway configuration for provider {}", provider);
    
    GatewayConfigDtos.GatewayConfigResponse response = gatewayConfigService.getGatewayConfig(provider);
    
    return ResponseEntity.ok(response);
  }

  /**
   * List all gateway configurations for the current tenant.
   *
   * @return List of gateway configuration summaries
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  @Operation(
      summary = "List all gateway configurations",
      description = "Lists all payment gateway configurations for the current tenant."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Gateway configurations retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  })
  public ResponseEntity<List<GatewayConfigDtos.GatewayConfigSummary>> listGatewayConfigs() {
    logger.debug("Listing all gateway configurations");
    
    List<GatewayConfigDtos.GatewayConfigSummary> summaries = gatewayConfigService.listGatewayConfigs();
    
    return ResponseEntity.ok(summaries);
  }

  /**
   * List active gateway configurations for the current tenant.
   *
   * @return List of active gateway configuration summaries
   */
  @GetMapping("/active")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  @Operation(
      summary = "List active gateway configurations",
      description = "Lists only active payment gateway configurations for the current tenant."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active gateway configurations retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  })
  public ResponseEntity<List<GatewayConfigDtos.GatewayConfigSummary>> listActiveGatewayConfigs() {
    logger.debug("Listing active gateway configurations");
    
    List<GatewayConfigDtos.GatewayConfigSummary> summaries = gatewayConfigService.listActiveGatewayConfigs();
    
    return ResponseEntity.ok(summaries);
  }

  /**
   * Get the primary gateway configuration for the current tenant.
   *
   * @return Primary gateway configuration
   */
  @GetMapping("/primary")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN', 'FINANCE_VIEWER')")
  @Operation(
      summary = "Get primary gateway configuration",
      description = "Retrieves the primary (default) payment gateway configuration for the current tenant."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Primary gateway configuration retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "No primary gateway configured")
  })
  public ResponseEntity<GatewayConfigDtos.GatewayConfigResponse> getPrimaryGatewayConfig() {
    logger.debug("Retrieving primary gateway configuration");
    
    return gatewayConfigService.getPrimaryGatewayConfig()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Activate a gateway for the current tenant.
   *
   * @param provider Gateway provider to activate
   * @return Status response
   */
  @PostMapping("/{provider}/activate")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  @Operation(
      summary = "Activate gateway",
      description = "Activates a payment gateway, allowing it to be used for processing payments."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Gateway activated successfully"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Gateway configuration not found")
  })
  public ResponseEntity<GatewayConfigDtos.GatewayStatusResponse> activateGateway(
      @Parameter(description = "Payment gateway provider", required = true)
      @PathVariable PaymentGatewayProvider provider) {
    
    logger.info("Activating gateway {}", provider);
    
    GatewayConfigDtos.GatewayStatusResponse response = gatewayConfigService.activateGateway(provider);
    
    return ResponseEntity.ok(response);
  }

  /**
   * Deactivate a gateway for the current tenant.
   *
   * @param provider Gateway provider to deactivate
   * @return Status response
   */
  @PostMapping("/{provider}/deactivate")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  @Operation(
      summary = "Deactivate gateway",
      description = "Deactivates a payment gateway. Cannot deactivate if it's the only active gateway."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Gateway deactivated successfully"),
      @ApiResponse(responseCode = "400", description = "Cannot deactivate the only active gateway"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Gateway configuration not found")
  })
  public ResponseEntity<GatewayConfigDtos.GatewayStatusResponse> deactivateGateway(
      @Parameter(description = "Payment gateway provider", required = true)
      @PathVariable PaymentGatewayProvider provider) {
    
    logger.info("Deactivating gateway {}", provider);
    
    GatewayConfigDtos.GatewayStatusResponse response = gatewayConfigService.deactivateGateway(provider);
    
    return ResponseEntity.ok(response);
  }

  /**
   * Set a gateway as the primary payment method for the tenant.
   *
   * @param provider Gateway provider to set as primary
   * @return Status response
   */
  @PostMapping("/{provider}/set-primary")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  @Operation(
      summary = "Set primary gateway",
      description = "Sets a gateway as the primary (default) payment method. " +
                    "Only one gateway can be primary at a time."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Gateway set as primary successfully"),
      @ApiResponse(responseCode = "400", description = "Gateway must be active to be set as primary"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Gateway configuration not found")
  })
  public ResponseEntity<GatewayConfigDtos.GatewayStatusResponse> setPrimaryGateway(
      @Parameter(description = "Payment gateway provider", required = true)
      @PathVariable PaymentGatewayProvider provider) {
    
    logger.info("Setting gateway {} as primary", provider);
    
    GatewayConfigDtos.GatewayStatusResponse response = gatewayConfigService.setPrimaryGateway(provider);
    
    return ResponseEntity.ok(response);
  }

  /**
   * Delete a gateway configuration.
   *
   * @param provider Gateway provider to delete
   * @return No content response
   */
  @DeleteMapping("/{provider}")
  @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TENANT_OWNER', 'BILLING_ADMIN')")
  @Operation(
      summary = "Delete gateway configuration",
      description = "Deletes a payment gateway configuration. Cannot delete if it's the only active gateway."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Gateway configuration deleted successfully"),
      @ApiResponse(responseCode = "400", description = "Cannot delete the only active gateway"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Gateway configuration not found")
  })
  public ResponseEntity<Void> deleteGatewayConfig(
      @Parameter(description = "Payment gateway provider", required = true)
      @PathVariable PaymentGatewayProvider provider) {
    
    logger.info("Deleting gateway configuration for provider {}", provider);
    
    gatewayConfigService.deleteGatewayConfig(provider);
    
    return ResponseEntity.noContent().build();
  }
}
