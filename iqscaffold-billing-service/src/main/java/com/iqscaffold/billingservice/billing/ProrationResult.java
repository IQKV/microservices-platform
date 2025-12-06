package com.iqscaffold.billingservice.billing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable value object representing the result of a proration calculation.
 * 
 * <p>Encapsulates the financial details when a subscription plan changes mid-period,
 * including credits for unused time on the old plan and charges for the new plan.
 * 
 * <p>Proration ensures fair billing when customers upgrade or downgrade their
 * subscription plans. The calculation considers:
 * <ul>
 *   <li>Days remaining in the current billing period</li>
 *   <li>Total days in the billing period</li>
 *   <li>Price difference between old and new plans</li>
 *   <li>Credit for unused time on the old plan</li>
 *   <li>Charge for the new plan for the remaining period</li>
 * </ul>
 * 
 * <p>As a Java record, this class is:
 * <ul>
 *   <li>Immutable - all fields are final</li>
 *   <li>Value-based - equality based on field values</li>
 *   <li>Compact - automatic constructor, getters, equals, hashCode, toString</li>
 * </ul>
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
 *   invoice.addLineItem(
 *     InvoiceLineItem.prorationCredit(result.description(), result.creditAmount())
 *   );
 *   invoice.addLineItem(
 *     InvoiceLineItem.prorationCharge(result.description(), result.chargeAmount())
 *   );
 * }
 * }</pre>
 * 
 * @param creditAmount amount credited for unused time on old plan (always non-negative)
 * @param chargeAmount amount charged for new plan for remaining period (always non-negative)
 * @param netAmount net amount to charge (chargeAmount - creditAmount)
 * @param daysRemaining number of days remaining in the current billing period
 * @param daysInPeriod total number of days in the billing period
 * @param description human-readable description of the proration
 * 
 * @see com.iqscaffold.billingservice.invoice.InvoiceLineItem
 */
