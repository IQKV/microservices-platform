package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DuplicateCategoryException Tests")
class DuplicateCategoryExceptionTest {

  @Test
  @DisplayName("Should create exception with category name")
  void shouldCreateExceptionWithCategoryName() {
    // Arrange & Act
    var exception = new DuplicateCategoryException("Fiction");

    // Assert
    assertThat(exception.getMessage())
        .contains("Category with name 'Fiction' already exists");
    assertThat(exception.getCategoryName()).isEqualTo("Fiction");
  }

  @Test
  @DisplayName("Should be a RuntimeException")
  void shouldBeRuntimeException() {
    // Arrange & Act
    var exception = new DuplicateCategoryException("Fiction");

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
