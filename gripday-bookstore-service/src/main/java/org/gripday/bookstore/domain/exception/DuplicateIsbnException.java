package org.gripday.bookstore.domain.exception;

public class DuplicateIsbnException extends RuntimeException {

  public DuplicateIsbnException(String isbn) {
    super("Book with ISBN already exists: " + isbn);
  }
}