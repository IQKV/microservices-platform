package com.iqscaffold.billingservice.plan;

/**
 * Billing cycle frequencies for subscription plans.
 * Determines how often customers are charged for their subscription.
 */
public enum BillingCycle {
  /**
   * Monthly billing cycle - charged every month.
   */
  MONTHLY,

  /**
   * Yearly billing cycle - charged annually.
   */
  YEARLY,

  /**
   * Lifetime billing cycle - one-time payment for lifetime access.
   */
  LIFETIME
}
