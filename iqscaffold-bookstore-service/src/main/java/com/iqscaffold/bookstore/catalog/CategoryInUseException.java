package com.iqscaffold.bookstore.catalog;

/**
 * Exception thrown when attempting to delete a category that has books associated with it.
 */
public class CategoryInUseException extends RuntimeException {

  private final Long categoryId;
  private final String categoryName;
  private final long bookCount;

  public CategoryInUseException(final Long categoryId, final String categoryName, final long bookCount) {
    super(String.format(
        "Cannot delete category '%s' (ID: %d): it has %d book(s) associated with it",
        categoryName, categoryId, bookCount));
    this.categoryId = categoryId;
    this.categoryName = categoryName;
    this.bookCount = bookCount;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public long getBookCount() {
    return bookCount;
  }
}
