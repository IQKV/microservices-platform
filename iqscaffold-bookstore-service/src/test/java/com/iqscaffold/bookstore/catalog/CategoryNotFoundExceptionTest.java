package com.iqscaffold.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CategoryNotFoundException Tests")
class CategoryNotFoundExceptionTest {

  @Test
  @DisplayName("Should create exception with category ID")
  void shouldCreateExceptionWithCategoryId() {
    // Arrange & Act
    var exception = new CategoryNotFoundException(123L);

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Category not found with ID: 123");
  }

  @Test
  @DisplayName("Should create exception with custom message")
  void shouldCreateExceptionWithCustomMessage() {
    // Arrange & Act
    var exception = new CategoryNotFoundException("Custom error message");

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Custom error message");
  }

  @Test
  @DisplayName("Should create exception by name")
  void shouldCreateExceptionByName() {
    // Arrange & Act
    var exception = CategoryNotFoundException.byName("Fiction");

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Category not found with name: Fiction");
  }
}
