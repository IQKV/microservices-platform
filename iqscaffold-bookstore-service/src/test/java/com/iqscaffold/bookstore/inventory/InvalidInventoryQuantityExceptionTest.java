package com.iqscaffold.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidInventoryQuantityException Tests")
class InvalidInventoryQuantityExceptionTest {

  @Test
  @DisplayName("Should create exception with book ID and quantities")
  void shouldCreateExceptionWithBookIdAndQuantities() {
    // Arrange & Act
    var exception = new InvalidInventoryQuantityException(1L, 5, 10);

    // Assert
    assertThat(exception.getMessage())
        .contains("Cannot set inventory quantity to 5 for book ID 1")
        .contains("reserved quantity is 10");
    assertThat(exception.getBookId()).isEqualTo(1L);
    assertThat(exception.getRequestedQuantity()).isEqualTo(5);
    assertThat(exception.getReservedQuantity()).isEqualTo(10);
  }

  @Test
  @DisplayName("Should be a RuntimeException")
  void shouldBeRuntimeException() {
    // Arrange & Act
    var exception = new InvalidInventoryQuantityException(1L, 5, 10);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
