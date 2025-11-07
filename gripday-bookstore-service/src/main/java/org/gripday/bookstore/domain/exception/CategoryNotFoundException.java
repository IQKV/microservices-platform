package org.gripday.bookstore.domain.exception;

public class CategoryNotFoundException extends RuntimeException {

  public CategoryNotFoundException(Long categoryId) {
    super("Category not found with ID: " + categoryId);
  }

  public CategoryNotFoundException(String message) {
    super(message);
  }

  public static CategoryNotFoundException byName(String categoryName) {
    return new CategoryNotFoundException("Category not found with name: " + categoryName);
  }
}