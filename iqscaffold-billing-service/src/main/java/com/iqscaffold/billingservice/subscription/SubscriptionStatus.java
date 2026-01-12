package com.iqscaffold.billingservice.subscription;

/**
 * Represents the lifecycle status of a tenant subscription.
 * <p>
 * Status Flow:
 * <ul>
 *   <li>INCOMPLETE → TRIALING/ACTIVE (Payment method added)</li>
 *   <li>TRIALING → ACTIVE (Trial ends successfully)</li>
 *   <li>ACTIVE → PAST_DUE (Payment fails)</li>
 *   <li>PAST_DUE → ACTIVE (Payment recovered) or CANCELED (Max retries exceeded)</li>
 *   <li>ACTIVE → PAUSED (Tenant pauses subscription)</li>
 *   <li>PAUSED → ACTIVE (Tenant resumes subscription)</li>
 *   <li>ANY → CANCELED (Cancellation requested)</li>
 * </ul>
 */
public enum SubscriptionStatus {
  /**
   * Subscription created but incomplete (awaiting payment method).
   */
  INCOMPLETE,

  /**
   * Subscription is in trial period.
   */
  TRIALING,

  /**
   * Subscription is active and current.
   */
  ACTIVE,

  /**
   * Payment failed, subscription past due.
   */
  PAST_DUE,

  /**
   * Subscription paused by tenant (billing suspended).
   */
  PAUSED,

  /**
   * Subscription canceled.
   */
  CANCELED,

  /**
   * Subscription unpaid (terminal state after repeated payment failures).
   */
  UNPAID
}
