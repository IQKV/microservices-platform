package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BookId Tests")
class BookIdTest {

  @Test
  @DisplayName("Should create BookId with valid value")
  void shouldCreateBookIdWithValidValue() {
    // Arrange & Act
    var bookId = BookId.of(1L);

    // Assert
    assertThat(bookId).isNotNull();
    assertThat(bookId.getValue()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should throw exception when value is null")
  void shouldThrowExceptionWhenValueIsNull() {
    // Act & Assert
    assertThatThrownBy(() -> BookId.of(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("BookId must not be null");
  }

  @Test
  @DisplayName("Should throw exception when value is zero")
  void shouldThrowExceptionWhenValueIsZero() {
    // Act & Assert
    assertThatThrownBy(() -> BookId.of(0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("BookId must be positive");
  }

  @Test
  @DisplayName("Should throw exception when value is negative")
  void shouldThrowExceptionWhenValueIsNegative() {
    // Act & Assert
    assertThatThrownBy(() -> BookId.of(-1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("BookId must be positive");
  }

  @Test
  @DisplayName("Should be equal when values are the same")
  void shouldBeEqualWhenValuesAreTheSame() {
    // Arrange
    var bookId1 = BookId.of(1L);
    var bookId2 = BookId.of(1L);

    // Act & Assert
    assertThat(bookId1).isEqualTo(bookId2);
    assertThat(bookId1.hashCode()).isEqualTo(bookId2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal when values are different")
  void shouldNotBeEqualWhenValuesAreDifferent() {
    // Arrange
    var bookId1 = BookId.of(1L);
    var bookId2 = BookId.of(2L);

    // Act & Assert
    assertThat(bookId1).isNotEqualTo(bookId2);
  }

  @Test
  @DisplayName("Should not be equal to null")
  void shouldNotBeEqualToNull() {
    // Arrange
    var bookId = BookId.of(1L);

    // Act & Assert
    assertThat(bookId).isNotEqualTo(null);
  }

  @Test
  @DisplayName("Should not be equal to different type")
  void shouldNotBeEqualToDifferentType() {
    // Arrange
    var bookId = BookId.of(1L);

    // Act & Assert
    assertThat(bookId).isNotEqualTo("1");
  }

  @Test
  @DisplayName("Should be equal to itself")
  void shouldBeEqualToItself() {
    // Arrange
    var bookId = BookId.of(1L);

    // Act & Assert
    assertThat(bookId).isEqualTo(bookId);
  }

  @Test
  @DisplayName("Should return string representation of value")
  void shouldReturnStringRepresentationOfValue() {
    // Arrange
    var bookId = BookId.of(123L);

    // Act
    var result = bookId.toString();

    // Assert
    assertThat(result).isEqualTo("123");
  }
}
