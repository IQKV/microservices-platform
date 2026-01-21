package com.iqscaffold.billingservice.feature;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.iqscaffold.billingservice.shared.JsonListConverter;
import com.iqscaffold.billingservice.shared.JsonMapConverter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Platform-wide feature definition entity.
 *
 * <p>
 * Defines all available features that can be enabled in subscription plans.
 * Stored in public schema as features are shared across all tenants.
 *
 * <p>
 * Features support different types:
 * <ul>
 * <li>BOOLEAN - Simple on/off features</li>
 * <li>QUOTA - Usage-based features with limits</li>
 * <li>LIMIT - Capacity-based features</li>
 * <li>TIER - Multi-level features</li>
 * </ul>
 *
 * <p>
 * Example feature definitions:
 * 
 * <pre>
 * {
 *   "featureKey": "advanced_analytics",
 *   "type": "BOOLEAN",
 *   "displayName": "Advanced Analytics",
 *   "description": "Access to advanced reporting and analytics dashboards"
 * }
 *
 * {
 *   "featureKey": "api_calls_monthly",
 *   "type": "QUOTA",
 *   "displayName": "API Calls per Month",
 *   "metadata": {"defaultQuota": 10000, "unit": "calls"}
 * }
 * </pre>
 */
@Entity
@Table(name = "feature_definition", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.feature.FeatureDefinition")
public class FeatureDefinition {

  @Id
  @Column(name = "feature_key", nullable = false, length = 100)
  private String featureKey;

  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private FeatureType type;

  @Column(name = "category", length = 100)
  private String category;

  @Column(name = "metadata", columnDefinition = "text")
  @Convert(converter = JsonMapConverter.class)
  private Map<String, Object> metadata;

  @Column(name = "dependencies", columnDefinition = "text")
  @Convert(converter = JsonListConverter.class)
  private List<String> dependencies;

  @Column(name = "deprecated", nullable = false)
  private boolean deprecated = false;

  @Column(name = "deprecation_message", length = 500)
  private String deprecationMessage;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Column(name = "updated_by", length = 100)
  private String updatedBy;

  // Constructors
  public FeatureDefinition() {
  }

  public FeatureDefinition(final String featureKey, final String displayName, final String description,
      final FeatureType type) {
    this.featureKey = featureKey;
    this.displayName = displayName;
    this.description = description;
    this.type = type;
  }

  // Lifecycle callbacks
  @PrePersist
  protected void onCreate() {
    var now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  // Business methods

  /**
   * Checks if this feature has dependencies on other features.
   */
  public boolean hasDependencies() {
    return dependencies != null && !dependencies.isEmpty();
  }

  /**
   * Checks if this feature is a quota-based feature.
   */
  public boolean isQuotaFeature() {
    return type == FeatureType.QUOTA;
  }

  /**
   * Checks if this feature is a limit-based feature.
   */
  public boolean isLimitFeature() {
    return type == FeatureType.LIMIT;
  }

  /**
   * Gets the default quota value for quota-based features.
   */
  public Long getDefaultQuota() {
    if (!isQuotaFeature() || metadata == null) {
      return null;
    }
    Object quota = metadata.get("defaultQuota");
    return quota instanceof Number ? ((Number) quota).longValue() : null;
  }

  /**
   * Gets the default limit value for limit-based features.
   */
  public Long getDefaultLimit() {
    if (!isLimitFeature() || metadata == null) {
      return null;
    }
    Object limit = metadata.get("defaultLimit");
    return limit instanceof Number ? ((Number) limit).longValue() : null;
  }

  // Getters and setters
  public String getFeatureKey() {
    return featureKey;
  }

  public void setFeatureKey(String featureKey) {
    this.featureKey = featureKey;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public FeatureType getType() {
    return type;
  }

  public void setType(FeatureType type) {
    this.type = type;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

  public String getDeprecationMessage() {
    return deprecationMessage;
  }

  public void setDeprecationMessage(String deprecationMessage) {
    this.deprecationMessage = deprecationMessage;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureDefinition that = (FeatureDefinition) o;
    return Objects.equals(featureKey, that.featureKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureKey);
  }

  @Override
  public String toString() {
    return "FeatureDefinition{" +
        "featureKey='" + featureKey + '\'' +
        ", displayName='" + displayName + '\'' +
        ", type=" + type +
        ", deprecated=" + deprecated +
        '}';
  }
}
