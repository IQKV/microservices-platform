package com.iqscaffold.billingservice.feature;

import java.util.List;
import java.util.stream.Collectors;

import com.iqscaffold.billingservice.feature.dto.FeatureDto;
import com.iqscaffold.billingservice.feature.dto.UserFeaturesResponse;
import com.iqscaffold.billingservice.subscription.SubscriptionService;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for feature management accessible to frontend applications.
 * <p>
 * Provides endpoints for React and other frontend applications to:
 * <ul>
 *   <li>Get user's enabled features based on subscription plan</li>
 *   <li>Get all available features with their status</li>
 *   <li>Get subscription plan information</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 *   <li>Feature access: Requires authenticated user (USER role)</li>
 *   <li>Tenant isolation: Features are automatically scoped to current tenant</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // React component
 * const { data: features } = useFetch('/api/v1/features/my-features');
 *
 * if (features.enabledFeatures.some(f => f.code === 'advanced_analytics')) {
 *   // Show advanced analytics UI
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/v1/features")
@Tag(name = "Features", description = "Feature management for frontend applications")
@SecurityRequirement(name = "bearerAuth")
public class FeatureResource {

  private static final Logger logger = LoggerFactory.getLogger(FeatureResource.class);

  private final FeatureEnablementService featureEnablementService;
  private final SubscriptionService subscriptionService;

  public FeatureResource(
      final FeatureEnablementService featureEnablementService,
      final SubscriptionService subscriptionService) {
    this.featureEnablementService = featureEnablementService;
    this.subscriptionService = subscriptionService;
  }

  @Operation(
      summary = "Get user's features",
      description = "Retrieves all features available to the current user based on their subscription plan")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Features retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
      @ApiResponse(responseCode = "404", description = "No active subscription found")
  })
  @GetMapping("/my-features")
  @PreAuthorize("hasAuthority('USER')")
  public ResponseEntity<UserFeaturesResponse> getMyFeatures() {
    String tenantId = TenantContext.getCurrentTenantId();

    if (tenantId == null) {
      logger.warn("No tenant context available for feature request");
      return ResponseEntity.badRequest().build();
    }

    logger.debug("Retrieving features for tenant: {}", tenantId);

    try {
      // Get active subscription
      var activeSubscription = subscriptionService.getActiveSubscription();
      if (activeSubscription.isEmpty()) {
        logger.debug("No active subscription found for tenant: {}", tenantId);
        return ResponseEntity.notFound().build();
      }

      var subscription = activeSubscription.get();

      // Get feature context for the tenant
      FeatureContext featureContext = featureEnablementService.getFeatureContext(tenantId);

      // Get all available features
      List<FeatureDefinition> allFeatures = featureEnablementService.getAllFeatures();

      // Build enabled features list
      List<FeatureDto> enabledFeatures = allFeatures.stream()
          .filter(feature -> featureContext.isFeatureEnabled(feature.getFeatureKey()))
          .map(feature -> createFeatureDto(feature, featureContext, true))
          .collect(Collectors.toList());

      // Build all features list with status
      List<FeatureDto> allFeaturesWithStatus = allFeatures.stream()
          .map(feature -> {
            boolean enabled = featureContext.isFeatureEnabled(feature.getFeatureKey());
            return createFeatureDto(feature, featureContext, enabled);
          })
          .collect(Collectors.toList());

      // Build response
      UserFeaturesResponse response = new UserFeaturesResponse(
          enabledFeatures,
          allFeaturesWithStatus,
          subscription.planName(),
          subscription.status(),
          subscription.currentPeriodEnd(),
          subscription.trialStart() != null,
          subscription.trialEnd(),
          tenantId
      );

      logger.debug("Retrieved {} enabled features out of {} total features for tenant: {}",
          enabledFeatures.size(), allFeatures.size(), tenantId);

      return ResponseEntity.ok(response);

    } catch (final Exception e) {
      logger.error("Error retrieving features for tenant {}: {}", tenantId, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @Operation(
      summary = "Get enabled features only",
      description = "Retrieves only the features that are enabled for the current user (lightweight response)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Enabled features retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
      @ApiResponse(responseCode = "404", description = "No active subscription found")
  })
  @GetMapping("/enabled")
  @PreAuthorize("hasAuthority('USER')")
  public ResponseEntity<List<FeatureDto>> getEnabledFeatures() {
    String tenantId = TenantContext.getCurrentTenantId();

    if (tenantId == null) {
      logger.warn("No tenant context available for enabled features request");
      return ResponseEntity.badRequest().build();
    }

    logger.debug("Retrieving enabled features for tenant: {}", tenantId);

    try {
      // Check if tenant has active subscription
      var activeSubscription = subscriptionService.getActiveSubscription();
      if (activeSubscription.isEmpty()) {
        logger.debug("No active subscription found for tenant: {}", tenantId);
        return ResponseEntity.notFound().build();
      }

      // Get feature context for the tenant
      FeatureContext featureContext = featureEnablementService.getFeatureContext(tenantId);

      // Get all available features and filter enabled ones
      List<FeatureDto> enabledFeatures = featureEnablementService.getAllFeatures().stream()
          .filter(feature -> featureContext.isFeatureEnabled(feature.getFeatureKey()))
          .map(feature -> createFeatureDto(feature, featureContext, true))
          .collect(Collectors.toList());

      logger.debug("Retrieved {} enabled features for tenant: {}", enabledFeatures.size(), tenantId);

      return ResponseEntity.ok(enabledFeatures);

    } catch (final Exception e) {
      logger.error("Error retrieving enabled features for tenant {}: {}", tenantId, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Creates a FeatureDto from a FeatureDefinition with context information.
   */
  private FeatureDto createFeatureDto(FeatureDefinition feature, FeatureContext context, boolean enabled) {
    Integer usageLimit = null;
    Integer currentUsage = 0;

    // Get usage information based on feature type
    switch (feature.getType()) {
      case QUOTA -> {
        Long quota = context.getQuota(feature.getFeatureKey());
        usageLimit = quota != null ? quota.intValue() : null;
        // TODO: Get current usage from usage tracking service
        // currentUsage = usageTrackingService.getCurrentUsage(tenantId, feature.getFeatureKey());
      }
      case LIMIT -> {
        Long limit = context.getLimit(feature.getFeatureKey());
        usageLimit = limit != null ? limit.intValue() : null;
        // TODO: Get current usage from usage tracking service
        // currentUsage = usageTrackingService.getCurrentUsage(tenantId, feature.getFeatureKey());
      }
      default -> {
        // BOOLEAN and TIER features don't have usage limits
      }
    }

    return new FeatureDto(
        feature.getFeatureKey(),
        feature.getDisplayName(),
        feature.getDescription(),
        feature.getCategory(),
        enabled,
        usageLimit,
        currentUsage
    );
  }
}
