package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ISBN Value Object Tests")
class ISBNTest {

  @Test
  @DisplayName("Should create valid ISBN-10")
  void shouldCreateValidISBN10() {
    // Arrange & Act
    var isbn = ISBN.of("0306406152");

    // Assert
    assertThat(isbn.getValue()).isEqualTo("0306406152");
    assertThat(isbn.isISBN10()).isTrue();
    assertThat(isbn.isISBN13()).isFalse();
  }

  @Test
  @DisplayName("Should create valid ISBN-13")
  void shouldCreateValidISBN13() {
    // Arrange & Act
    var isbn = ISBN.of("9780306406157");

    // Assert
    assertThat(isbn.getValue()).isEqualTo("9780306406157");
    assertThat(isbn.isISBN13()).isTrue();
    assertThat(isbn.isISBN10()).isFalse();
  }

  @Test
  @DisplayName("Should normalize ISBN by removing hyphens")
  void shouldNormalizeISBNByRemovingHyphens() {
    // Arrange & Act
    var isbn = ISBN.of("0-306-40615-2");

    // Assert
    assertThat(isbn.getValue()).isEqualTo("0306406152");
  }

  @Test
  @DisplayName("Should normalize ISBN by removing spaces")
  void shouldNormalizeISBNByRemovingSpaces() {
    // Arrange & Act
    var isbn = ISBN.of("0 306 40615 2");

    // Assert
    assertThat(isbn.getValue()).isEqualTo("0306406152");
  }

  @Test
  @DisplayName("Should normalize ISBN by removing hyphens and spaces")
  void shouldNormalizeISBNByRemovingHyphensAndSpaces() {
    // Arrange & Act
    var isbn = ISBN.of("978-0-306-40615-7");

    // Assert
    assertThat(isbn.getValue()).isEqualTo("9780306406157");
  }

  @Test
  @DisplayName("Should throw exception for null ISBN")
  void shouldThrowExceptionForNullISBN() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ISBN must not be null or blank");
  }

  @Test
  @DisplayName("Should throw exception for blank ISBN")
  void shouldThrowExceptionForBlankISBN() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ISBN must not be null or blank");
  }

  @Test
  @DisplayName("Should throw exception for empty ISBN")
  void shouldThrowExceptionForEmptyISBN() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ISBN must not be null or blank");
  }

  @Test
  @DisplayName("Should throw exception for invalid ISBN-10")
  void shouldThrowExceptionForInvalidISBN10() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("0306406153"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid ISBN format");
  }

  @Test
  @DisplayName("Should throw exception for invalid ISBN-13")
  void shouldThrowExceptionForInvalidISBN13() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("9780306406158"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid ISBN format");
  }

  @Test
  @DisplayName("Should throw exception for ISBN with invalid length")
  void shouldThrowExceptionForISBNWithInvalidLength() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("123456789"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid ISBN format");
  }

  @Test
  @DisplayName("Should throw exception for ISBN with letters")
  void shouldThrowExceptionForISBNWithLetters() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("030640615A"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid ISBN format");
  }

  @Test
  @DisplayName("Should accept ISBN-10 with X as check digit")
  void shouldAcceptISBN10WithXAsCheckDigit() {
    // Arrange & Act
    var isbn = ISBN.of("043942089X");

    // Assert
    assertThat(isbn.getValue()).isEqualTo("043942089X");
    assertThat(isbn.isISBN10()).isTrue();
  }

  @Test
  @DisplayName("Should format ISBN-10 with hyphens")
  void shouldFormatISBN10WithHyphens() {
    // Arrange
    var isbn = ISBN.of("0306406152");

    // Act
    var formatted = isbn.getFormatted();

    // Assert
    assertThat(formatted).isEqualTo("0-3064-0615-2");
  }

  @Test
  @DisplayName("Should format ISBN-13 with hyphens")
  void shouldFormatISBN13WithHyphens() {
    // Arrange
    var isbn = ISBN.of("9780306406157");

    // Act
    var formatted = isbn.getFormatted();

    // Assert
    assertThat(formatted).isEqualTo("978-0-30640-615-7");
  }

  @Test
  @DisplayName("Should be equal when ISBNs have same value")
  void shouldBeEqualWhenISBNsHaveSameValue() {
    // Arrange
    var isbn1 = ISBN.of("0306406152");
    var isbn2 = ISBN.of("0-306-40615-2");

    // Act & Assert
    assertThat(isbn1).isEqualTo(isbn2);
    assertThat(isbn1.hashCode()).isEqualTo(isbn2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal when ISBNs have different values")
  void shouldNotBeEqualWhenISBNsHaveDifferentValues() {
    // Arrange
    var isbn1 = ISBN.of("0306406152");
    var isbn2 = ISBN.of("9780306406157");

    // Act & Assert
    assertThat(isbn1).isNotEqualTo(isbn2);
  }

  @Test
  @DisplayName("Should not be equal to null")
  void shouldNotBeEqualToNull() {
    // Arrange
    var isbn = ISBN.of("0306406152");

    // Act & Assert
    assertThat(isbn).isNotEqualTo(null);
  }

  @Test
  @DisplayName("Should not be equal to different type")
  void shouldNotBeEqualToDifferentType() {
    // Arrange
    var isbn = ISBN.of("0306406152");

    // Act & Assert
    assertThat(isbn).isNotEqualTo("0306406152");
  }

  @Test
  @DisplayName("Should be equal to itself")
  void shouldBeEqualToItself() {
    // Arrange
    var isbn = ISBN.of("0306406152");

    // Act & Assert
    assertThat(isbn).isEqualTo(isbn);
  }

  @Test
  @DisplayName("Should return ISBN value as string")
  void shouldReturnISBNValueAsString() {
    // Arrange
    var isbn = ISBN.of("0306406152");

    // Act
    var result = isbn.toString();

    // Assert
    assertThat(result).isEqualTo("0306406152");
  }

  @Test
  @DisplayName("Should validate ISBN-10 with all digits")
  void shouldValidateISBN10WithAllDigits() {
    // Valid ISBN-10 examples
    assertThat(ISBN.of("0306406152").isISBN10()).isTrue();
    assertThat(ISBN.of("0471958697").isISBN10()).isTrue();
  }

  @Test
  @DisplayName("Should validate ISBN-13 starting with 978")
  void shouldValidateISBN13StartingWith978() {
    // Arrange & Act
    var isbn = ISBN.of("9780306406157");

    // Assert
    assertThat(isbn.isISBN13()).isTrue();
    assertThat(isbn.getValue()).startsWith("978");
  }

  @Test
  @DisplayName("Should validate ISBN-13 starting with 979")
  void shouldValidateISBN13StartingWith979() {
    // Arrange & Act - Using a valid ISBN-13 starting with 979
    var isbn = ISBN.of("9791091146135");

    // Assert
    assertThat(isbn.isISBN13()).isTrue();
    assertThat(isbn.getValue()).startsWith("979");
  }

  @Test
  @DisplayName("Should handle ISBN-10 with invalid characters")
  void shouldHandleISBN10WithInvalidCharacters() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("030640615@"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should handle ISBN-13 with invalid characters")
  void shouldHandleISBN13WithInvalidCharacters() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("978030640615@"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should reject ISBN-10 with incorrect checksum")
  void shouldRejectISBN10WithIncorrectChecksum() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("0306406151"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should reject ISBN-13 with incorrect checksum")
  void shouldRejectISBN13WithIncorrectChecksum() {
    // Act & Assert
    assertThatThrownBy(() -> ISBN.of("9780306406156"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
