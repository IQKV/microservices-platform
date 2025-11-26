package com.iqscaffold.bookstore.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representing monetary amounts.
 * Immutable and self-validating following DDD principles.
 */
@Embeddable
public class Money implements Serializable, Comparable<Money> {

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  protected Money() {
    // For JPA only
  }

  private Money(final BigDecimal amount, final String currency) {
    validateAmount(amount);
    validateCurrency(currency);

    this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    this.currency = currency.toUpperCase();
  }

  /**
   * Creates a new Money value object.
   *
   * @param amount   the monetary amount
   * @param currency the currency code (ISO 4217, e.g., "USD", "EUR")
   * @return a new Money instance
   * @throws IllegalArgumentException if amount or currency is invalid
   */
  public static Money of(final BigDecimal amount, final String currency) {
    return new Money(amount, currency);
  }

  /**
   * Creates a new Money value object with USD currency.
   *
   * @param amount the monetary amount
   * @return a new Money instance in USD
   * @throws IllegalArgumentException if amount is invalid
   */
  public static Money usd(final BigDecimal amount) {
    return new Money(amount, "USD");
  }

  /**
   * Creates a new Money value object with EUR currency.
   *
   * @param amount the monetary amount
   * @return a new Money instance in EUR
   * @throws IllegalArgumentException if amount is invalid
   */
  public static Money eur(final BigDecimal amount) {
    return new Money(amount, "EUR");
  }

  /**
   * Creates a zero Money value object.
   *
   * @param currency the currency code
   * @return a new Money instance with zero amount
   */
  public static Money zero(final String currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  private void validateAmount(final BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount must not be null");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount must not be negative");
    }
  }

  private void validateCurrency(final String currency) {
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency must not be null or blank");
    }
    try {
      Currency.getInstance(currency.toUpperCase());
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid currency code: " + currency);
    }
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  /**
   * Adds another Money value to this one.
   *
   * @param other the Money to add
   * @return a new Money instance with the sum
   * @throws IllegalArgumentException if currencies don't match
   */
  public Money add(final Money other) {
    ensureSameCurrency(other);
    return new Money(this.amount.add(other.amount), this.currency);
  }

  /**
   * Subtracts another Money value from this one.
   *
   * @param other the Money to subtract
   * @return a new Money instance with the difference
   * @throws IllegalArgumentException if currencies don't match or result is negative
   */
  public Money subtract(final Money other) {
    ensureSameCurrency(other);
    BigDecimal result = this.amount.subtract(other.amount);
    if (result.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Subtraction would result in negative amount");
    }
    return new Money(result, this.currency);
  }

  /**
   * Multiplies this Money by a factor.
   *
   * @param factor the multiplication factor
   * @return a new Money instance with the product
   */
  public Money multiply(final BigDecimal factor) {
    if (factor == null) {
      throw new IllegalArgumentException("Factor must not be null");
    }
    if (factor.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Factor must not be negative");
    }
    return new Money(this.amount.multiply(factor), this.currency);
  }

  /**
   * Multiplies this Money by an integer quantity.
   *
   * @param quantity the quantity
   * @return a new Money instance with the product
   */
  public Money multiply(final int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("Quantity must not be negative");
    }
    return multiply(BigDecimal.valueOf(quantity));
  }

  /**
   * Checks if this Money is zero.
   */
  public boolean isZero() {
    return amount.compareTo(BigDecimal.ZERO) == 0;
  }

  /**
   * Checks if this Money is positive.
   */
  public boolean isPositive() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Checks if this Money is greater than another.
   */
  public boolean isGreaterThan(final Money other) {
    ensureSameCurrency(other);
    return this.amount.compareTo(other.amount) > 0;
  }

  /**
   * Checks if this Money is less than another.
   */
  public boolean isLessThan(final Money other) {
    ensureSameCurrency(other);
    return this.amount.compareTo(other.amount) < 0;
  }

  private void ensureSameCurrency(final Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          String.format("Currency mismatch: %s vs %s", this.currency, other.currency));
    }
  }

  @Override
  public int compareTo(Money other) {
    ensureSameCurrency(other);
    return this.amount.compareTo(other.amount);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Money money = (Money) o;
    return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }

  @Override
  public String toString() {
    return String.format("%s %s", currency, amount.toPlainString());
  }
}
