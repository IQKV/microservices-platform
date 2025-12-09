package com.iqscaffold.billingservice.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import com.iqscaffold.billingservice.subscription.Subscription;
import org.springframework.stereotype.Service;

/**
 * Domain service for calculating proration when subscription plans change mid-period.
 *
 * <p>This service encapsulates the complex business logic for calculating credits
 * and charges when a customer upgrades or downgrades their subscription plan before
 * the current billing period ends. The proration ensures fair billing by:
 * <ul>
 *   <li>Crediting unused time on the old plan</li>
 *   <li>Charging for the new plan for the remaining period</li>
 *   <li>Calculating the net amount owed or credited</li>
 * </ul>
 *
 * <p>This logic doesn't naturally belong to either the Subscription or SubscriptionPlan
 * aggregate, as it spans both and involves time-based calculations. Therefore, it's
 * implemented as a stateless domain service.
 *
 * <p>The proration calculation follows this formula:
 * <pre>
 * prorationFactor = daysRemaining / daysInPeriod
 * creditAmount = oldPlanPrice * prorationFactor
 * chargeAmount = newPlanPrice * prorationFactor
 * netAmount = chargeAmount - creditAmount
 * </pre>
 *
 * <p>Usage example:
 * <pre>{@code
 * ProrationResult result = prorationCalculator.calculate(
 *   subscription,
 *   newPlan,
 *   LocalDateTime.now()
 * );
 *
 * if (result.isUpgrade()) {
 *   // Customer owes additional amount
 *   paymentService.charge(result.netAmount());
 * } else if (result.isDowngrade()) {
 *   // Customer receives credit
 *   creditService.apply(result.getAbsoluteNetAmount());
 * }
 * }</pre>
 *
 * <p>Design Rationale: Domain services keep business logic in the domain layer while
 * avoiding artificial assignment to aggregates. They remain stateless and focused on
 * domain operations.
 *
 * @see ProrationResult
 * @see Subscription
 * @see SubscriptionPlan
 */
@Service
public class ProrationCalculator {

  /**
   * Calculates proration for a subscription plan change.
   *
   * <p>This method determines the credit for unused time on the old plan and the
   * charge for the new plan for the remaining period. It handles both upgrades
   * (net positive amount) and downgrades (net negative amount).
   *
   * <p>The calculation considers:
   * <ul>
   *   <li>Current subscription plan and pricing</li>
   *   <li>New plan and pricing</li>
   *   <li>Days remaining in the current billing period</li>
   *   <li>Total days in the billing period</li>
   * </ul>
   *
   * @param subscription  the current subscription
   * @param newPlan       the new subscription plan
   * @param effectiveDate the date when the plan change takes effect
   * @return a ProrationResult containing credit, charge, and net amounts
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws IllegalStateException    if subscription is not in a state that allows plan changes
   */
  public ProrationResult calculate(
      final Subscription subscription,
      final SubscriptionPlan newPlan,
      final LocalDateTime effectiveDate) {

    validateSubscription(subscription);
    validateNewPlan(newPlan);
    validateEffectiveDate(effectiveDate);

    // Get current plan
    var currentPlan = subscription.getPlan();

    // Validate that plan change is meaningful
    if (currentPlan.getId().equals(newPlan.getId())) {
      throw new IllegalArgumentException("New plan must be different from current plan");
    }

    // Validate subscription allows plan changes
    if (!subscription.getStatus().canChangePlan()) {
      throw new IllegalStateException(
          "Cannot change plan for subscription in " + subscription.getStatus() + " status"
      );
    }

    // Calculate days in period and days remaining
    var periodStart = subscription.getCurrentPeriodStart();
    var periodEnd = subscription.getCurrentPeriodEnd();

    if (periodStart == null || periodEnd == null) {
      throw new IllegalStateException("Subscription must have valid period start and end dates");
    }

    var daysInPeriod = (int) ChronoUnit.DAYS.between(periodStart, periodEnd);
    var daysRemaining = (int) ChronoUnit.DAYS.between(effectiveDate, periodEnd);

    // Ensure effective date is within the current period
    if (effectiveDate.isBefore(periodStart)) {
      throw new IllegalArgumentException("Effective date cannot be before period start");
    }

    if (effectiveDate.isAfter(periodEnd)) {
      throw new IllegalArgumentException("Effective date cannot be after period end");
    }

    // Handle edge case: effective date is at period end (no proration needed)
    if (daysRemaining == 0) {
      return ProrationResult.noProration(newPlan.getBasePrice(), daysInPeriod, newPlan.getName());
    }

    // Get plan prices
    var oldPlanPrice = currentPlan.getBasePrice();
    var newPlanPrice = newPlan.getBasePrice();

    // Determine if upgrade or downgrade
    var isUpgrade = newPlanPrice.compareTo(oldPlanPrice) > 0;

    // Calculate proration
    if (isUpgrade) {
      return ProrationResult.forUpgrade(
          oldPlanPrice,
          newPlanPrice,
          daysRemaining,
          daysInPeriod,
          currentPlan.getName(),
          newPlan.getName()
      );
    } else {
      return ProrationResult.forDowngrade(
          oldPlanPrice,
          newPlanPrice,
          daysRemaining,
          daysInPeriod,
          currentPlan.getName(),
          newPlan.getName()
      );
    }
  }

