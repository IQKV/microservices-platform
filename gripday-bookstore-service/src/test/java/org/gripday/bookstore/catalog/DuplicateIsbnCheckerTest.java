package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.gripday.bookstore.shared.ISBN;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateIsbnChecker Domain Service Tests")
class DuplicateIsbnCheckerTest {

  @Mock
  private BookRepository bookRepository;

  @InjectMocks
  private DuplicateIsbnChecker duplicateIsbnChecker;

  private Book existingBook;
  private static final String EXISTING_ISBN = "9780132350884";
  private static final String NEW_ISBN = "9780134685991";

  @BeforeEach
  void setUp() {
    existingBook = Book.create(
        "Existing Book",
        "Test Author",
        EXISTING_ISBN,
        new java.math.BigDecimal("29.99"),
        "Test Description",
        null
    );
    existingBook.setId(1L);
  }

  @Nested
  @DisplayName("isDuplicate with ISBN Tests")
  class IsDuplicateWithIsbnTests {

    @Test
    @DisplayName("Should return true when ISBN exists")
    void shouldReturnTrueWhenIsbnExists() {
      // Arrange
      var isbn = ISBN.of(EXISTING_ISBN);
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act
      var result = duplicateIsbnChecker.isDuplicate(isbn);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when ISBN does not exist")
    void shouldReturnFalseWhenIsbnDoesNotExist() {
      // Arrange
      var isbn = ISBN.of(NEW_ISBN);
      when(bookRepository.findByIsbn(NEW_ISBN)).thenReturn(Optional.empty());

      // Act
      var result = duplicateIsbnChecker.isDuplicate(isbn);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when ISBN is null")
    void shouldThrowExceptionWhenIsbnIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.isDuplicate((ISBN) null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ISBN must not be null");
    }
  }

  @Nested
  @DisplayName("isDuplicate with String Tests")
  class IsDuplicateWithStringTests {

    @Test
    @DisplayName("Should return true when ISBN string exists")
    void shouldReturnTrueWhenIsbnStringExists() {
      // Arrange
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act
      var result = duplicateIsbnChecker.isDuplicate(EXISTING_ISBN);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when ISBN string does not exist")
    void shouldReturnFalseWhenIsbnStringDoesNotExist() {
      // Arrange
      when(bookRepository.findByIsbn(NEW_ISBN)).thenReturn(Optional.empty());

      // Act
      var result = duplicateIsbnChecker.isDuplicate(NEW_ISBN);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when ISBN string is null")
    void shouldThrowExceptionWhenIsbnStringIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.isDuplicate((String) null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ISBN must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when ISBN string is blank")
    void shouldThrowExceptionWhenIsbnStringIsBlank() {
      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.isDuplicate("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ISBN must not be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when ISBN string is empty")
    void shouldThrowExceptionWhenIsbnStringIsEmpty() {
      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.isDuplicate(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ISBN must not be null or blank");
    }
  }

  @Nested
  @DisplayName("isDuplicateExcluding Tests")
  class IsDuplicateExcludingTests {

    @Test
    @DisplayName("Should return false when ISBN belongs to excluded book")
    void shouldReturnFalseWhenIsbnBelongsToExcludedBook() {
      // Arrange
      var isbn = ISBN.of(EXISTING_ISBN);
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act
      var result = duplicateIsbnChecker.isDuplicateExcluding(isbn, 1L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when ISBN belongs to different book")
    void shouldReturnTrueWhenIsbnBelongsToDifferentBook() {
      // Arrange
      var isbn = ISBN.of(EXISTING_ISBN);
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act
      var result = duplicateIsbnChecker.isDuplicateExcluding(isbn, 2L);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when ISBN does not exist")
    void shouldReturnFalseWhenIsbnDoesNotExist() {
      // Arrange
      var isbn = ISBN.of(NEW_ISBN);
      when(bookRepository.findByIsbn(NEW_ISBN)).thenReturn(Optional.empty());

      // Act
      var result = duplicateIsbnChecker.isDuplicateExcluding(isbn, 1L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when ISBN is null")
    void shouldThrowExceptionWhenIsbnIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.isDuplicateExcluding(null, 1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ISBN must not be null");
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Arrange
      var isbn = ISBN.of(EXISTING_ISBN);

      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.isDuplicateExcluding(isbn, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }
  }

  @Nested
  @DisplayName("ensureUnique with ISBN Tests")
  class EnsureUniqueWithIsbnTests {

    @Test
    @DisplayName("Should not throw exception when ISBN is unique")
    void shouldNotThrowExceptionWhenIsbnIsUnique() {
      // Arrange
      var isbn = ISBN.of(NEW_ISBN);
      when(bookRepository.findByIsbn(NEW_ISBN)).thenReturn(Optional.empty());

      // Act & Assert
      duplicateIsbnChecker.ensureUnique(isbn);
    }

    @Test
    @DisplayName("Should throw DuplicateIsbnException when ISBN exists")
    void shouldThrowDuplicateIsbnExceptionWhenIsbnExists() {
      // Arrange
      var isbn = ISBN.of(EXISTING_ISBN);
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.ensureUnique(isbn))
          .isInstanceOf(DuplicateIsbnException.class)
          .hasMessage("Book with ISBN already exists: " + EXISTING_ISBN);
    }
  }

  @Nested
  @DisplayName("ensureUnique with String Tests")
  class EnsureUniqueWithStringTests {

    @Test
    @DisplayName("Should not throw exception when ISBN string is unique")
    void shouldNotThrowExceptionWhenIsbnStringIsUnique() {
      // Arrange
      when(bookRepository.findByIsbn(NEW_ISBN)).thenReturn(Optional.empty());

      // Act & Assert
      duplicateIsbnChecker.ensureUnique(NEW_ISBN);
    }

    @Test
    @DisplayName("Should throw DuplicateIsbnException when ISBN string exists")
    void shouldThrowDuplicateIsbnExceptionWhenIsbnStringExists() {
      // Arrange
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.ensureUnique(EXISTING_ISBN))
          .isInstanceOf(DuplicateIsbnException.class)
          .hasMessage("Book with ISBN already exists: " + EXISTING_ISBN);
    }
  }

  @Nested
  @DisplayName("ensureUniqueExcluding Tests")
  class EnsureUniqueExcludingTests {

    @Test
    @DisplayName("Should not throw exception when ISBN belongs to excluded book")
    void shouldNotThrowExceptionWhenIsbnBelongsToExcludedBook() {
      // Arrange
      var isbn = ISBN.of(EXISTING_ISBN);
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act & Assert
      duplicateIsbnChecker.ensureUniqueExcluding(isbn, 1L);
    }

    @Test
    @DisplayName("Should not throw exception when ISBN is unique")
    void shouldNotThrowExceptionWhenIsbnIsUnique() {
      // Arrange
      var isbn = ISBN.of(NEW_ISBN);
      when(bookRepository.findByIsbn(NEW_ISBN)).thenReturn(Optional.empty());

      // Act & Assert
      duplicateIsbnChecker.ensureUniqueExcluding(isbn, 1L);
    }

    @Test
    @DisplayName("Should throw DuplicateIsbnException when ISBN belongs to different book")
    void shouldThrowDuplicateIsbnExceptionWhenIsbnBelongsToDifferentBook() {
      // Arrange
      var isbn = ISBN.of(EXISTING_ISBN);
      when(bookRepository.findByIsbn(EXISTING_ISBN)).thenReturn(Optional.of(existingBook));

      // Act & Assert
      assertThatThrownBy(() -> duplicateIsbnChecker.ensureUniqueExcluding(isbn, 2L))
          .isInstanceOf(DuplicateIsbnException.class)
          .hasMessage("Book with ISBN already exists: " + EXISTING_ISBN);
    }
  }
}
