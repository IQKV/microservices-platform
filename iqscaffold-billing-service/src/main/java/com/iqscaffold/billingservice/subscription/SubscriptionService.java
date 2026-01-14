package com.iqscaffold.billingservice.subscription;

import java.util.UUID;

import com.iqscaffold.billingservice.subscription.dto.SubscriptionDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing tenant subscription lifecycle operations.
 * <p>
 * This service orchestrates subscription management including:
 * <ul>
 *   <li>Creating new subscriptions with Stripe integration</li>
 *   <li>Managing subscription state transitions (pause, resume, cancel)</li>
 *   <li>Subscription updates and modifications</li>
 *   <li>Synchronization between local state and Stripe</li>
 * </ul>
 *
 * <h4>Key Features:</h4>
 * <ul>
 *   <li><strong>State Machine Validation</strong> - Enforces valid subscription state transitions</li>
 *   <li><strong>Stripe Integration</strong> - Manages Stripe subscription lifecycle</li>
 *   <li><strong>Tenant Isolation</strong> - Properly scopes subscriptions to tenants</li>
 *   <li><strong>Audit Trail</strong> - Tracks all subscription state changes</li>
 * </ul>
 */
public interface SubscriptionService {

  /**
   * Creates a new subscription for the current tenant.
   * <p>
   * Steps:
   * <ol>
   *   <li>Validates subscription plan exists and is active</li>
   *   <li>Checks for existing active subscription (if business rules disallow duplicates)</li>
   *   <li>Creates Stripe customer if needed</li>
   *   <li>Creates Stripe subscription with trial period if applicable</li>
   *   <li>Persists local subscription record</li>
   *   <li>Creates audit trail entry</li>
   * </ol>
   *
   * @param request The subscription creation request
   * @return The created subscription response
   * @throws SubscriptionNotFoundException     If the specified plan does not exist
   * @throws InvalidSubscriptionStateException If subscription state transition is invalid
   */
  SubscriptionDtos.SubscriptionResponse createSubscription(SubscriptionDtos.CreateSubscriptionRequest request);

  /**
   * Retrieves a subscription by its unique identifier for the current tenant.
   *
   * @param id The subscription UUID
   * @return The subscription response
   * @throws SubscriptionNotFoundException If subscription not found
   */
  SubscriptionDtos.SubscriptionResponse getSubscription(UUID id);

  /**
   * Retrieves the active subscription for the current tenant.
   *
   * @return The active subscription response, or empty if no active subscription
   */
  java.util.Optional<SubscriptionDtos.SubscriptionResponse> getActiveSubscription();

  /**
   * Retrieves all subscriptions for the current tenant with pagination.
   *
   * @param pageable Pagination parameters
   * @return Page of subscription responses
   */
  Page<SubscriptionDtos.SubscriptionResponse> getSubscriptions(Pageable pageable);

  /**
   * Updates an existing subscription (e.g., change plan, update payment method).
   *
   * @param id      The subscription UUID
   * @param request The update request
   * @return The updated subscription response
   * @throws SubscriptionNotFoundException     If subscription not found
   * @throws InvalidSubscriptionStateException If update is not allowed in current state
   */
  SubscriptionDtos.SubscriptionResponse updateSubscription(UUID id,
                                                           SubscriptionDtos.UpdateSubscriptionRequest request);

  /**
   * Cancels a subscription at the end of the current billing period.
   *
   * @param id The subscription UUID
   * @return The updated subscription response showing canceled status
   * @throws SubscriptionNotFoundException     If subscription not found
   * @throws InvalidSubscriptionStateException If cancellation is not allowed in current state
   */
  SubscriptionDtos.SubscriptionResponse cancelSubscription(UUID id);

  /**
   * Cancels a subscription immediately without waiting for the billing period to end.
   *
   * @param id The subscription UUID
   * @return The updated subscription response showing canceled status
   * @throws SubscriptionNotFoundException     If subscription not found
   * @throws InvalidSubscriptionStateException If cancellation is not allowed in current state
   */
  SubscriptionDtos.SubscriptionResponse cancelSubscriptionImmediately(UUID id);

  /**
   * Pauses a subscription, preventing billing while preserving the subscription.
   *
   * @param id The subscription UUID
   * @return The updated subscription response showing paused status
   * @throws SubscriptionNotFoundException     If subscription not found
   * @throws InvalidSubscriptionStateException If pause is not allowed in current state
   */
  SubscriptionDtos.SubscriptionResponse pauseSubscription(UUID id);

  /**
   * Resumes a paused subscription.
   *
   * @param id The subscription UUID
   * @return The updated subscription response showing active status
   * @throws SubscriptionNotFoundException     If subscription not found
   * @throws InvalidSubscriptionStateException If resume is not allowed in current state
   */
  SubscriptionDtos.SubscriptionResponse resumeSubscription(UUID id);

  /**
   * Synchronizes a subscription with its current state in Stripe.
   * <p>
   * This is useful for reconciliation and ensuring local state matches Stripe.
   *
   * @param stripeSubscriptionId The Stripe subscription ID
   * @throws SubscriptionNotFoundException If subscription not found locally
   */
  void syncSubscriptionFromStripe(String stripeSubscriptionId);
}
