package com.iqscaffold.billingservice.feature;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Context object containing all feature enablement information for a tenant.
 *
 * <p>This immutable object encapsulates:
 * <ul>
 *   <li>Enabled features (boolean features)</li>
 *   <li>Feature quotas (usage-based limits)</li>
 *   <li>Feature limits (capacity constraints)</li>
 *   <li>Feature tiers (multi-level features)</li>
 * </ul>
 *
 * <p>Used by gateway filters and downstream services to make feature-based decisions.
 */
public class FeatureContext {

  private final String tenantId;
  private final String planId;
  private final String planName;
  private final Set<String> enabledFeatures;
  private final Map<String, Long> quotas;
  private final Map<String, Long> limits;
  private final Map<String, String> tiers;
  private final Map<String, Object> metadata;

  private FeatureContext(final Builder builder) {
    this.tenantId = builder.tenantId;
    this.planId = builder.planId;
    this.planName = builder.planName;
    this.enabledFeatures = Collections.unmodifiableSet(builder.enabledFeatures);
    this.quotas = Collections.unmodifiableMap(builder.quotas);
    this.limits = Collections.unmodifiableMap(builder.limits);
    this.tiers = Collections.unmodifiableMap(builder.tiers);
    this.metadata = Collections.unmodifiableMap(builder.metadata);
  }

  // Factory methods

  /**
   * Creates an empty feature context (no features enabled).
   */
  public static FeatureContext empty(final String tenantId) {
    return new Builder(tenantId).build();
  }

  /**
   * Creates a builder for constructing feature context.
   */
  public static Builder builder(String tenantId) {
    return new Builder(tenantId);
  }

  // Query methods

  /**
   * Checks if a boolean feature is enabled.
   */
  public boolean isFeatureEnabled(String featureKey) {
    return enabledFeatures.contains(featureKey);
  }

  /**
   * Gets the quota for a quota-based feature.
   * Returns null if feature is not quota-based or not configured.
   */
  public Long getQuota(String featureKey) {
    return quotas.get(featureKey);
  }

  /**
   * Gets the limit for a limit-based feature.
   * Returns null if feature is not limit-based or not configured.
   */
  public Long getLimit(String featureKey) {
    return limits.get(featureKey);
  }

  /**
   * Gets the tier for a tier-based feature.
   * Returns null if feature is not tier-based or not configured.
   */
  public String getTier(String featureKey) {
    return tiers.get(featureKey);
  }

  /**
   * Checks if any features are enabled.
   */
  public boolean hasAnyFeatures() {
    return !enabledFeatures.isEmpty() || !quotas.isEmpty() || !limits.isEmpty() || !tiers.isEmpty();
  }

  /**
   * Gets the number of enabled features.
   */
  public int getEnabledFeatureCount() {
    return enabledFeatures.size() + quotas.size() + limits.size() + tiers.size();
  }

  // Getters
  public String getTenantId() {
    return tenantId;
  }

  public String getPlanId() {
    return planId;
  }

  public String getPlanName() {
    return planName;
  }

  public Set<String> getEnabledFeatures() {
    return enabledFeatures;
  }

  public Map<String, Long> getQuotas() {
    return quotas;
  }

  public Map<String, Long> getLimits() {
    return limits;
  }

  public Map<String, String> getTiers() {
    return tiers;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public String toString() {
    return "FeatureContext{" +
           "tenantId='" + tenantId + '\'' +
           ", planId='" + planId + '\'' +
           ", planName='" + planName + '\'' +
           ", enabledFeatures=" + enabledFeatures.size() +
           ", quotas=" + quotas.size() +
           ", limits=" + limits.size() +
           ", tiers=" + tiers.size() +
           '}';
  }

  /**
   * Builder for constructing FeatureContext instances.
   */
  public static class Builder {
    private final String tenantId;
    private String planId;
    private String planName;
    private Set<String> enabledFeatures = Collections.emptySet();
    private Map<String, Long> quotas = new HashMap<>();
    private Map<String, Long> limits = new HashMap<>();
    private Map<String, String> tiers = new HashMap<>();
    private Map<String, Object> metadata = new HashMap<>();

    private Builder(final String tenantId) {
      this.tenantId = tenantId;
    }

    public Builder planId(String planId) {
      this.planId = planId;
      return this;
    }

    public Builder planName(String planName) {
      this.planName = planName;
      return this;
    }

    public Builder enabledFeatures(Set<String> enabledFeatures) {
      this.enabledFeatures = enabledFeatures != null ? enabledFeatures : Collections.emptySet();
      return this;
    }

    public Builder addQuota(String featureKey, Long quota) {
      if (quota != null) {
        this.quotas.put(featureKey, quota);
      }
      return this;
    }

    public Builder quotas(Map<String, Long> quotas) {
      if (quotas != null) {
        this.quotas.putAll(quotas);
      }
      return this;
    }

    public Builder addLimit(String featureKey, Long limit) {
      if (limit != null) {
        this.limits.put(featureKey, limit);
      }
      return this;
    }

    public Builder limits(Map<String, Long> limits) {
      if (limits != null) {
        this.limits.putAll(limits);
      }
      return this;
    }

    public Builder addTier(String featureKey, String tier) {
      if (tier != null) {
        this.tiers.put(featureKey, tier);
      }
      return this;
    }

    public Builder tiers(Map<String, String> tiers) {
      if (tiers != null) {
        this.tiers.putAll(tiers);
      }
      return this;
    }

    public Builder addMetadata(String key, Object value) {
      if (value != null) {
        this.metadata.put(key, value);
      }
      return this;
    }

    public Builder metadata(Map<String, Object> metadata) {
      if (metadata != null) {
        this.metadata.putAll(metadata);
      }
      return this;
    }

    public FeatureContext build() {
      return new FeatureContext(this);
    }
  }
}
