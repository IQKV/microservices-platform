package com.iqscaffold.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BulkOperationException Tests")
class BulkOperationExceptionTest {

  @Test
  @DisplayName("Should create exception with failure details")
  void shouldCreateExceptionWithFailureDetails() {
    // Arrange
    var failures = List.of(
        new BulkOperationException.BulkOperationFailure(1L, "Quantity below reserved", null),
        new BulkOperationException.BulkOperationFailure(2L, "Book not found", new RuntimeException("Not found"))
    );

    // Act
    var exception = new BulkOperationException(5, 3, failures);

    // Assert
    assertThat(exception.getMessage())
        .contains("Bulk operation completed with failures")
        .contains("3 succeeded")
        .contains("2 failed")
        .contains("5 total");
    assertThat(exception.getTotalRequested()).isEqualTo(5);
    assertThat(exception.getSuccessCount()).isEqualTo(3);
    assertThat(exception.getFailureCount()).isEqualTo(2);
    assertThat(exception.getFailures()).hasSize(2);
  }

  @Test
  @DisplayName("Should return unmodifiable list of failures")
  void shouldReturnUnmodifiableListOfFailures() {
    // Arrange
    var failures = List.of(
        new BulkOperationException.BulkOperationFailure(1L, "Error", null)
    );
    var exception = new BulkOperationException(2, 1, failures);

    // Act
    var returnedFailures = exception.getFailures();

    // Assert
    assertThat(returnedFailures).isUnmodifiable();
  }

  @Test
  @DisplayName("Should create BulkOperationFailure with details")
  void shouldCreateBulkOperationFailureWithDetails() {
    // Arrange
    var cause = new RuntimeException("Test exception");

    // Act
    var failure = new BulkOperationException.BulkOperationFailure(1L, "Test reason", cause);

    // Assert
    assertThat(failure.getBookId()).isEqualTo(1L);
    assertThat(failure.getReason()).isEqualTo("Test reason");
    assertThat(failure.getException()).isEqualTo(cause);
    assertThat(failure.toString()).contains("Book ID 1").contains("Test reason");
  }

  @Test
  @DisplayName("Should be a RuntimeException")
  void shouldBeRuntimeException() {
    // Arrange
    var failures = List.of(
        new BulkOperationException.BulkOperationFailure(1L, "Error", null)
    );

    // Act
    var exception = new BulkOperationException(2, 1, failures);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
