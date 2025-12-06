package com.iqscaffold.billingservice.subscription;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Domain service for managing subscription lifecycle state transitions.
 * 
 * <p>This service encapsulates the complex business logic for transitioning subscriptions
 * between different states while enforcing business rules and maintaining audit trails.
 * It ensures that all state transitions are valid and properly documented.
 * 
 * <p>Subscription lifecycle includes:
 * <ul>
 *   <li>Trial period management (start, extend, convert, expire)</li>
 *   <li>Activation and deactivation</li>
 *   <li>Cancellation (immediate or at period end)</li>
 *   <li>Reactivation of canceled subscriptions</li>
 *   <li>Suspension and unsuspension (admin actions)</li>
 *   <li>Expiration handling</li>
 *   <li>Past due management (payment failures)</li>
 * </ul>
 * 
 * <p>This logic involves complex state machine validation and doesn't naturally belong
 * to the Subscription aggregate alone, as it may involve external factors (payment status,
 * admin actions, scheduled jobs). Therefore, it's implemented as a stateless domain service.
 * 
 * <p>Usage example:
 * <pre>{@code
 * subscriptionLifecycleManager.transitionStatus(
 *   subscription,
 *   SubscriptionStatus.ACTIVE,
 *   "Trial period ended, payment method on file"
 * );
 * 
 * // Or use convenience methods
 * subscriptionLifecycleManager.convertTrialToActive(subscription);
 * subscriptionLifecycleManager.cancelAtPeriodEnd(subscription, "Customer request");
 * }</pre>
 * 
 * <p>Design Rationale: Domain services keep business logic in the domain layer while
 * avoiding artificial assignment to aggregates. They remain stateless and focused on
 * domain operations.
 * 
 * @see Subscription
 * @see SubscriptionStatus
 */
