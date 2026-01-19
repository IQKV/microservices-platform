package com.iqscaffold.billingservice.feature;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API for feature management used by other services.
 *
 * <p>This controller provides internal endpoints for the gateway and other services
 * to check feature enablement and track usage. These endpoints are not exposed
 * to external clients and should only be accessible within the service mesh.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/v1/internal/features/context/{tenantId} - Get feature context</li>
 *   <li>POST /api/v1/internal/features/usage - Record feature usage</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/internal/features")
@Tag(name = "Internal Feature API", description = "Internal feature management endpoints for service-to-service communication")
@Hidden // Hide from public API documentation
public class FeatureInternalResource {

  private static final Logger logger = LoggerFactory.getLogger(FeatureInternalResource.class);

  private final FeatureEnablementService featureEnablementService;
  private final FeatureUsageTrackingService usageTrackingService;

  public FeatureInternalResource(
      final FeatureEnablementService featureEnablementService,
      final FeatureUsageTrackingService usageTrackingService) {
    this.featureEnablementService = featureEnablementService;
    this.usageTrackingService = usageTrackingService;
  }

  /**
   * Gets the complete feature context for a tenant.
   * Used by the gateway to validate feature access and propagate context.
   *
   * @param tenantId the tenant identifier
   * @return feature context with enabled features, quotas, and limits
   */
  @GetMapping("/context/{tenantId}")
  @Operation(
      summary = "Get feature context for tenant",
      description = "Returns the complete feature context including enabled features, quotas, limits, and plan information"
  )
  public ResponseEntity<FeatureContext> getFeatureContext(
      @Parameter(description = "Tenant identifier", required = true)
      @PathVariable final String tenantId) {

    logger.debug("Getting feature context for tenant: {}", tenantId);

    try {
      FeatureContext context = featureEnablementService.getFeatureContext(tenantId);

      logger.debug("Retrieved feature context for tenant {}: {} features enabled",
          tenantId, context.getEnabledFeatureCount());

      return ResponseEntity.ok(context);

    } catch (final Exception e) {
      logger.error("Error retrieving feature context for tenant {}: {}", tenantId, e.getMessage(), e);

      // Return empty context on error to avoid blocking requests
      return ResponseEntity.ok(FeatureContext.empty(tenantId));
    }
  }

  /**
   * Records feature usage for analytics and billing.
   * Used by the gateway and other services to track feature usage.
   *
   * @param usageRequest the usage tracking request
   * @return success response
   */
  @PostMapping("/usage")
  @Operation(
      summary = "Record feature usage",
      description = "Records feature usage for analytics and billing purposes"
  )
  public ResponseEntity<Void> recordFeatureUsage(@RequestBody FeatureUsageRequest usageRequest) {

    logger.debug("Recording feature usage: tenant={}, feature={}, endpoint={}",
        usageRequest.getTenantId(), usageRequest.getFeatureKey(), usageRequest.getEndpoint());

    try {
      // Create metadata map from request
      java.util.Map<String, Object> metadata = new java.util.HashMap<>();
      if (usageRequest.getCorrelationId() != null) {
        metadata.put("correlationId", usageRequest.getCorrelationId());
      }
      if (usageRequest.getUserId() != null) {
        metadata.put("userId", usageRequest.getUserId());
      }

      // Record usage with metadata
      if (!metadata.isEmpty()) {
        usageTrackingService.recordUsage(
            usageRequest.getTenantId(),
            usageRequest.getFeatureKey(),
            usageRequest.getEndpoint(),
            metadata
        );
      } else {
        usageTrackingService.recordUsage(
            usageRequest.getTenantId(),
            usageRequest.getFeatureKey(),
            usageRequest.getEndpoint()
        );
      }

      return ResponseEntity.ok().build();

    } catch (final Exception e) {
      logger.error("Error recording feature usage for tenant {} and feature {}: {}",
          usageRequest.getTenantId(), usageRequest.getFeatureKey(), e.getMessage(), e);

      // Don't fail the request if usage tracking fails
      return ResponseEntity.ok().build();
    }
  }

  /**
   * Checks if a specific feature is enabled for a tenant.
   * Used for simple feature checks without full context.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the feature key to check
   * @return feature enablement status
   */
  @GetMapping("/enabled/{tenantId}/{featureKey}")
  @Operation(
      summary = "Check if feature is enabled",
      description = "Checks if a specific feature is enabled for a tenant"
  )
  public ResponseEntity<FeatureEnabledResponse> isFeatureEnabled(
      @Parameter(description = "Tenant identifier", required = true)
      @PathVariable final String tenantId,
      @Parameter(description = "Feature key to check", required = true)
      @PathVariable final String featureKey) {

    logger.debug("Checking feature enablement: tenant={}, feature={}", tenantId, featureKey);

    try {
      boolean enabled = featureEnablementService.isFeatureEnabled(tenantId, featureKey);

      return ResponseEntity.ok(new FeatureEnabledResponse(enabled, featureKey, tenantId));

    } catch (final Exception e) {
      logger.error("Error checking feature enablement for tenant {} and feature {}: {}",
          tenantId, featureKey, e.getMessage(), e);

      // Return false on error to fail closed
      return ResponseEntity.ok(new FeatureEnabledResponse(false, featureKey, tenantId));
    }
  }

  /**
   * Request DTO for feature usage tracking.
   */
  public static class FeatureUsageRequest {
    private String tenantId;
    private String featureKey;
    private String endpoint;
    private String userId;
    private String correlationId;

    // Default constructor for JSON deserialization
    public FeatureUsageRequest() {
    }

    public FeatureUsageRequest(final String tenantId, final String featureKey, final String endpoint) {
      this.tenantId = tenantId;
      this.featureKey = featureKey;
      this.endpoint = endpoint;
    }

    // Getters and setters
    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getFeatureKey() {
      return featureKey;
    }

    public void setFeatureKey(String featureKey) {
      this.featureKey = featureKey;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getCorrelationId() {
      return correlationId;
    }

    public void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
    }
  }

  /**
   * Response DTO for feature enablement checks.
   */
  public static class FeatureEnabledResponse {
    private boolean enabled;
    private String featureKey;
    private String tenantId;

    public FeatureEnabledResponse() {
    }

    public FeatureEnabledResponse(final boolean enabled, final String featureKey, final String tenantId) {
      this.enabled = enabled;
      this.featureKey = featureKey;
      this.tenantId = tenantId;
    }

    // Getters and setters
    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getFeatureKey() {
      return featureKey;
    }

    public void setFeatureKey(String featureKey) {
      this.featureKey = featureKey;
    }

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }
  }
}
