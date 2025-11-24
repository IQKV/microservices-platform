package org.gripday.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InsufficientInventoryException Tests")
class InsufficientInventoryExceptionTest {

  @Test
  @DisplayName("Should create exception with book details")
  void shouldCreateExceptionWithBookDetails() {
    // Arrange
    var bookId = 1L;
    var requested = 10;
    var available = 5;

    // Act
    var exception = new InsufficientInventoryException(bookId, requested, available);

    // Assert
    assertThat(exception.getMessage())
        .contains("Insufficient inventory for book ID: 1")
        .contains("Requested: 10")
        .contains("Available: 5");
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Should create exception with custom message")
  void shouldCreateExceptionWithCustomMessage() {
    // Arrange
    var message = "Custom inventory error message";

    // Act
    var exception = new InsufficientInventoryException(message);

    // Assert
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
