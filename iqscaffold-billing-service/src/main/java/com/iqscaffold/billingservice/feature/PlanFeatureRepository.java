package com.iqscaffold.billingservice.feature;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing plan-feature associations.
 *
 * <p>Provides access to the relationships between subscription plans and features,
 * including their specific configurations and enablement status.
 */
@Repository
public interface PlanFeatureRepository extends JpaRepository<PlanFeature, PlanFeature.PlanFeatureId> {

  /**
   * Finds all enabled features for a specific plan.
   */
  @Query("SELECT pf FROM PlanFeature pf JOIN FETCH pf.feature f WHERE pf.plan.id = :planId AND pf.enabled = true AND f.deprecated = false ORDER BY f.sortOrder ASC, f.displayName ASC")
  List<PlanFeature> findEnabledFeaturesByPlanId(@Param("planId") UUID planId);

  /**
   * Finds all features (enabled and disabled) for a specific plan.
   */
  @Query("SELECT pf FROM PlanFeature pf JOIN FETCH pf.feature f WHERE pf.plan.id = :planId ORDER BY f.sortOrder ASC, f.displayName ASC")
  List<PlanFeature> findAllFeaturesByPlanId(@Param("planId") UUID planId);

  /**
   * Finds a specific plan-feature association.
   */
  @Query("SELECT pf FROM PlanFeature pf JOIN FETCH pf.feature f WHERE pf.plan.id = :planId AND pf.feature.featureKey = :featureKey")
  Optional<PlanFeature> findByPlanIdAndFeatureKey(@Param("planId") UUID planId, @Param("featureKey") String featureKey);

  /**
   * Checks if a feature is enabled for a specific plan.
   */
  @Query("SELECT COUNT(pf) > 0 FROM PlanFeature pf WHERE pf.plan.id = :planId AND pf.feature.featureKey = :featureKey AND pf.enabled = true")
  boolean isFeatureEnabledForPlan(@Param("planId") UUID planId, @Param("featureKey") String featureKey);

  /**
   * Finds all plans that have a specific feature enabled.
   */
  @Query("SELECT pf FROM PlanFeature pf JOIN FETCH pf.plan p WHERE pf.feature.featureKey = :featureKey AND pf.enabled = true AND p.isActive = true")
  List<PlanFeature> findPlansByEnabledFeature(@Param("featureKey") String featureKey);

  /**
   * Finds features by type for a specific plan.
   */
  @Query("SELECT pf FROM PlanFeature pf JOIN FETCH pf.feature f WHERE pf.plan.id = :planId AND f.type = :featureType AND pf.enabled = true AND f.deprecated = false")
  List<PlanFeature> findEnabledFeaturesByPlanIdAndType(@Param("planId") UUID planId, @Param("featureType") FeatureType featureType);

  /**
   * Finds quota-based features for a specific plan.
   */
  @Query("SELECT pf FROM PlanFeature pf JOIN FETCH pf.feature f WHERE pf.plan.id = :planId AND f.type = 'QUOTA' AND pf.enabled = true AND f.deprecated = false")
  List<PlanFeature> findQuotaFeaturesByPlanId(@Param("planId") UUID planId);

  /**
   * Finds limit-based features for a specific plan.
   */
  @Query("SELECT pf FROM PlanFeature pf JOIN FETCH pf.feature f WHERE pf.plan.id = :planId AND f.type = 'LIMIT' AND pf.enabled = true AND f.deprecated = false")
  List<PlanFeature> findLimitFeaturesByPlanId(@Param("planId") UUID planId);

  /**
   * Deletes all features for a specific plan.
   */
  void deleteByPlanId(UUID planId);

  /**
   * Counts enabled features for a plan.
   */
  @Query("SELECT COUNT(pf) FROM PlanFeature pf WHERE pf.plan.id = :planId AND pf.enabled = true")
  long countEnabledFeaturesByPlanId(@Param("planId") UUID planId);

  /**
   * Finds plans that use a specific feature (for impact analysis when deprecating features).
   */
  @Query("SELECT DISTINCT pf.plan.id FROM PlanFeature pf WHERE pf.feature.featureKey = :featureKey AND pf.enabled = true")
  List<UUID> findPlanIdsUsingFeature(@Param("featureKey") String featureKey);
}
