package com.iqscaffold.billingservice.subscription;

/**
 * Billing interval for subscription plans.
 */
public enum SubscriptionInterval {
  /**
   * Daily billing interval.
   */
  DAY,

  /**
   * Weekly billing interval.
   */
  WEEK,

  /**
   * Monthly billing interval.
   */
  MONTH,

  /**
   * Quarterly billing interval (3 months).
   */
  QUARTER,

  /**
   * Yearly billing interval.
   */
  YEAR
}
