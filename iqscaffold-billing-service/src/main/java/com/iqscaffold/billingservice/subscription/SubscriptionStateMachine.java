package com.iqscaffold.billingservice.subscription;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * State machine for validating subscription status transitions.
 * <p>
 * Ensures that subscriptions can only move between valid states,
 * preventing invalid state changes that could compromise billing integrity.
 * <p>
 * Valid transitions:
 * <ul>
 *   <li>INCOMPLETE → TRIALING (payment method added with trial)</li>
 *   <li>INCOMPLETE → ACTIVE (payment method added without trial)</li>
 *   <li>TRIALING → ACTIVE (trial period ends successfully)</li>
 *   <li>TRIALING → PAST_DUE (trial ends, payment fails)</li>
 *   <li>TRIALING → CANCELED (canceled during trial)</li>
 *   <li>ACTIVE → PAST_DUE (payment failure)</li>
 *   <li>ACTIVE → PAUSED (tenant pauses subscription)</li>
 *   <li>ACTIVE → CANCELED (subscription canceled)</li>
 *   <li>PAST_DUE → ACTIVE (payment recovered)</li>
 *   <li>PAST_DUE → CANCELED (max retries exceeded)</li>
 *   <li>PAST_DUE → UNPAID (marked as uncollectible)</li>
 *   <li>PAUSED → ACTIVE (subscription resumed)</li>
 *   <li>PAUSED → CANCELED (canceled while paused)</li>
 * </ul>
 */
@Component
public class SubscriptionStateMachine {

  /**
   * Map of allowed state transitions.
   * Key: Current status, Value: Set of allowed next statuses
   */
  private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> ALLOWED_TRANSITIONS = Map.of(
      SubscriptionStatus.INCOMPLETE, EnumSet.of(
          SubscriptionStatus.TRIALING,
          SubscriptionStatus.ACTIVE,
          SubscriptionStatus.CANCELED
      ),
      SubscriptionStatus.TRIALING, EnumSet.of(
          SubscriptionStatus.ACTIVE,
          SubscriptionStatus.PAST_DUE,
          SubscriptionStatus.CANCELED
      ),
      SubscriptionStatus.ACTIVE, EnumSet.of(
          SubscriptionStatus.PAST_DUE,
          SubscriptionStatus.PAUSED,
          SubscriptionStatus.CANCELED
      ),
      SubscriptionStatus.PAST_DUE, EnumSet.of(
          SubscriptionStatus.ACTIVE,
          SubscriptionStatus.CANCELED,
          SubscriptionStatus.UNPAID
      ),
      SubscriptionStatus.PAUSED, EnumSet.of(
          SubscriptionStatus.ACTIVE,
          SubscriptionStatus.CANCELED
      ),
      SubscriptionStatus.CANCELED, EnumSet.noneOf(SubscriptionStatus.class),
      SubscriptionStatus.UNPAID, EnumSet.noneOf(SubscriptionStatus.class)
  );

  /**
   * Validates whether a transition from one status to another is allowed.
   *
   * @param currentStatus The current subscription status (can be null for new subscriptions)
   * @param newStatus     The desired new status
   * @return true if the transition is valid, false otherwise
   */
  public boolean isTransitionAllowed(SubscriptionStatus currentStatus, SubscriptionStatus newStatus) {
    if (newStatus == null) {
      return false;
    }

    // Allow initial creation (null -> INCOMPLETE or null -> TRIALING or null -> ACTIVE)
    if (currentStatus == null) {
      return newStatus == SubscriptionStatus.INCOMPLETE
             || newStatus == SubscriptionStatus.TRIALING
             || newStatus == SubscriptionStatus.ACTIVE;
    }

    // No transition if statuses are the same
    if (currentStatus == newStatus) {
      return true;
    }

    // Check if transition is in the allowed transitions map
    Set<SubscriptionStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
    return allowedNextStatuses != null && allowedNextStatuses.contains(newStatus);
  }

  /**
   * Validates a state transition and throws an exception if invalid.
   *
   * @param currentStatus The current subscription status
   * @param newStatus     The desired new status
   * @throws InvalidSubscriptionStateException if the transition is not allowed
   */
  public void validateTransition(SubscriptionStatus currentStatus, SubscriptionStatus newStatus) {
    if (!isTransitionAllowed(currentStatus, newStatus)) {
      throw new InvalidSubscriptionStateException(
          String.format("Invalid subscription state transition from %s to %s",
              currentStatus, newStatus)
      );
    }
  }

  /**
   * Gets the set of allowed next states for a given current state.
   *
   * @param currentStatus The current subscription status
   * @return Set of allowed next statuses
   */
  public Set<SubscriptionStatus> getAllowedNextStates(SubscriptionStatus currentStatus) {
    if (currentStatus == null) {
      return EnumSet.of(SubscriptionStatus.INCOMPLETE, SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE);
    }
    return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(SubscriptionStatus.class));
  }

  /**
   * Checks if a subscription status is terminal (no further transitions allowed).
   *
   * @param status The subscription status to check
   * @return true if the status is terminal
   */
  public boolean isTerminalState(SubscriptionStatus status) {
    return status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.UNPAID;
  }

  /**
   * Checks if a subscription status represents an active billing state.
   *
   * @param status The subscription status to check
   * @return true if the subscription should be billed
   */
  public boolean isActiveBillingState(SubscriptionStatus status) {
    return status == SubscriptionStatus.ACTIVE
           || status == SubscriptionStatus.TRIALING
           || status == SubscriptionStatus.PAST_DUE;
  }
}
