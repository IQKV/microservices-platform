package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidPriceRangeException Tests")
class InvalidPriceRangeExceptionTest {

  @Test
  @DisplayName("Should create exception with price range details")
  void shouldCreateExceptionWithPriceRangeDetails() {
    // Arrange
    var minPrice = new BigDecimal("50.00");
    var maxPrice = new BigDecimal("20.00");

    // Act
    var exception = new InvalidPriceRangeException(minPrice, maxPrice);

    // Assert
    assertThat(exception.getMessage())
        .contains("Invalid price range")
        .contains("minimum price (50.00)")
        .contains("maximum price (20.00)")
        .contains("cannot be greater than");
    assertThat(exception.getMinPrice()).isEqualTo(minPrice);
    assertThat(exception.getMaxPrice()).isEqualTo(maxPrice);
  }

  @Test
  @DisplayName("Should be a RuntimeException")
  void shouldBeRuntimeException() {
    // Arrange & Act
    var exception = new InvalidPriceRangeException(
        new BigDecimal("50.00"),
        new BigDecimal("20.00")
    );

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