  /**
   * Calculates proration for a subscription upgrade with immediate effect.
   *
   * <p>Convenience method that uses the current time as the effective date.
   *
   * @param subscription the current subscription
   * @param newPlan      the new subscription plan (higher tier)
   * @return a ProrationResult for the upgrade
   */
  public ProrationResult calculateUpgrade(
      final Subscription subscription,
      final SubscriptionPlan newPlan) {
    return calculate(subscription, newPlan, LocalDateTime.now());
  }

  /**
   * Calculates proration for a subscription downgrade with immediate effect.
   *
   * <p>Convenience method that uses the current time as the effective date.
   *
   * @param subscription the current subscription
   * @param newPlan      the new subscription plan (lower tier)
   * @return a ProrationResult for the downgrade
   */
  public ProrationResult calculateDowngrade(
      final Subscription subscription,
      final SubscriptionPlan newPlan) {
    return calculate(subscription, newPlan, LocalDateTime.now());
  }

  /**
   * Calculates proration for a scheduled plan change at period end.
   *
   * <p>When a plan change is scheduled for the end of the current period,
   * no proration is needed. This method returns a result indicating the
   * full charge for the new plan starting at the next period.
   *
   * @param subscription the current subscription
   * @param newPlan      the new subscription plan
   * @return a ProrationResult with no proration (full period charge)
   */
  public ProrationResult calculateScheduledChange(
      final Subscription subscription,
      final SubscriptionPlan newPlan) {

    validateSubscription(subscription);
    validateNewPlan(newPlan);

    var periodEnd = subscription.getCurrentPeriodEnd();
    if (periodEnd == null) {
      throw new IllegalStateException("Subscription must have valid period end date");
    }

    // Calculate days in the next period based on new plan's billing cycle
    var nextPeriodStart = periodEnd;
    var nextPeriodEnd = calculateNextPeriodEnd(nextPeriodStart, newPlan);
    var daysInNextPeriod = (int) ChronoUnit.DAYS.between(nextPeriodStart, nextPeriodEnd);

    return ProrationResult.noProration(
        newPlan.getBasePrice(),
        daysInNextPeriod,
        newPlan.getName()
    );
  }

  /**
   * Estimates the annual cost difference between two plans.
   *
   * <p>Useful for showing customers the annual savings or additional cost
   * when considering a plan change.
   *
   * @param currentPlan the current subscription plan
   * @param newPlan     the new subscription plan
   * @return the annual cost difference (positive for increase, negative for decrease)
   */
  public BigDecimal estimateAnnualCostDifference(
      final SubscriptionPlan currentPlan,
      final SubscriptionPlan newPlan) {

    validatePlan(currentPlan, "Current plan");
    validatePlan(newPlan, "New plan");

    var currentAnnualCost = calculateAnnualCost(currentPlan);
    var newAnnualCost = calculateAnnualCost(newPlan);

    return newAnnualCost.subtract(currentAnnualCost).setScale(2, RoundingMode.HALF_UP);
  }

  // Private helper methods

  private void validateSubscription(Subscription subscription) {
    if (subscription == null) {
      throw new IllegalArgumentException("Subscription cannot be null");
    }
    if (subscription.getPlan() == null) {
      throw new IllegalArgumentException("Subscription must have a plan");
    }
  }

  private void validateNewPlan(SubscriptionPlan newPlan) {
    validatePlan(newPlan, "New plan");
  }

  private void validatePlan(SubscriptionPlan plan, String paramName) {
    if (plan == null) {
      throw new IllegalArgumentException(paramName + " cannot be null");
    }
    if (plan.getBasePrice() == null) {
      throw new IllegalArgumentException(paramName + " must have a base price");
    }
  }

  private void validateEffectiveDate(LocalDateTime effectiveDate) {
    if (effectiveDate == null) {
      throw new IllegalArgumentException("Effective date cannot be null");
    }
  }

  private LocalDateTime calculateNextPeriodEnd(LocalDateTime start, SubscriptionPlan plan) {
    return switch (plan.getBillingCycle()) {
      case MONTHLY -> start.plusMonths(1);
      case YEARLY -> start.plusYears(1);
      case LIFETIME -> start.plusYears(100); // Effectively never expires
    };
  }

  private BigDecimal calculateAnnualCost(SubscriptionPlan plan) {
    var basePrice = plan.getBasePrice();

    return switch (plan.getBillingCycle()) {
      case MONTHLY -> basePrice.multiply(BigDecimal.valueOf(12));
      case YEARLY -> basePrice;
      case LIFETIME -> basePrice; // One-time cost
    };
  }
}
