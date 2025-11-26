package com.iqscaffold.bookstore.catalog;

import java.math.BigDecimal;

/**
 * Exception thrown when price range is invalid (min > max).
 */
public class InvalidPriceRangeException extends RuntimeException {

  private final BigDecimal minPrice;
  private final BigDecimal maxPrice;

  public InvalidPriceRangeException(final BigDecimal minPrice, final BigDecimal maxPrice) {
    super(String.format(
        "Invalid price range: minimum price (%s) cannot be greater than maximum price (%s)",
        minPrice, maxPrice));
    this.minPrice = minPrice;
    this.maxPrice = maxPrice;
  }

  public BigDecimal getMinPrice() {
    return minPrice;
  }

  public BigDecimal getMaxPrice() {
    return maxPrice;
  }
}
