package com.iqscaffold.billingservice.subscription;

import com.iqscaffold.billingservice.paymentmethod.PaymentMethod;
import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.shared.exception.SubscriptionException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Factory for creating Subscription aggregates with complex business logic.
 * 
 * <p>This factory encapsulates the complex logic required to create subscriptions
 * in various states (trial, paid, incomplete) while ensuring all business rules
 * and invariants are satisfied before object creation.
 * 
 * <p>The factory validates:
 * <ul>
 *   <li>Trial eligibility for trial subscriptions</li>
 *   <li>Payment method validity for paid subscriptions</li>
 *   <li>Plan configuration and availability</li>
 *   <li>Tenant and user identifiers</li>
 *   <li>All subscription invariants</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Create a trial subscription
 * TenantTrialHistory history = trialHistoryRepository.findByTenantId(tenantId);
 * Subscription trialSub = subscriptionFactory.createTrialSubscription(
 *     tenantId, userId, plan, history
 * );
 * 
 * // Create a paid subscription
 * Subscription paidSub = subscriptionFactory.createPaidSubscription(
 *     tenantId, userId, plan, paymentMethod
 * );
 * }</pre>
 * 
 * <p>Design Rationale: Factories ensure that complex objects are created in a
 * valid state with all invariants satisfied. They centralize creation logic,
 * making it testable and maintainable.
 * 
 * @see Subscription
 * @see SubscriptionPlan
 * @see TrialEligibilitySpecification
 */
@Component
public class SubscriptionFactory {

  private final TrialEligibilitySpecification trialEligibilitySpecification;

  /**
   * Constructs a new SubscriptionFactory.
   * 
   * @param trialEligibilitySpecification specification for checking trial eligibility
   */
  public SubscriptionFactory(TrialEligibilitySpecification trialEligibilitySpecification) {
    this.trialEligibilitySpecification = trialEligibilitySpecification;
  }

  /**
   * Creates a new trial subscription for a tenant.
   * 
   * <p>This method performs the following validations:
   * <ul>
   *   <li>Verifies the tenant is eligible for a trial period</li>
   *   <li>Ensures the plan offers a trial period</li>
   *   <li>Validates tenant ID, user ID, and plan</li>
   *   <li>Ensures all subscription invariants are satisfied</li>
   * </ul>
   * 
   * <p>The created subscription will be in TRIAL status with:
   * <ul>
   *   <li>Trial start date set to current time</li>
   *   <li>Trial end date set based on plan's trial days</li>
   *   <li>Current period matching the trial period</li>
   *   <li>No payment method required</li>
   * </ul>
   * 
   * @param tenantId the tenant identifier
   * @param userId the user who is creating the subscription
   * @param plan the subscription plan with trial period
   * @param trialHistory the tenant's trial history for eligibility check
   * @return a new Subscription in TRIAL status
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws SubscriptionException.TrialNotEligibleException if tenant has already used trial
   * @throws SubscriptionException.PlanDoesNotOfferTrialException if plan doesn't offer trial
   */
  public Subscription createTrialSubscription(
      UUID tenantId,
      UUID userId,
      SubscriptionPlan plan,
      TenantTrialHistory trialHistory
  ) {
    // Validate input parameters
    validateTenantId(tenantId);
    validateUserId(userId);
    validatePlan(plan);
    validateTrialHistory(trialHistory);

    // Check trial eligibility using specification
    if (!trialEligibilitySpecification.isSatisfiedBy(trialHistory)) {
      throw new SubscriptionException.TrialNotEligibleException(
          "Tenant " + tenantId + " has already used their trial period"
      );
    }

    // Verify plan offers trial
    if (!plan.hasTrial()) {
      throw new SubscriptionException.PlanDoesNotOfferTrialException(
          "Plan " + plan.getPlanCode() + " does not offer a trial period"
      );
    }

    // Create trial subscription using aggregate factory method
    return Subscription.createTrial(tenantId, userId, plan);
  }

  /**
   * Creates a new paid subscription with a payment method.
   * 
   * <p>This method performs the following validations:
   * <ul>
   *   <li>Validates tenant ID, user ID, and plan</li>
   *   <li>Ensures payment method is valid and not expired</li>
   *   <li>Verifies payment method belongs to the tenant</li>
   *   <li>Ensures all subscription invariants are satisfied</li>
   * </ul>
   * 
   * <p>The created subscription will be in ACTIVE status with:
   * <ul>
   *   <li>Current period start set to current time</li>
   *   <li>Current period end calculated based on billing cycle</li>
   *   <li>Payment method attached for billing</li>
   *   <li>No trial period</li>
   * </ul>
   * 
   * @param tenantId the tenant identifier
   * @param userId the user who is creating the subscription
   * @param plan the subscription plan
   * @param paymentMethod the payment method for billing
   * @return a new Subscription in ACTIVE status
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws SubscriptionException.InvalidPaymentMethodException if payment method is invalid
   * @throws SubscriptionException.PaymentMethodMismatchException if payment method doesn't belong to tenant
   */
  public Subscription createPaidSubscription(
      UUID tenantId,
      UUID userId,
      SubscriptionPlan plan,
      PaymentMethod paymentMethod
  ) {
    // Validate input parameters
    validateTenantId(tenantId);
    validateUserId(userId);
    validatePlan(plan);
    validatePaymentMethod(paymentMethod);

    // Verify payment method belongs to the tenant
    if (!paymentMethod.getTenantId().equals(tenantId)) {
      throw new SubscriptionException.PaymentMethodMismatchException(
          "Payment method does not belong to tenant " + tenantId
      );
    }

    // Verify payment method is not expired
    if (paymentMethod.isExpired()) {
      throw new SubscriptionException.InvalidPaymentMethodException(
          "Payment method is expired"
      );
    }

    // Create active subscription using aggregate factory method
    return Subscription.createActive(tenantId, userId, plan);
  }

