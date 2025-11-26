package com.iqscaffold.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UnauthorizedOperationException Tests")
class UnauthorizedOperationExceptionTest {

  @Test
  @DisplayName("Should create exception with operation only")
  void shouldCreateExceptionWithOperationOnly() {
    // Arrange
    var operation = "DELETE_BOOK";

    // Act
    var exception = new UnauthorizedOperationException(operation);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
    assertThat(exception.getMessage()).isEqualTo("Unauthorized to perform operation: DELETE_BOOK");
  }

  @Test
  @DisplayName("Should create exception with operation and required role")
  void shouldCreateExceptionWithOperationAndRequiredRole() {
    // Arrange
    var operation = "DELETE_BOOK";
    var requiredRole = "ADMIN";

    // Act
    var exception = new UnauthorizedOperationException(operation, requiredRole);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
    assertThat(exception.getMessage())
        .isEqualTo("Unauthorized to perform operation: DELETE_BOOK. Required role: ADMIN");
  }

  @Test
  @DisplayName("Should handle null operation")
  void shouldHandleNullOperation() {
    // Act
    var exception = new UnauthorizedOperationException(null);

    // Assert
    assertThat(exception.getMessage()).contains("null");
  }

  @Test
  @DisplayName("Should handle empty operation")
  void shouldHandleEmptyOperation() {
    // Act
    var exception = new UnauthorizedOperationException("");

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Unauthorized to perform operation: ");
  }
}
