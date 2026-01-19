package com.iqscaffold.billingservice.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.subscription.TenantSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service for feature enablement and validation.
 *
 * <p>This service provides the primary interface for checking feature availability
 * and building feature contexts for tenants based on their subscription plans.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Feature enablement checking</li>
 *   <li>Feature context building</li>
 *   <li>Feature dependency validation</li>
 *   <li>Feature usage tracking</li>
 *   <li>Caching for performance</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class FeatureEnablementService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureEnablementService.class);

  private final TenantSubscriptionRepository subscriptionRepository;
  private final PlanFeatureRepository planFeatureRepository;
  private final FeatureDefinitionRepository featureDefinitionRepository;
  private final FeatureUsageTrackingService usageTrackingService;

  // In-memory cache for feature definitions (rarely change)
  private final Map<String, FeatureDefinition> featureDefinitionCache = new ConcurrentHashMap<>();

  public FeatureEnablementService(
      final TenantSubscriptionRepository subscriptionRepository,
      final PlanFeatureRepository planFeatureRepository,
      final FeatureDefinitionRepository featureDefinitionRepository,
      final FeatureUsageTrackingService usageTrackingService) {
    this.subscriptionRepository = subscriptionRepository;
    this.planFeatureRepository = planFeatureRepository;
    this.featureDefinitionRepository = featureDefinitionRepository;
    this.usageTrackingService = usageTrackingService;
  }

  /**
   * Checks if a specific feature is enabled for a tenant.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the feature key to check
   * @return true if the feature is enabled, false otherwise
   */
  @Cacheable(value = "featureEnablement", key = "#tenantId + ':' + #featureKey")
  public boolean isFeatureEnabled(String tenantId, String featureKey) {
    logger.debug("Checking feature enablement for tenant {} and feature {}", tenantId, featureKey);

    try {
      // Get tenant's active subscription
      Optional<TenantSubscription> subscription = subscriptionRepository.findActiveSubscriptionByTenantId(tenantId);
      if (subscription.isEmpty()) {
        logger.debug("No active subscription found for tenant {}", tenantId);
        return false;
      }

      // Check if feature is enabled in the plan
      UUID planId = subscription.get().getPlan().getId();
      boolean enabled = planFeatureRepository.isFeatureEnabledForPlan(planId, featureKey);

      if (enabled) {
        // Validate feature dependencies
        enabled = validateFeatureDependencies(tenantId, featureKey, planId);
      }

      logger.debug("Feature {} is {} for tenant {}", featureKey, enabled ? "enabled" : "disabled", tenantId);
      return enabled;

    } catch (final Exception e) {
      logger.error("Error checking feature enablement for tenant {} and feature {}: {}",
          tenantId, featureKey, e.getMessage(), e);
      return false; // Fail closed for security
    }
  }

  /**
   * Gets the complete feature context for a tenant.
   *
   * @param tenantId the tenant identifier
   * @return the feature context containing all enabled features, quotas, and limits
   */
  @Cacheable(value = "featureContext", key = "#tenantId")
  public FeatureContext getFeatureContext(String tenantId) {
    logger.debug("Building feature context for tenant {}", tenantId);

    try {
      // Get tenant's active subscription
      Optional<TenantSubscription> subscription = subscriptionRepository.findActiveSubscriptionByTenantId(tenantId);
      if (subscription.isEmpty()) {
        logger.debug("No active subscription found for tenant {}, returning empty context", tenantId);
        return FeatureContext.empty(tenantId);
      }

      TenantSubscription tenantSubscription = subscription.get();
      UUID planId = tenantSubscription.getPlan().getId();

      // Get all enabled features for the plan
      List<PlanFeature> planFeatures = planFeatureRepository.findEnabledFeaturesByPlanId(planId);

      // Build feature context
      FeatureContext.Builder contextBuilder = FeatureContext.builder(tenantId)
          .planId(planId.toString())
          .planName(tenantSubscription.getPlan().getName());

      Set<String> enabledFeatures = new HashSet<>();

      for (final PlanFeature planFeature : planFeatures) {
        FeatureDefinition feature = planFeature.getFeature();
        String featureKey = feature.getFeatureKey();

        // Validate dependencies before including feature
        if (!validateFeatureDependencies(tenantId, featureKey, planId)) {
          logger.warn("Feature {} disabled for tenant {} due to missing dependencies", featureKey, tenantId);
          continue;
        }

        switch (feature.getType()) {
          case BOOLEAN -> enabledFeatures.add(featureKey);
          case QUOTA -> {
            Long quota = planFeature.getQuota();
            if (quota != null) {
              contextBuilder.addQuota(featureKey, quota);
            }
          }
          case LIMIT -> {
            Long limit = planFeature.getLimit();
            if (limit != null) {
              contextBuilder.addLimit(featureKey, limit);
            }
          }
          case TIER -> {
            String tier = planFeature.getTier();
            if (tier != null) {
              contextBuilder.addTier(featureKey, tier);
            }
          }
          default -> logger.warn("Unknown feature type {} for feature {}", feature.getType(), featureKey);
        }
      }

      contextBuilder.enabledFeatures(enabledFeatures);

      FeatureContext context = contextBuilder.build();
      logger.debug("Built feature context for tenant {} with {} features", tenantId, context.getEnabledFeatureCount());

      return context;

    } catch (final Exception e) {
      logger.error("Error building feature context for tenant {}: {}", tenantId, e.getMessage(), e);
      return FeatureContext.empty(tenantId); // Fail closed
    }
  }

  /**
   * Records feature usage for analytics and billing.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the feature that was used
   * @param endpoint   the endpoint where the feature was used (optional)
   */
  public void recordFeatureUsage(String tenantId, String featureKey, String endpoint) {
    try {
      usageTrackingService.recordUsage(tenantId, featureKey, endpoint);
    } catch (final Exception e) {
      logger.error("Error recording feature usage for tenant {} and feature {}: {}",
          tenantId, featureKey, e.getMessage(), e);
      // Don't fail the request if usage tracking fails
    }
  }

  /**
   * Gets quota information for a specific feature.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the quota-based feature key
   * @return the quota value, or null if not configured
   */
  public Long getFeatureQuota(String tenantId, String featureKey) {
    FeatureContext context = getFeatureContext(tenantId);
    return context.getQuota(featureKey);
  }

  /**
   * Gets limit information for a specific feature.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the limit-based feature key
   * @return the limit value, or null if not configured
   */
  public Long getFeatureLimit(String tenantId, String featureKey) {
    FeatureContext context = getFeatureContext(tenantId);
    return context.getLimit(featureKey);
  }

  /**
   * Validates that all dependencies for a feature are satisfied.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the feature to validate
   * @param planId     the subscription plan ID
   * @return true if all dependencies are satisfied
   */
  private boolean validateFeatureDependencies(String tenantId, String featureKey, UUID planId) {
    FeatureDefinition feature = getFeatureDefinition(featureKey);
    if (feature == null || !feature.hasDependencies()) {
      return true; // No dependencies to validate
    }

    for (final String dependencyKey : feature.getDependencies()) {
      if (!planFeatureRepository.isFeatureEnabledForPlan(planId, dependencyKey)) {
        logger.warn("Feature {} dependency {} not satisfied for tenant {}", featureKey, dependencyKey, tenantId);
        return false;
      }
    }

    return true;
  }

  /**
   * Gets a feature definition, using cache for performance.
   */
  private FeatureDefinition getFeatureDefinition(String featureKey) {
    return featureDefinitionCache.computeIfAbsent(featureKey, key -> {
      Optional<FeatureDefinition> definition = featureDefinitionRepository.findByFeatureKey(key);
      return definition.orElse(null);
    });
  }

  /**
   * Clears the feature definition cache (for testing or admin operations).
   */
  public void clearFeatureDefinitionCache() {
    featureDefinitionCache.clear();
  }

  /**
   * Gets all available features (for admin/management purposes).
   */
  public List<FeatureDefinition> getAllFeatures() {
    return featureDefinitionRepository.findAllActive();
  }

  /**
   * Gets features by category.
   */
  public List<FeatureDefinition> getFeaturesByCategory(String category) {
    return featureDefinitionRepository.findByCategoryAndNotDeprecated(category);
  }

  /**
   * Gets all feature categories.
   */
  public List<String> getAllCategories() {
    return featureDefinitionRepository.findAllCategories();
  }
}
