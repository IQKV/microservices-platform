package com.iqscaffold.contactservice.feature;

import java.util.Map;
import java.util.Set;

/**
 * Feature context for contact service operations.
 * Contains feature enablement information propagated from the gateway.
 */
public record FeatureContext(
    Set<String> enabledFeatures,
    String planId,
    String planName,
    Map<String, Integer> quotas,
    Map<String, Integer> limits,
    Map<String, String> tiers
) {

  public FeatureContext {
    // Ensure immutable collections
    enabledFeatures = enabledFeatures != null ? Set.copyOf(enabledFeatures) : Set.of();
    quotas = quotas != null ? Map.copyOf(quotas) : Map.of();
    limits = limits != null ? Map.copyOf(limits) : Map.of();
    tiers = tiers != null ? Map.copyOf(tiers) : Map.of();
  }

  /**
   * Creates an empty feature context.
   */
  public static FeatureContext empty() {
    return new FeatureContext(Set.of(), null, null, Map.of(), Map.of(), Map.of());
  }

  /**
   * Checks if a specific feature is enabled.
   */
  public boolean isFeatureEnabled(String featureName) {
    return enabledFeatures.contains(featureName);
  }

  /**
   * Gets the quota value for a specific feature.
   */
  public Integer getQuota(String featureName) {
    return quotas.get(featureName);
  }

  /**
   * Gets the limit value for a specific feature.
   */
  public Integer getLimit(String featureName) {
    return limits.get(featureName);
  }

  /**
   * Gets the tier value for a specific feature.
   */
  public String getTier(String featureName) {
    return tiers.get(featureName);
  }

  /**
   * Checks if any features are enabled.
   */
  public boolean hasEnabledFeatures() {
    return !enabledFeatures.isEmpty();
  }
}