package com.iqscaffold.billingservice.subscription;

/**
 * Subscription lifecycle status values.
 * Represents the current state of a subscription in its lifecycle.
 */
public enum SubscriptionStatus {
  /**
   * Subscription is in trial period with full feature access.
   */
  TRIAL,

  /**
   * Subscription is active and paid, with full feature access.
   */
  ACTIVE,

  /**
   * Subscription payment is overdue but access is maintained during grace period.
   */
  PAST_DUE,

  /**
   * Subscription has been canceled but access continues until period end.
   */
  CANCELED,

  /**
   * Subscription has expired and access is revoked.
   */
  EXPIRED,

  /**
   * Subscription is suspended by administrator.
   */
  SUSPENDED,

  /**
   * Subscription creation is incomplete (awaiting payment method or confirmation).
   */
  INCOMPLETE;

  /**
   * Checks if the subscription status allows full feature access.
   *
   * @return true if status allows access (TRIAL, ACTIVE, PAST_DUE, CANCELED)
   */
  public boolean allowsAccess() {
    return switch (this) {
      case TRIAL, ACTIVE, PAST_DUE, CANCELED -> true;
      case EXPIRED, SUSPENDED, INCOMPLETE -> false;
    };
  }

  /**
   * Checks if the subscription is in an active billing state.
   *
   * @return true if status requires billing (ACTIVE, PAST_DUE)
   */
  public boolean isActiveBilling() {
    return switch (this) {
      case ACTIVE, PAST_DUE -> true;
      case TRIAL, CANCELED, EXPIRED, SUSPENDED, INCOMPLETE -> false;
    };
  }

  /**
   * Checks if the subscription can be upgraded or downgraded.
   *
   * @return true if plan changes are allowed
   */
  public boolean canChangePlan() {
    return switch (this) {
      case TRIAL, ACTIVE, PAST_DUE -> true;
      case CANCELED, EXPIRED, SUSPENDED, INCOMPLETE -> false;
    };
  }

  /**
   * Checks if the subscription can be canceled.
   *
   * @return true if cancellation is allowed
   */
  public boolean canCancel() {
    return switch (this) {
      case TRIAL, ACTIVE, PAST_DUE -> true;
      case CANCELED, EXPIRED, SUSPENDED, INCOMPLETE -> false;
    };
  }

  /**
   * Checks if the subscription can be reactivated.
   *
   * @return true if reactivation is allowed
   */
  public boolean canReactivate() {
    return this == CANCELED;
  }
}
