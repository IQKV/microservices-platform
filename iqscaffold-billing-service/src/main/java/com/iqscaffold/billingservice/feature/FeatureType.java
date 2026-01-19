package com.iqscaffold.billingservice.feature;

/**
 * Enumeration of feature types supported by the platform.
 * 
 * <p>Different feature types enable different validation and configuration patterns:
 * <ul>
 *   <li>BOOLEAN - Simple on/off features (e.g., "advanced_analytics")</li>
 *   <li>QUOTA - Features with usage limits (e.g., "api_calls_per_month": 10000)</li>
 *   <li>LIMIT - Features with capacity constraints (e.g., "max_team_members": 50)</li>
 *   <li>TIER - Features with multiple levels (e.g., "support_level": "premium")</li>
 * </ul>
 */
public enum FeatureType {
  
  /**
   * Simple boolean feature - either enabled or disabled.
   * No additional configuration required.
   */
  BOOLEAN,
  
  /**
   * Quota-based feature with usage tracking.
   * Requires quota configuration in metadata (e.g., monthly limits).
   */
  QUOTA,
  
  /**
   * Limit-based feature with capacity constraints.
   * Requires limit configuration in metadata (e.g., max users, storage).
   */
  LIMIT,
  
  /**
   * Tiered feature with multiple levels.
   * Requires tier configuration in metadata (e.g., basic, premium, enterprise).
   */
  TIER
}