package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CategoryInUseException Tests")
class CategoryInUseExceptionTest {

  @Test
  @DisplayName("Should create exception with category details")
  void shouldCreateExceptionWithCategoryDetails() {
    // Arrange & Act
    var exception = new CategoryInUseException(1L, "Fiction", 5);

    // Assert
    assertThat(exception.getMessage())
        .contains("Cannot delete category 'Fiction'")
        .contains("ID: 1")
        .contains("5 book(s) associated");
    assertThat(exception.getCategoryId()).isEqualTo(1L);
    assertThat(exception.getCategoryName()).isEqualTo("Fiction");
    assertThat(exception.getBookCount()).isEqualTo(5);
  }

  @Test
  @DisplayName("Should be a RuntimeException")
  void shouldBeRuntimeException() {
    // Arrange & Act
    var exception = new CategoryInUseException(1L, "Fiction", 5);

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