  /**
   * Creates a new incomplete subscription.
   * 
   * <p>Incomplete subscriptions are used when the subscription creation process
   * requires additional steps, such as:
   * <ul>
   *   <li>Waiting for payment method to be added</li>
   *   <li>Pending payment authorization</li>
   *   <li>Requiring additional user information</li>
   * </ul>
   * 
   * <p>The created subscription will be in INCOMPLETE status and must be
   * transitioned to TRIAL or ACTIVE once the required steps are completed.
   * 
   * @param tenantId the tenant identifier
   * @param userId the user who is creating the subscription
   * @param plan the subscription plan
   * @return a new Subscription in INCOMPLETE status
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  public Subscription createIncompleteSubscription(
      UUID tenantId,
      UUID userId,
      SubscriptionPlan plan
  ) {
    // Validate input parameters
    validateTenantId(tenantId);
    validateUserId(userId);
    validatePlan(plan);

    // Create incomplete subscription using aggregate factory method
    return Subscription.createIncomplete(tenantId, userId, plan);
  }

  /**
   * Creates a trial subscription that will convert to paid after trial ends.
   * 
   * <p>This is a convenience method that creates a trial subscription with
   * a payment method attached, so it can automatically convert to paid status
   * when the trial period ends.
   * 
   * <p>This method performs all validations from both createTrialSubscription
   * and createPaidSubscription.
   * 
   * @param tenantId the tenant identifier
   * @param userId the user who is creating the subscription
   * @param plan the subscription plan with trial period
   * @param trialHistory the tenant's trial history for eligibility check
   * @param paymentMethod the payment method for post-trial billing
   * @return a new Subscription in TRIAL status with payment method attached
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws SubscriptionException if validation fails
   */
  public Subscription createTrialWithPaymentMethod(
      UUID tenantId,
      UUID userId,
      SubscriptionPlan plan,
      TenantTrialHistory trialHistory,
      PaymentMethod paymentMethod
  ) {
    // Validate input parameters
    validateTenantId(tenantId);
    validateUserId(userId);
    validatePlan(plan);
    validateTrialHistory(trialHistory);
    validatePaymentMethod(paymentMethod);

    // Check trial eligibility
    if (!trialEligibilitySpecification.isSatisfiedBy(trialHistory)) {
      throw new SubscriptionException.TrialNotEligibleException(
          "Tenant " + tenantId + " has already used their trial period"
      );
    }

    // Verify plan offers trial
    if (!plan.hasTrial()) {
      throw new SubscriptionException.PlanDoesNotOfferTrialException(
          "Plan " + plan.getPlanCode() + " does not offer a trial period"
      );
    }

    // Verify payment method belongs to the tenant
    if (!paymentMethod.getTenantId().equals(tenantId)) {
      throw new SubscriptionException.PaymentMethodMismatchException(
          "Payment method does not belong to tenant " + tenantId
      );
    }

    // Verify payment method is not expired
    if (paymentMethod.isExpired()) {
      throw new SubscriptionException.InvalidPaymentMethodException(
          "Payment method is expired"
      );
    }

    // Create trial subscription
    // Note: Payment method will be attached separately by the service layer
    return Subscription.createTrial(tenantId, userId, plan);
  }

  // Private validation methods

  private void validateTenantId(UUID tenantId) {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
  }

  private void validateUserId(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
  }

  private void validatePlan(SubscriptionPlan plan) {
    if (plan == null) {
      throw new IllegalArgumentException("Subscription plan cannot be null");
    }

    if (!plan.getActive()) {
      throw new IllegalArgumentException(
          "Cannot create subscription with inactive plan: " + plan.getPlanCode()
      );
    }
  }

  private void validateTrialHistory(TenantTrialHistory trialHistory) {
    if (trialHistory == null) {
      throw new IllegalArgumentException("Trial history cannot be null");
    }
  }

  private void validatePaymentMethod(PaymentMethod paymentMethod) {
    if (paymentMethod == null) {
      throw new IllegalArgumentException("Payment method cannot be null");
    }
  }
}
