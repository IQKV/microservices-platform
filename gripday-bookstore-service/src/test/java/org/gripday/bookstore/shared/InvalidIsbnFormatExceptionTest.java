package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidIsbnFormatException Tests")
class InvalidIsbnFormatExceptionTest {

  @Test
  @DisplayName("Should create exception with ISBN")
  void shouldCreateExceptionWithIsbn() {
    // Arrange & Act
    var exception = new InvalidIsbnFormatException("123456789");

    // Assert
    assertThat(exception.getMessage())
        .contains("Invalid ISBN format: 123456789")
        .contains("Must be valid ISBN-10 or ISBN-13");
    assertThat(exception.getIsbn()).isEqualTo("123456789");
  }

  @Test
  @DisplayName("Should create exception with ISBN and reason")
  void shouldCreateExceptionWithIsbnAndReason() {
    // Arrange & Act
    var exception = new InvalidIsbnFormatException("", "ISBN must not be blank");

    // Assert
    assertThat(exception.getMessage())
        .contains("Invalid ISBN format: ")
        .contains("Reason: ISBN must not be blank");
    assertThat(exception.getIsbn()).isEmpty();
  }

  @Test
  @DisplayName("Should be a RuntimeException")
  void shouldBeRuntimeException() {
    // Arrange & Act
    var exception = new InvalidIsbnFormatException("123456789");

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
