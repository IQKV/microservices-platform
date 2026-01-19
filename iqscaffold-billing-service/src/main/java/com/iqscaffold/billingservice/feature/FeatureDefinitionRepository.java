package com.iqscaffold.billingservice.feature;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing feature definitions.
 * 
 * <p>Provides access to platform-wide feature definitions stored in the public schema.
 * Features are cached for performance as they are frequently accessed.
 */
@Repository
public interface FeatureDefinitionRepository extends JpaRepository<FeatureDefinition, String> {

  /**
   * Finds all active (non-deprecated) features ordered by sort order and display name.
   */
  @Query("SELECT f FROM FeatureDefinition f WHERE f.deprecated = false ORDER BY f.sortOrder ASC, f.displayName ASC")
  List<FeatureDefinition> findAllActive();

  /**
   * Finds features by category, ordered by sort order.
   */
  @Query("SELECT f FROM FeatureDefinition f WHERE f.category = :category AND f.deprecated = false ORDER BY f.sortOrder ASC, f.displayName ASC")
  List<FeatureDefinition> findByCategoryAndNotDeprecated(@Param("category") String category);

  /**
   * Finds features by type.
   */
  List<FeatureDefinition> findByTypeAndDeprecatedFalse(FeatureType type);

  /**
   * Finds a feature by key, including deprecated ones.
   */
  Optional<FeatureDefinition> findByFeatureKey(String featureKey);

  /**
   * Checks if a feature exists and is not deprecated.
   */
  @Query("SELECT COUNT(f) > 0 FROM FeatureDefinition f WHERE f.featureKey = :featureKey AND f.deprecated = false")
  boolean existsByFeatureKeyAndNotDeprecated(@Param("featureKey") String featureKey);

  /**
   * Finds features that have dependencies on the given feature.
   */
  @Query("SELECT f FROM FeatureDefinition f WHERE :featureKey MEMBER OF f.dependencies AND f.deprecated = false")
  List<FeatureDefinition> findDependentFeatures(@Param("featureKey") String featureKey);

  /**
   * Finds all distinct categories.
   */
  @Query("SELECT DISTINCT f.category FROM FeatureDefinition f WHERE f.category IS NOT NULL AND f.deprecated = false ORDER BY f.category")
  List<String> findAllCategories();

  /**
   * Finds features by multiple keys.
   */
  @Query("SELECT f FROM FeatureDefinition f WHERE f.featureKey IN :featureKeys AND f.deprecated = false")
  List<FeatureDefinition> findByFeatureKeysAndNotDeprecated(@Param("featureKeys") List<String> featureKeys);
}