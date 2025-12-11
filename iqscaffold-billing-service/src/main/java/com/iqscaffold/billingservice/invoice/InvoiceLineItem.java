package com.iqscaffold.billingservice.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable value object representing a line item on an invoice.
 *
 * <p>Line items represent individual charges or credits on an invoice, such as:
 * <ul>
 *   <li>Subscription fees</li>
 *   <li>Usage charges</li>
 *   <li>Proration credits</li>
 *   <li>Proration charges</li>
 *   <li>Discounts</li>
 *   <li>Taxes</li>
 * </ul>
 *
 * <p>This is implemented as a Java record for immutability and automatic
 * generation of equals(), hashCode(), and toString() methods based on value.
 *
 * <p>Line items enforce the following invariants:
 * <ul>
 *   <li>Description cannot be blank</li>
 *   <li>Quantity must be positive</li>
 *   <li>Unit price must be non-negative</li>
 *   <li>Amount is automatically calculated as quantity * unitPrice</li>
 *   <li>All monetary values use 2 decimal places</li>
 * </ul>
 *
 * @param type        the type of line item (SUBSCRIPTION_FEE, USAGE_CHARGE, etc.)
 * @param description human-readable description of the charge
 * @param quantity    number of units (e.g., 1 for subscription, 1000 for API calls)
 * @param unitPrice   price per unit in the invoice currency
 * @param amount      total amount for this line item (quantity * unitPrice)
 */
public record InvoiceLineItem(
    @NotNull(message = "Line item type cannot be null")
    LineItemType type,

    @NotBlank(message = "Line item description cannot be blank")
    String description,

    @Positive(message = "Line item quantity must be positive")
    Long quantity,

    @PositiveOrZero(message = "Line item unit price must be non-negative")
    BigDecimal unitPrice,

    @NotNull(message = "Line item amount cannot be null")
    BigDecimal amount
) {

  /**
   * Compact constructor that validates invariants and calculates amount.
   *
   * @throws IllegalArgumentException if invariants are violated
   */
  public InvoiceLineItem {
    // Validate type
    if (type == null) {
      throw new IllegalArgumentException("Line item type cannot be null");
    }

    // Validate description
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Line item description cannot be blank");
    }

    // Validate quantity
    if (quantity == null || quantity <= 0) {
      throw new IllegalArgumentException("Line item quantity must be positive");
    }

    // Validate unit price
    if (unitPrice == null) {
      throw new IllegalArgumentException("Line item unit price cannot be null");
    }
    
    // Only allow negative unit prices for credit/discount types
    boolean allowNegative = (type == LineItemType.PRORATION_CREDIT
        || type == LineItemType.DISCOUNT);
    if (!allowNegative && unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Line item unit price must be non-negative");
    }

    // Calculate and validate amount
    BigDecimal calculatedAmount = unitPrice
        .multiply(BigDecimal.valueOf(quantity))
        .setScale(2, RoundingMode.HALF_UP);

    if (amount == null) {
      amount = calculatedAmount;
    } else {
      // Verify provided amount matches calculated amount
      if (amount.compareTo(calculatedAmount) != 0) {
        throw new IllegalArgumentException(
            "Line item amount must equal quantity * unitPrice. " +
            "Expected: " + calculatedAmount + ", Got: " + amount
        );
      }
    }

    // Ensure proper scale for monetary values
    unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    amount = amount.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Factory method to create a subscription fee line item.
   *
   * @param description description of the subscription
   * @param price       subscription price
   * @return a new InvoiceLineItem for subscription fee
   */
  public static InvoiceLineItem subscriptionFee(String description, BigDecimal price) {
    return new InvoiceLineItem(
        LineItemType.SUBSCRIPTION_FEE,
        description,
        1L,
        price,
        price.setScale(2, RoundingMode.HALF_UP)
    );
  }

  /**
   * Factory method to create a usage charge line item.
   *
   * @param description description of the usage
   * @param quantity    number of units consumed
   * @param unitPrice   price per unit
   * @return a new InvoiceLineItem for usage charge
   */
  public static InvoiceLineItem usageCharge(
      String description,
      Long quantity,
      BigDecimal unitPrice
  ) {
    BigDecimal amount = unitPrice
        .multiply(BigDecimal.valueOf(quantity))
        .setScale(2, RoundingMode.HALF_UP);

    return new InvoiceLineItem(
        LineItemType.USAGE_CHARGE,
        description,
        quantity,
        unitPrice,
        amount
    );
  }

  /**
   * Factory method to create a proration credit line item.
   *
   * @param description description of the credit
   * @param amount      credit amount (positive value, will be negated)
   * @return a new InvoiceLineItem for proration credit
   */
  public static InvoiceLineItem prorationCredit(String description, BigDecimal amount) {
    BigDecimal creditAmount = amount.negate().setScale(2, RoundingMode.HALF_UP);
    return new InvoiceLineItem(
        LineItemType.PRORATION_CREDIT,
        description,
        1L,
        creditAmount,
        creditAmount
    );
  }

  /**
   * Factory method to create a proration charge line item.
   *
   * @param description description of the charge
   * @param amount      charge amount
   * @return a new InvoiceLineItem for proration charge
   */
  public static InvoiceLineItem prorationCharge(String description, BigDecimal amount) {
    return new InvoiceLineItem(
        LineItemType.PRORATION_CHARGE,
        description,
        1L,
        amount.setScale(2, RoundingMode.HALF_UP),
        amount.setScale(2, RoundingMode.HALF_UP)
    );
  }

  /**
   * Factory method to create a discount line item.
   *
   * @param description description of the discount
   * @param amount      discount amount (positive value, will be negated)
   * @return a new InvoiceLineItem for discount
   */
  public static InvoiceLineItem discount(String description, BigDecimal amount) {
    BigDecimal negatedAmount = amount.negate().setScale(2, RoundingMode.HALF_UP);
    return new InvoiceLineItem(
        LineItemType.DISCOUNT,
        description,
        1L,
        negatedAmount,
        negatedAmount
    );
  }

  /**
   * Factory method to create a tax line item.
   *
   * @param description description of the tax
   * @param amount      tax amount
   * @return a new InvoiceLineItem for tax
   */
  public static InvoiceLineItem tax(String description, BigDecimal amount) {
    return new InvoiceLineItem(
        LineItemType.TAX,
        description,
        1L,
        amount.setScale(2, RoundingMode.HALF_UP),
        amount.setScale(2, RoundingMode.HALF_UP)
    );
  }

  /**
   * Checks if this line item is a credit (negative amount).
   *
   * @return true if amount is negative
   */
  public boolean isCredit() {
    return amount.compareTo(BigDecimal.ZERO) < 0;
  }

  /**
   * Checks if this line item is a charge (positive amount).
   *
   * @return true if amount is positive
   */
  public boolean isCharge() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Gets the absolute value of the amount.
   *
   * @return absolute amount
   */
  public BigDecimal getAbsoluteAmount() {
    return amount.abs();
  }

  /**
   * Enumeration of line item types.
   */
  public enum LineItemType {
    /**
     * Subscription fee for the billing period.
     */
    SUBSCRIPTION_FEE,

    /**
     * Usage-based charges (overages, metered usage).
     */
    USAGE_CHARGE,

    /**
     * Credit for unused time when downgrading.
     */
    PRORATION_CREDIT,

    /**
     * Charge for additional time when upgrading.
     */
    PRORATION_CHARGE,

    /**
     * Promotional or other discounts.
     */
    DISCOUNT,

    /**
     * Tax charges (sales tax, VAT, etc.).
     */
    TAX
  }
}
