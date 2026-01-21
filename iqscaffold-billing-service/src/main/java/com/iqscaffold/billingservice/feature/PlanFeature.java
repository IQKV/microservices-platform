package com.iqscaffold.billingservice.feature;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.iqscaffold.billingservice.subscription.SubscriptionPlan;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Association entity between subscription plans and features.
 *
 * <p>Defines which features are enabled for each subscription plan and their specific configuration.
 * Stored in public schema as plan-feature associations are shared across all tenants.
 *
 * <p>The configuration field allows plan-specific customization of features:
 * <ul>
 *   <li>For QUOTA features: {"quota": 50000, "resetPeriod": "monthly"}</li>
 *   <li>For LIMIT features: {"limit": 100, "unit": "users"}</li>
 *   <li>For TIER features: {"tier": "premium", "level": 2}</li>
 *   <li>For BOOLEAN features: {} (empty configuration)</li>
 * </ul>
 *
 * <h3>Entity Graphs</h3>
 * <ul>
 *   <li><strong>planfeature-with-plan</strong> - Eagerly loads subscription plan for billing operations</li>
 *   <li><strong>planfeature-with-feature</strong> - Eagerly loads feature definition for feature checking</li>
 *   <li><strong>planfeature-complete</strong> - Loads both plan and feature for complete context</li>
 * </ul>
 */
@Entity
@Table(name = "plan_feature", schema = "public")
@IdClass(PlanFeature.PlanFeatureId.class)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.feature.PlanFeature")
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "planfeature-with-plan",
        attributeNodes = {
            @NamedAttributeNode("plan")
        }
    ),
    @NamedEntityGraph(
        name = "planfeature-with-feature",
        attributeNodes = {
            @NamedAttributeNode("feature")
        }
    ),
    @NamedEntityGraph(
        name = "planfeature-complete",
        attributeNodes = {
            @NamedAttributeNode("plan"),
            @NamedAttributeNode("feature")
        }
    )
})
public class PlanFeature {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private SubscriptionPlan plan;

  @Id
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "feature_key", nullable = false)
  private FeatureDefinition feature;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "configuration", columnDefinition = "jsonb")
  private Map<String, Object> configuration;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Column(name = "updated_by", length = 100)
  private String updatedBy;

  // Constructors
  public PlanFeature() {
  }

  public PlanFeature(final SubscriptionPlan plan, final FeatureDefinition feature, final boolean enabled) {
    this.plan = plan;
    this.feature = feature;
    this.enabled = enabled;
  }

  public PlanFeature(final SubscriptionPlan plan, final FeatureDefinition feature, final boolean enabled, final Map<String, Object> configuration) {
    this.plan = plan;
    this.feature = feature;
    this.enabled = enabled;
    this.configuration = configuration;
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
   * Gets the quota value for quota-based features.
   * Returns the plan-specific quota or the feature's default quota.
   */
  public Long getQuota() {
    if (!feature.isQuotaFeature()) {
      return null;
    }

    // Check plan-specific configuration first
    if (configuration != null && configuration.containsKey("quota")) {
      Object quota = configuration.get("quota");
      if (quota instanceof Number) {
        return ((Number) quota).longValue();
      }
    }

    // Fall back to feature default
    return feature.getDefaultQuota();
  }

  /**
   * Gets the limit value for limit-based features.
   * Returns the plan-specific limit or the feature's default limit.
   */
  public Long getLimit() {
    if (!feature.isLimitFeature()) {
      return null;
    }

    // Check plan-specific configuration first
    if (configuration != null && configuration.containsKey("limit")) {
      Object limit = configuration.get("limit");
      if (limit instanceof Number) {
        return ((Number) limit).longValue();
      }
    }

    // Fall back to feature default
    return feature.getDefaultLimit();
  }

  /**
   * Gets the tier value for tier-based features.
   */
  public String getTier() {
    if (feature.getType() != FeatureType.TIER || configuration == null) {
      return null;
    }

    Object tier = configuration.get("tier");
    return tier instanceof String ? (String) tier : null;
  }

  /**
   * Checks if this feature has custom configuration beyond the default.
   */
  public boolean hasCustomConfiguration() {
    return configuration != null && !configuration.isEmpty();
  }

  // Getters and setters
  public SubscriptionPlan getPlan() {
    return plan;
  }

  public void setPlan(SubscriptionPlan plan) {
    this.plan = plan;
  }

  public FeatureDefinition getFeature() {
    return feature;
  }

  public void setFeature(FeatureDefinition feature) {
    this.feature = feature;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, Object> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Map<String, Object> configuration) {
    this.configuration = configuration;
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
    PlanFeature that = (PlanFeature) o;
    return Objects.equals(plan, that.plan) && Objects.equals(feature, that.feature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(plan, feature);
  }

  @Override
  public String toString() {
    return "PlanFeature{" +
           "plan=" + (plan != null ? plan.getId() : null) +
           ", feature=" + (feature != null ? feature.getFeatureKey() : null) +
           ", enabled=" + enabled +
           '}';
  }

  /**
   * Composite primary key for PlanFeature entity.
   */
  public static class PlanFeatureId implements Serializable {
    private UUID plan;
    private String feature;

    public PlanFeatureId() {
    }

    public PlanFeatureId(final UUID plan, final String feature) {
      this.plan = plan;
      this.feature = feature;
    }

    public UUID getPlan() {
      return plan;
    }

    public void setPlan(UUID plan) {
      this.plan = plan;
    }

    public String getFeature() {
      return feature;
    }

    public void setFeature(String feature) {
      this.feature = feature;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PlanFeatureId that = (PlanFeatureId) o;
      return Objects.equals(plan, that.plan) && Objects.equals(feature, that.feature);
    }

    @Override
    public int hashCode() {
      return Objects.hash(plan, feature);
    }
  }
}
