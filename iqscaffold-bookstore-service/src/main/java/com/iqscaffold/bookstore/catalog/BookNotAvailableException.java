package com.iqscaffold.bookstore.catalog;

/**
 * Exception thrown when a book is not available for sale.
 */
public class BookNotAvailableException extends RuntimeException {

  private final Long bookId;

  public BookNotAvailableException(final Long bookId) {
    super(String.format("Book with ID %d is not available for sale", bookId));
    this.bookId = bookId;
  }

  public Long getBookId() {
    return bookId;
  }
}
