package org.gripday.bookstore.catalog;

public class CategoryNotFoundException extends RuntimeException {

  public CategoryNotFoundException(final Long categoryId) {
    super("Category not found with ID: " + categoryId);
  }

  public CategoryNotFoundException(final String message) {
    super(message);
  }

  public static CategoryNotFoundException byName(String categoryName) {
    return new CategoryNotFoundException("Category not found with name: " + categoryName);
  }
}