@Service
public class SubscriptionLifecycleManager {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionLifecycleManager.class);

  /**
   * Transitions a subscription to a new status with validation and audit logging.
   * 
   * <p>This method:
   * <ul>
   *   <li>Validates the status transition is allowed</li>
   *   <li>Delegates to the subscription aggregate for the actual transition</li>
   *   <li>Logs the transition for audit purposes</li>
   *   <li>Provides a reason for the transition</li>
   * </ul>
   * 
   * <p>The subscription aggregate enforces the state machine rules, and this
   * service provides the orchestration and audit trail.
   * 
   * @param subscription the subscription to transition
   * @param newStatus the target status
   * @param reason the reason for the transition
   * @throws IllegalArgumentException if any parameter is null
   * @throws IllegalStateException if the transition is not allowed
   */
  public void transitionStatus(
      final Subscription subscription,
      final SubscriptionStatus newStatus,
      final String reason) {

    validateSubscription(subscription);
    validateNewStatus(newStatus);
    validateReason(reason);

    var oldStatus = subscription.getStatus();

    // Log the transition attempt
    logger.info(
        "Attempting subscription status transition: subscriptionId={}, tenantId={}, " +
        "oldStatus={}, newStatus={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        oldStatus,
        newStatus,
        reason
    );

    // Delegate to the appropriate method based on target status
    try {
      switch (newStatus) {
        case ACTIVE -> activateSubscription(subscription, reason);
        case TRIAL -> throw new IllegalStateException("Cannot transition to TRIAL status directly");
        case PAST_DUE -> markPastDue(subscription, reason);
        case CANCELED -> cancelSubscription(subscription, reason, true);
        case SUSPENDED -> suspendSubscription(subscription, reason);
        case EXPIRED -> expireSubscription(subscription, reason);
        case INCOMPLETE -> throw new IllegalStateException("Cannot transition to INCOMPLETE status");
      }

      // Log successful transition
      logger.info(
          "Subscription status transition successful: subscriptionId={}, tenantId={}, " +
          "oldStatus={}, newStatus={}",
          subscription.getId(),
          subscription.getTenantId(),
          oldStatus,
          newStatus
      );

    } catch (IllegalStateException e) {
      logger.error(
          "Subscription status transition failed: subscriptionId={}, tenantId={}, " +
          "oldStatus={}, newStatus={}, error={}",
          subscription.getId(),
          subscription.getTenantId(),
          oldStatus,
          newStatus,
          e.getMessage()
      );
      throw e;
    }
  }

  /**
   * Converts a trial subscription to active status.
   * 
   * <p>This is typically called when:
   * <ul>
   *   <li>Trial period ends and payment method is on file</li>
   *   <li>Customer manually converts trial to paid</li>
   * </ul>
   * 
   * @param subscription the trial subscription to convert
   * @throws IllegalStateException if subscription is not in TRIAL status
   */
  public void convertTrialToActive(final Subscription subscription) {
    validateSubscription(subscription);

    if (subscription.getStatus() != SubscriptionStatus.TRIAL) {
      throw new IllegalStateException("Can only convert TRIAL subscriptions to ACTIVE");
    }

    logger.info(
        "Converting trial subscription to active: subscriptionId={}, tenantId={}",
        subscription.getId(),
        subscription.getTenantId()
    );

    subscription.convertTrialToActive();
  }

  /**
   * Extends the trial period for a subscription.
   * 
   * <p>This is typically an admin action to give customers more time to evaluate.
   * 
   * @param subscription the trial subscription to extend
   * @param additionalDays number of days to extend
   * @param reason the reason for the extension
   * @throws IllegalStateException if subscription is not in TRIAL status
   * @throws IllegalArgumentException if additionalDays is negative
   */
  public void extendTrial(
      final Subscription subscription,
      final int additionalDays,
      final String reason) {

    validateSubscription(subscription);
    validateReason(reason);

    if (subscription.getStatus() != SubscriptionStatus.TRIAL) {
      throw new IllegalStateException("Can only extend TRIAL subscriptions");
    }

    if (additionalDays <= 0) {
      throw new IllegalArgumentException("Additional days must be positive");
    }

    logger.info(
        "Extending trial subscription: subscriptionId={}, tenantId={}, additionalDays={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        additionalDays,
        reason
    );

    subscription.extendTrial(additionalDays);
  }

  /**
   * Cancels a subscription at the end of the current billing period.
   * 
   * <p>The subscription remains active until the period end, allowing the customer
   * to continue using the service for the time they've paid for.
   * 
   * @param subscription the subscription to cancel
   * @param reason the reason for cancellation
   */
  public void cancelAtPeriodEnd(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    logger.info(
        "Canceling subscription at period end: subscriptionId={}, tenantId={}, " +
        "periodEnd={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getCurrentPeriodEnd(),
        reason
    );

    subscription.cancelAtPeriodEnd();
  }

  /**
   * Cancels a subscription immediately.
   * 
   * <p>The subscription is immediately expired, and access is revoked.
   * This is typically used for:
   * <ul>
   *   <li>Policy violations</li>
   *   <li>Fraud detection</li>
   *   <li>Customer request for immediate cancellation</li>
   * </ul>
   * 
   * @param subscription the subscription to cancel
   * @param reason the reason for immediate cancellation
   */
  public void cancelImmediately(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    logger.warn(
        "Canceling subscription immediately: subscriptionId={}, tenantId={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        reason
    );

    subscription.cancelImmediately();
  }

  /**
   * Reactivates a canceled subscription before the period end.
   * 
   * <p>This allows customers to undo a cancellation if they change their mind
   * before the subscription actually expires.
   * 
   * @param subscription the canceled subscription to reactivate
   * @param reason the reason for reactivation
   * @throws IllegalStateException if subscription cannot be reactivated
   */
  public void reactivate(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    if (!subscription.getStatus().canReactivate()) {
      throw new IllegalStateException(
          "Cannot reactivate subscription in " + subscription.getStatus() + " status"
      );
    }

    logger.info(
        "Reactivating subscription: subscriptionId={}, tenantId={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        reason
    );

    subscription.reactivate();
  }

  /**
   * Marks a subscription as past due when payment fails.
   * 
   * <p>This triggers the dunning process (automated payment retries).
   * The subscription remains accessible during the grace period.
   * 
   * @param subscription the subscription with failed payment
   * @param reason the reason for marking past due (e.g., payment failure details)
   */
  public void markPastDue(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    logger.warn(
        "Marking subscription as past due: subscriptionId={}, tenantId={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        reason
    );

    subscription.markPastDue();
  }

  /**
   * Reactivates a past due subscription after successful payment.
   * 
   * <p>This is called when a payment retry succeeds or the customer manually
   * updates their payment method and pays.
   * 
   * @param subscription the past due subscription
   * @param reason the reason for reactivation (e.g., payment success details)
   */
  public void reactivateFromPastDue(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    if (subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
      throw new IllegalStateException("Can only reactivate PAST_DUE subscriptions");
    }

    logger.info(
        "Reactivating subscription from past due: subscriptionId={}, tenantId={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        reason
    );

    subscription.reactivateFromPastDue();
  }

  /**
   * Suspends a subscription (admin action).
   * 
   * <p>This is typically used for:
   * <ul>
   *   <li>Policy violations</li>
   *   <li>Fraud investigation</li>
   *   <li>Legal holds</li>
   * </ul>
   * 
   * @param subscription the subscription to suspend
   * @param reason the reason for suspension
   */
  public void suspendSubscription(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    logger.warn(
        "Suspending subscription: subscriptionId={}, tenantId={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        reason
    );

    subscription.suspend();
  }

  /**
   * Unsuspends a subscription (admin action).
   * 
   * <p>This restores access after a suspension is lifted.
   * 
   * @param subscription the suspended subscription
   * @param reason the reason for unsuspension
   * @throws IllegalStateException if subscription is not suspended
   */
  public void unsuspendSubscription(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    if (subscription.getStatus() != SubscriptionStatus.SUSPENDED) {
      throw new IllegalStateException("Can only unsuspend SUSPENDED subscriptions");
    }

    logger.info(
        "Unsuspending subscription: subscriptionId={}, tenantId={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        reason
    );

    subscription.unsuspend();
  }

  /**
   * Expires a subscription.
   * 
   * <p>This is typically called when:
   * <ul>
   *   <li>Billing period ends and no payment is received</li>
   *   <li>Trial period ends without conversion</li>
   *   <li>Canceled subscription reaches period end</li>
   *   <li>Payment retries are exhausted</li>
   * </ul>
   * 
   * @param subscription the subscription to expire
   * @param reason the reason for expiration
   */
  public void expireSubscription(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    logger.info(
        "Expiring subscription: subscriptionId={}, tenantId={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        reason
    );

    subscription.expire();
  }

  /**
   * Renews a subscription for the next billing period.
   * 
   * <p>This is typically called by a scheduled job after successful payment
   * for the next period.
   * 
   * @param subscription the subscription to renew
   * @param reason the reason for renewal (e.g., payment success)
   * @throws IllegalStateException if subscription is not in ACTIVE status
   */
  public void renewSubscription(final Subscription subscription, final String reason) {
    validateSubscription(subscription);
    validateReason(reason);

    if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException("Can only renew ACTIVE subscriptions");
    }

    logger.info(
        "Renewing subscription: subscriptionId={}, tenantId={}, newPeriodEnd={}, reason={}",
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getCurrentPeriodEnd(),
        reason
    );

    subscription.renew();
  }

  // Private helper methods

  private void activateSubscription(Subscription subscription, String reason) {
    var currentStatus = subscription.getStatus();

    switch (currentStatus) {
      case TRIAL -> subscription.convertTrialToActive();
      case PAST_DUE -> subscription.reactivateFromPastDue();
      case CANCELED -> subscription.reactivate();
      case SUSPENDED -> subscription.unsuspend();
      case INCOMPLETE -> throw new IllegalStateException(
          "Cannot activate INCOMPLETE subscription without completing setup"
      );
      case ACTIVE -> throw new IllegalStateException("Subscription is already ACTIVE");
      case EXPIRED -> throw new IllegalStateException("Cannot activate EXPIRED subscription");
    }
  }

  private void cancelSubscription(Subscription subscription, String reason, boolean atPeriodEnd) {
    if (atPeriodEnd) {
      subscription.cancelAtPeriodEnd();
    } else {
      subscription.cancelImmediately();
    }
  }

  private void validateSubscription(Subscription subscription) {
    if (subscription == null) {
      throw new IllegalArgumentException("Subscription cannot be null");
    }
  }

  private void validateNewStatus(SubscriptionStatus newStatus) {
    if (newStatus == null) {
      throw new IllegalArgumentException("New status cannot be null");
    }
  }

  private void validateReason(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Reason cannot be null or blank");
    }
  }
}