public record ProrationResult(
    @NotNull(message = "Credit amount cannot be null")
    @PositiveOrZero(message = "Credit amount must be non-negative")
    BigDecimal creditAmount,

    @NotNull(message = "Charge amount cannot be null")
    @PositiveOrZero(message = "Charge amount must be non-negative")
    BigDecimal chargeAmount,

    @NotNull(message = "Net amount cannot be null")
    BigDecimal netAmount,

    @PositiveOrZero(message = "Days remaining must be non-negative")
    int daysRemaining,

    @PositiveOrZero(message = "Days in period must be positive")
    int daysInPeriod,

    @NotNull(message = "Description cannot be null")
    String description
) {

  /**
   * Compact constructor with validation and calculation.
   * 
   * @param creditAmount credit for unused time
   * @param chargeAmount charge for new plan
   * @param netAmount net amount to charge
   * @param daysRemaining days remaining in period
   * @param daysInPeriod total days in period
   * @param description proration description
   * @throws IllegalArgumentException if validation fails
   */
  public ProrationResult {
    // Validate credit amount
    if (creditAmount == null) {
      throw new IllegalArgumentException("Credit amount cannot be null");
    }
    if (creditAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Credit amount must be non-negative");
    }

    // Validate charge amount
    if (chargeAmount == null) {
      throw new IllegalArgumentException("Charge amount cannot be null");
    }
    if (chargeAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Charge amount must be non-negative");
    }

    // Validate net amount
    if (netAmount == null) {
      throw new IllegalArgumentException("Net amount cannot be null");
    }

    // Validate days
    if (daysRemaining < 0) {
      throw new IllegalArgumentException("Days remaining must be non-negative");
    }
    if (daysInPeriod <= 0) {
      throw new IllegalArgumentException("Days in period must be positive");
    }
    if (daysRemaining > daysInPeriod) {
      throw new IllegalArgumentException(
          "Days remaining cannot exceed days in period. " +
          "Days remaining: " + daysRemaining + ", Days in period: " + daysInPeriod
      );
    }

    // Validate description
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Description cannot be null or blank");
    }

    // Ensure proper scale for monetary values (2 decimal places)
    creditAmount = creditAmount.setScale(2, RoundingMode.HALF_UP);
    chargeAmount = chargeAmount.setScale(2, RoundingMode.HALF_UP);
    netAmount = netAmount.setScale(2, RoundingMode.HALF_UP);

    // Verify net amount calculation
    BigDecimal calculatedNetAmount = chargeAmount
        .subtract(creditAmount)
        .setScale(2, RoundingMode.HALF_UP);

    if (netAmount.compareTo(calculatedNetAmount) != 0) {
      throw new IllegalArgumentException(
          "Net amount must equal charge amount minus credit amount. " +
          "Expected: " + calculatedNetAmount + ", Got: " + netAmount
      );
    }
  }

  /**
   * Factory method to create a proration result for an upgrade scenario.
   * 
   * <p>In an upgrade, the customer receives credit for unused time on the old plan
   * and is charged for the new plan for the remaining period. The net amount is
   * typically positive (customer owes money).
   * 
   * @param oldPlanPrice price of the old plan
   * @param newPlanPrice price of the new plan
   * @param daysRemaining days remaining in the billing period
   * @param daysInPeriod total days in the billing period
   * @param oldPlanName name of the old plan
   * @param newPlanName name of the new plan
   * @return a ProrationResult for the upgrade
   */
  public static ProrationResult forUpgrade(
      final BigDecimal oldPlanPrice,
      final BigDecimal newPlanPrice,
      final int daysRemaining,
      final int daysInPeriod,
      final String oldPlanName,
      final String newPlanName
  ) {
    BigDecimal prorationFactor = BigDecimal.valueOf(daysRemaining)
        .divide(BigDecimal.valueOf(daysInPeriod), 10, RoundingMode.HALF_UP);

    BigDecimal creditAmount = oldPlanPrice
        .multiply(prorationFactor)
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal chargeAmount = newPlanPrice
        .multiply(prorationFactor)
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal netAmount = chargeAmount
        .subtract(creditAmount)
        .setScale(2, RoundingMode.HALF_UP);

    String description = String.format(
        "Upgrade from %s to %s (%d/%d days remaining)",
        oldPlanName,
        newPlanName,
        daysRemaining,
        daysInPeriod
    );

    return new ProrationResult(
        creditAmount,
        chargeAmount,
        netAmount,
        daysRemaining,
        daysInPeriod,
        description
    );
  }

  /**
   * Factory method to create a proration result for a downgrade scenario.
   * 
   * <p>In a downgrade, the customer receives credit for unused time on the old plan
   * and is charged for the new plan for the remaining period. The net amount is
   * typically negative (customer receives credit).
   * 
   * @param oldPlanPrice price of the old plan
   * @param newPlanPrice price of the new plan
   * @param daysRemaining days remaining in the billing period
   * @param daysInPeriod total days in the billing period
   * @param oldPlanName name of the old plan
   * @param newPlanName name of the new plan
   * @return a ProrationResult for the downgrade
   */
  public static ProrationResult forDowngrade(
      final BigDecimal oldPlanPrice,
      final BigDecimal newPlanPrice,
      final int daysRemaining,
      final int daysInPeriod,
      final String oldPlanName,
      final String newPlanName
  ) {
    BigDecimal prorationFactor = BigDecimal.valueOf(daysRemaining)
        .divide(BigDecimal.valueOf(daysInPeriod), 10, RoundingMode.HALF_UP);

    BigDecimal creditAmount = oldPlanPrice
        .multiply(prorationFactor)
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal chargeAmount = newPlanPrice
        .multiply(prorationFactor)
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal netAmount = chargeAmount
        .subtract(creditAmount)
        .setScale(2, RoundingMode.HALF_UP);

    String description = String.format(
        "Downgrade from %s to %s (%d/%d days remaining)",
        oldPlanName,
        newPlanName,
        daysRemaining,
        daysInPeriod
    );

    return new ProrationResult(
        creditAmount,
        chargeAmount,
        netAmount,
        daysRemaining,
        daysInPeriod,
        description
    );
  }

  /**
   * Factory method to create a proration result with no proration (full period).
   * 
   * <p>Used when a subscription starts at the beginning of a billing period
   * or when no proration is needed.
   * 
   * @param planPrice price of the plan
   * @param daysInPeriod total days in the billing period
   * @param planName name of the plan
   * @return a ProrationResult with no proration
   */
  public static ProrationResult noProration(
      final BigDecimal planPrice,
      final int daysInPeriod,
      final String planName
  ) {
    return new ProrationResult(
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
        planPrice.setScale(2, RoundingMode.HALF_UP),
        planPrice.setScale(2, RoundingMode.HALF_UP),
        daysInPeriod,
        daysInPeriod,
        String.format("Full period charge for %s", planName)
    );
  }

  /**
   * Checks if this proration represents an upgrade (net amount is positive).
   * 
   * @return true if customer owes money (upgrade)
   */
  public boolean isUpgrade() {
    return netAmount.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Checks if this proration represents a downgrade (net amount is negative).
   * 
   * @return true if customer receives credit (downgrade)
   */
  public boolean isDowngrade() {
    return netAmount.compareTo(BigDecimal.ZERO) < 0;
  }

  /**
   * Checks if this proration has no net change (net amount is zero).
   * 
   * @return true if no money is owed or credited
   */
  public boolean isNeutral() {
    return netAmount.compareTo(BigDecimal.ZERO) == 0;
  }

  /**
   * Gets the absolute value of the net amount.
   * 
   * @return absolute net amount
   */
  public BigDecimal getAbsoluteNetAmount() {
    return netAmount.abs();
  }

  /**
   * Calculates the proration factor (days remaining / days in period).
   * 
   * @return proration factor as a decimal between 0 and 1
   */
  public BigDecimal getProrationFactor() {
    if (daysInPeriod == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(daysRemaining)
        .divide(BigDecimal.valueOf(daysInPeriod), 10, RoundingMode.HALF_UP);
  }

  /**
   * Calculates the percentage of the period remaining.
   * 
   * @return percentage remaining (0-100)
   */
  public double getPercentageRemaining() {
    if (daysInPeriod == 0) {
      return 0.0;
    }
    return (daysRemaining * 100.0) / daysInPeriod;
  }

  /**
   * Checks if proration is needed (days remaining is less than total days).
   * 
   * @return true if proration is needed
   */
  public boolean requiresProration() {
    return daysRemaining < daysInPeriod;
  }

  /**
   * Creates a detailed description including all amounts.
   * 
   * @return detailed description string
   */
  public String getDetailedDescription() {
    return String.format(
        "%s - Credit: $%s, Charge: $%s, Net: $%s",
        description,
        creditAmount,
        chargeAmount,
        netAmount
    );
  }
}
