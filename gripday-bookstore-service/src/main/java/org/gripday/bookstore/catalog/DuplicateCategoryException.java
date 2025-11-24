package org.gripday.bookstore.catalog;

/**
 * Exception thrown when attempting to create a category with a name that already exists.
 */
public class DuplicateCategoryException extends RuntimeException {

  private final String categoryName;

  public DuplicateCategoryException(final String categoryName) {
    super(String.format("Category with name '%s' already exists", categoryName));
    this.categoryName = categoryName;
  }

  public String getCategoryName() {
    return categoryName;
  }
}
