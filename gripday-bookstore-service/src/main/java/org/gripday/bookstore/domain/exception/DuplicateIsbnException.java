package org.gripday.bookstore.domain.exception;

public class DuplicateIsbnException extends RuntimeException {

  public DuplicateIsbnException(final String isbn) {
    super("Book with ISBN already exists: " + isbn);
  }
}