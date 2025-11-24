package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CategoryDto Tests")
class CategoryDtoTest {

  @Test
  @DisplayName("Should create CategoryDto from Category entity")
  void shouldCreateCategoryDtoFromCategoryEntity() {
    // Arrange
    var category = Category.create("Fiction", "Fiction books and novels");
    category.setId(1L);
    category.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
    category.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 14, 30));

    // Act
    var dto = CategoryDto.from(category);

    // Assert
    assertThat(dto.id()).isEqualTo(1L);
    assertThat(dto.name()).isEqualTo("Fiction");
    assertThat(dto.description()).isEqualTo("Fiction books and novels");
    assertThat(dto.bookCount()).isZero();
    assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
    assertThat(dto.updatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 14, 30));
  }

  @Test
  @DisplayName("Should handle null books list")
  void shouldHandleNullBooksList() {
    // Arrange
    var category = Category.create("Science", "Science books");
    category.setBooks(null);

    // Act
    var dto = CategoryDto.from(category);

    // Assert
    assertThat(dto.bookCount()).isZero();
  }

  @Test
  @DisplayName("Should count books correctly")
  void shouldCountBooksCorrectly() {
    // Arrange
    var category = Category.create("History", "History books");
    var books = new ArrayList<Book>();
    books.add(new Book());
    books.add(new Book());
    books.add(new Book());
    category.setBooks(books);

    // Act
    var dto = CategoryDto.from(category);

    // Assert
    assertThat(dto.bookCount()).isEqualTo(3);
  }
}
