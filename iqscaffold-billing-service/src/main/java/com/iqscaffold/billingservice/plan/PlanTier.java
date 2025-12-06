package com.iqscaffold.billingservice.plan;

/**
 * Subscription plan tiers representing different service levels.
 * Used to categorize subscription plans by feature set and pricing.
 */
public enum PlanTier {
  /**
   * Free tier with basic features and limited quotas.
   */
  FREE,

  /**
   * Professional tier with advanced features and higher quotas.
   */
  PRO,

  /**
   * Enterprise tier with all features, custom quotas, and dedicated support.
   */
  ENTERPRISE
}
