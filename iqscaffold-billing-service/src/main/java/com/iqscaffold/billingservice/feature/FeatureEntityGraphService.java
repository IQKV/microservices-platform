package com.iqscaffold.billingservice.feature;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service demonstrating optimal usage of entity graphs for Feature operations.
 * 
 * <p>This service showcases how to leverage entity graphs to optimize query performance
 * for feature-related operations by eagerly loading specific relationships based
 * on business requirements.
 *
 * <h3>Entity Graph Usage Patterns</h3>
 * <ul>
 *   <li><strong>Feature Configuration</strong> - Load plan features with definitions for setup</li>
 *   <li><strong>Access Control</strong> - Load features with plans for permission checking</li>
 *   <li><strong>Feature Management</strong> - Load complete feature context for administration</li>
 * </ul>
 *
 * <h3>Business Use Cases</h3>
 * <ul>
 *   <li>Feature availability checking</li>
 *   <li>Quota and limit enforcement</li>
 *   <li>Plan feature configuration</li>
 *   <li>Feature usage analytics</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class FeatureEntityGraphService {

  private final PlanFeatureRepository planFeatureRepository;
  private final FeatureDefinitionRepository featureDefinitionRepository;

  public FeatureEntityGraphService(
      final PlanFeatureRepository planFeatureRepository,
      final FeatureDefinitionRepository featureDefinitionRepository) {
    this.planFeatureRepository = planFeatureRepository;
    this.featureDefinitionRepository = featureDefinitionRepository;
  }

  /**
   * Find plan feature with complete context for feature management.
   * Use this when configuring or managing plan features.
   *
   * @param planId the plan ID
   * @param featureKey the feature key
   * @return Optional containing plan feature with complete context if found
   */
  public Optional<PlanFeature> findPlanFeatureForManagement(final UUID planId, final String featureKey) {
    var id = new PlanFeature.PlanFeatureId(planId, featureKey);
    return planFeatureRepository.findById(id); // Uses planfeature-complete entity graph
  }

  /**
   * Find all plan features with definitions for a specific plan.
   * Use this when displaying plan capabilities or configuring features.
   *
   * @param planId the plan ID
   * @return List of plan features with definitions loaded
   */
  public List<PlanFeature> findPlanFeaturesWithDefinitions(final UUID planId) {
    return planFeatureRepository.findAll().stream()
        .filter(pf -> pf.getPlan().getId().equals(planId))
        .collect(Collectors.toList());
  }

  /**
   * Get feature configuration summary for a plan.
   * Loads plan features with definitions for complete feature context.
   *
   * @param planId the plan ID
   * @return FeatureConfigurationSummary containing feature details
   */
  public FeatureConfigurationSummary getFeatureConfigurationSummary(final UUID planId) {
    var planFeatures = findPlanFeaturesWithDefinitions(planId);
    
    var enabledFeatures = planFeatures.stream()
        .filter(PlanFeature::isEnabled)
        .collect(Collectors.toMap(
            pf -> pf.getFeature().getFeatureKey(),
            pf -> new FeatureConfig(
                pf.getFeature().getDisplayName(),
                pf.getFeature().getType(),
                pf.getQuota(),
                pf.getLimit(),
                pf.getTier(),
                pf.hasCustomConfiguration()
            )
        ));

    var totalFeatures = planFeatures.size();
    var enabledCount = (int) planFeatures.stream().filter(PlanFeature::isEnabled).count();

    return new FeatureConfigurationSummary(
        planId,
        totalFeatures,
        enabledCount,
        enabledFeatures
    );
  }

  /**
   * Check if a specific feature type is available in a plan.
   * Optimized method for feature type checking.
   *
   * @param planId the plan ID
   * @param featureType the feature type to check
   * @return true if plan has enabled features of the specified type
   */
  public boolean hasFeaturesOfType(final UUID planId, final FeatureType featureType) {
    return findPlanFeaturesWithDefinitions(planId).stream()
        .anyMatch(pf -> pf.isEnabled() && pf.getFeature().getType() == featureType);
  }

  /**
   * Get quota-based features for a plan.
   * Returns features with their quota configurations.
   *
   * @param planId the plan ID
   * @return Map of feature key to quota value for quota-based features
   */
  public Map<String, Long> getQuotaFeatures(final UUID planId) {
    return findPlanFeaturesWithDefinitions(planId).stream()
        .filter(pf -> pf.isEnabled() && pf.getFeature().isQuotaFeature())
        .collect(Collectors.toMap(
            pf -> pf.getFeature().getFeatureKey(),
            PlanFeature::getQuota,
            (existing, replacement) -> existing // Handle duplicates
        ));
  }

  /**
   * Get limit-based features for a plan.
   * Returns features with their limit configurations.
   *
   * @param planId the plan ID
   * @return Map of feature key to limit value for limit-based features
   */
  public Map<String, Long> getLimitFeatures(final UUID planId) {
    return findPlanFeaturesWithDefinitions(planId).stream()
        .filter(pf -> pf.isEnabled() && pf.getFeature().isLimitFeature())
        .collect(Collectors.toMap(
            pf -> pf.getFeature().getFeatureKey(),
            PlanFeature::getLimit,
            (existing, replacement) -> existing // Handle duplicates
        ));
  }

  /**
   * Get boolean features for a plan.
   * Returns simple on/off features.
   *
   * @param planId the plan ID
   * @return List of enabled boolean feature keys
   */
  public List<String> getBooleanFeatures(final UUID planId) {
    return findPlanFeaturesWithDefinitions(planId).stream()
        .filter(pf -> pf.isEnabled() && pf.getFeature().getType() == FeatureType.BOOLEAN)
        .map(pf -> pf.getFeature().getFeatureKey())
        .collect(Collectors.toList());
  }

  /**
   * Data transfer object for feature configuration information.
   */
  public record FeatureConfig(
      String displayName,
      FeatureType type,
      Long quota,
      Long limit,
      String tier,
      boolean hasCustomConfig
  ) {}

  /**
   * Data transfer object for feature configuration summary.
   */
  public record FeatureConfigurationSummary(
      UUID planId,
      int totalFeatures,
      int enabledFeatures,
      Map<String, FeatureConfig> features
  ) {}
}