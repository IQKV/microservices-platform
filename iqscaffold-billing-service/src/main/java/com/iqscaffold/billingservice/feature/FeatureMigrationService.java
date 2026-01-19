package com.iqscaffold.billingservice.feature;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.subscription.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.SubscriptionPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for migrating legacy JSON features to structured feature system.
 * 
 * <p>This service runs automatically on application startup to migrate existing
 * subscription plans that use the legacy JSON features field to the new
 * structured PlanFeature entities.
 * 
 * <p>Migration is idempotent and safe to run multiple times.
 */
@Service
@Transactional
public class FeatureMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureMigrationService.class);

  private final SubscriptionPlanRepository planRepository;
  private final FeatureDefinitionRepository featureDefinitionRepository;
  private final PlanFeatureRepository planFeatureRepository;
  private final ObjectMapper objectMapper;

  public FeatureMigrationService(
      SubscriptionPlanRepository planRepository,
      FeatureDefinitionRepository featureDefinitionRepository,
      PlanFeatureRepository planFeatureRepository,
      ObjectMapper objectMapper) {
    this.planRepository = planRepository;
    this.featureDefinitionRepository = featureDefinitionRepository;
    this.planFeatureRepository = planFeatureRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Migrates legacy JSON features to structured format on application startup.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void migrateLegacyFeatures() {
    logger.info("Starting migration of legacy JSON features to structured format");

    try {
      List<SubscriptionPlan> plansWithLegacyFeatures = planRepository.findPlansWithLegacyFeatures();
      
      if (plansWithLegacyFeatures.isEmpty()) {
        logger.info("No plans with legacy features found, migration not needed");
        return;
      }

      int migratedCount = 0;
      for (SubscriptionPlan plan : plansWithLegacyFeatures) {
        if (migratePlanFeatures(plan)) {
          migratedCount++;
        }
      }

      logger.info("Successfully migrated {} subscription plans from legacy features", migratedCount);

    } catch (Exception e) {
      logger.error("Error during feature migration: {}", e.getMessage(), e);
      // Don't fail application startup due to migration issues
    }
  }

  /**
   * Migrates features for a single subscription plan.
   */
  private boolean migratePlanFeatures(SubscriptionPlan plan) {
    try {
      String legacyFeatures = plan.getFeatures();
      if (legacyFeatures == null || legacyFeatures.trim().isEmpty()) {
        return false;
      }

      // Check if plan already has structured features
      long existingFeatureCount = planFeatureRepository.countEnabledFeaturesByPlanId(plan.getId());
      if (existingFeatureCount > 0) {
        logger.debug("Plan {} already has {} structured features, skipping migration", 
            plan.getName(), existingFeatureCount);
        return false;
      }

      // Parse legacy JSON features
      List<String> featureKeys = parseLegacyFeatures(legacyFeatures);
      if (featureKeys.isEmpty()) {
        logger.debug("No valid features found in legacy format for plan {}", plan.getName());
        return false;
      }

      // Create structured features
      int createdCount = 0;
      for (String featureKey : featureKeys) {
        if (createPlanFeature(plan, featureKey)) {
          createdCount++;
        }
      }

      logger.info("Migrated {} features for plan {} ({})", createdCount, plan.getName(), plan.getId());
      return createdCount > 0;

    } catch (Exception e) {
      logger.error("Error migrating features for plan {} ({}): {}", 
          plan.getName(), plan.getId(), e.getMessage(), e);
      return false;
    }
  }

  /**
   * Parses legacy JSON features into a list of feature keys.
   */
  private List<String> parseLegacyFeatures(String legacyFeatures) {
    try {
      // Handle different JSON formats
      if (legacyFeatures.startsWith("[")) {
        // Array format: ["feature1", "feature2"]
        return objectMapper.readValue(legacyFeatures, new TypeReference<List<String>>() {});
      } else if (legacyFeatures.startsWith("{")) {
        // Object format: {"features": ["feature1", "feature2"]}
        Map<String, Object> featureMap = objectMapper.readValue(legacyFeatures, 
            new TypeReference<Map<String, Object>>() {});
        Object features = featureMap.get("features");
        if (features instanceof List) {
          return (List<String>) features;
        }
      } else {
        // Comma-separated format: "feature1,feature2,feature3"
        return Arrays.asList(legacyFeatures.split(","));
      }
    } catch (Exception e) {
      logger.warn("Failed to parse legacy features '{}': {}", legacyFeatures, e.getMessage());
    }
    
    return List.of();
  }

  /**
   * Creates a PlanFeature entity for a feature key.
   */
  private boolean createPlanFeature(SubscriptionPlan plan, String featureKey) {
    try {
      // Clean up feature key
      featureKey = featureKey.trim();
      if (featureKey.isEmpty()) {
        return false;
      }

      // Check if feature definition exists
      Optional<FeatureDefinition> featureDefinition = featureDefinitionRepository.findByFeatureKey(featureKey);
      if (featureDefinition.isEmpty()) {
        logger.warn("Feature definition not found for key '{}', skipping", featureKey);
        return false;
      }

      // Check if plan feature already exists
      Optional<PlanFeature> existingPlanFeature = planFeatureRepository.findByPlanIdAndFeatureKey(
          plan.getId(), featureKey);
      if (existingPlanFeature.isPresent()) {
        logger.debug("Plan feature already exists for plan {} and feature {}", plan.getId(), featureKey);
        return false;
      }

      // Create new plan feature
      PlanFeature planFeature = new PlanFeature(plan, featureDefinition.get(), true);
      
      // Add default configuration based on feature type and plan quotas
      Map<String, Object> configuration = createDefaultConfiguration(plan, featureDefinition.get());
      if (!configuration.isEmpty()) {
        planFeature.setConfiguration(configuration);
      }

      planFeatureRepository.save(planFeature);
      logger.debug("Created plan feature for plan {} and feature {}", plan.getId(), featureKey);
      
      return true;

    } catch (Exception e) {
      logger.error("Error creating plan feature for plan {} and feature '{}': {}", 
          plan.getId(), featureKey, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Creates default configuration for a feature based on plan settings.
   */
  private Map<String, Object> createDefaultConfiguration(SubscriptionPlan plan, FeatureDefinition feature) {
    Map<String, Object> configuration = new java.util.HashMap<>();

    switch (feature.getType()) {
      case QUOTA -> {
        // Map plan quotas to feature configuration
        if ("api_calls_monthly".equals(feature.getFeatureKey()) && plan.getMaxApiCalls() != null) {
          configuration.put("quota", plan.getMaxApiCalls());
        } else if ("storage_quota".equals(feature.getFeatureKey()) && plan.getMaxStorageGb() != null) {
          configuration.put("quota", plan.getMaxStorageGb());
        }
      }
      case LIMIT -> {
        // Map plan limits to feature configuration
        if ("max_team_members".equals(feature.getFeatureKey()) && plan.getMaxUsers() != null) {
          configuration.put("limit", plan.getMaxUsers());
        }
      }
      case TIER -> {
        // Set default tier based on plan name
        String planName = plan.getName().toLowerCase();
        if (planName.contains("enterprise")) {
          configuration.put("tier", "enterprise");
        } else if (planName.contains("pro") || planName.contains("premium")) {
          configuration.put("tier", "premium");
        } else if (planName.contains("standard")) {
          configuration.put("tier", "standard");
        } else {
          configuration.put("tier", "basic");
        }
      }
    }

    return configuration;
  }

  /**
   * Manually trigger migration for a specific plan (for admin use).
   */
  public boolean migratePlan(String planId) {
    try {
      Optional<SubscriptionPlan> plan = planRepository.findById(java.util.UUID.fromString(planId));
      if (plan.isEmpty()) {
        logger.warn("Plan not found for ID: {}", planId);
        return false;
      }

      return migratePlanFeatures(plan.get());

    } catch (Exception e) {
      logger.error("Error migrating plan {}: {}", planId, e.getMessage(), e);
      return false;
    }
  }
}