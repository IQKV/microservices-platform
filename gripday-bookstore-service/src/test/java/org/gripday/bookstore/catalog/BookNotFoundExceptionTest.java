package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BookNotFoundException Tests")
class BookNotFoundExceptionTest {

  @Test
  @DisplayName("Should create exception with book ID")
  void shouldCreateExceptionWithBookId() {
    // Arrange
    var bookId = 123L;

    // Act
    var exception = new BookNotFoundException(bookId);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
    assertThat(exception.getMessage()).isEqualTo("Book not found with ID: 123");
  }

  @Test
  @DisplayName("Should create exception with custom message")
  void shouldCreateExceptionWithCustomMessage() {
    // Arrange
    var message = "Custom error message";

    // Act
    var exception = new BookNotFoundException(message);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
    assertThat(exception.getMessage()).isEqualTo("Custom error message");
  }

  @Test
  @DisplayName("Should create exception by ISBN using factory method")
  void shouldCreateExceptionByIsbnUsingFactoryMethod() {
    // Arrange
    var isbn = "978-0-123456-78-9";

    // Act
    var exception = BookNotFoundException.byIsbn(isbn);

    // Assert
    assertThat(exception).isInstanceOf(BookNotFoundException.class);
    assertThat(exception.getMessage()).isEqualTo("Book not found with ISBN: 978-0-123456-78-9");
  }

  @Test
  @DisplayName("Should handle null ISBN in factory method")
  void shouldHandleNullIsbnInFactoryMethod() {
    // Act
    var exception = BookNotFoundException.byIsbn(null);

    // Assert
    assertThat(exception.getMessage()).contains("null");
  }

  @Test
  @DisplayName("Should handle empty ISBN in factory method")
  void shouldHandleEmptyIsbnInFactoryMethod() {
    // Act
    var exception = BookNotFoundException.byIsbn("");

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Book not found with ISBN: ");
  }

  @Test
  @DisplayName("Should be throwable")
  void shouldBeThrowable() {
    // Arrange
    var bookId = 999L;

    // Act & Assert
    try {
      throw new BookNotFoundException(bookId);
    } catch (final BookNotFoundException e) {
      assertThat(e.getMessage()).contains("999");
    }
  }
}
