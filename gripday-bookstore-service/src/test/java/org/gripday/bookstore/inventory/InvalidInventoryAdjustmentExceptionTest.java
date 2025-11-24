package org.gripday.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidInventoryAdjustmentException Tests")
class InvalidInventoryAdjustmentExceptionTest {

  @Test
  @DisplayName("Should create exception with book ID and adjustment details")
  void shouldCreateExceptionWithBookIdAndAdjustmentDetails() {
    // Arrange & Act
    var exception = new InvalidInventoryAdjustmentException(1L, 10, -15);

    // Assert
    assertThat(exception.getMessage())
        .contains("Invalid inventory adjustment for book ID 1")
        .contains("current quantity 10")
        .contains("adjustment -15")
        .contains("negative quantity");
    assertThat(exception.getBookId()).isEqualTo(1L);
    assertThat(exception.getCurrentQuantity()).isEqualTo(10);
    assertThat(exception.getAdjustment()).isEqualTo(-15);
    assertThat(exception.getResultingQuantity()).isEqualTo(-5);
  }

  @Test
  @DisplayName("Should create exception with custom message")
  void shouldCreateExceptionWithCustomMessage() {
    // Arrange & Act
    var exception = new InvalidInventoryAdjustmentException("Custom error message");

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Custom error message");
    assertThat(exception.getBookId()).isNull();
    assertThat(exception.getCurrentQuantity()).isZero();
    assertThat(exception.getAdjustment()).isZero();
    assertThat(exception.getResultingQuantity()).isZero();
  }

  @Test
  @DisplayName("Should be a RuntimeException")
  void shouldBeRuntimeException() {
    // Arrange & Act
    var exception = new InvalidInventoryAdjustmentException(1L, 10, -15);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
