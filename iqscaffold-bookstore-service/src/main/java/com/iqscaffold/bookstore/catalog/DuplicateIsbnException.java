package com.iqscaffold.bookstore.catalog;

public class DuplicateIsbnException extends RuntimeException {

  public DuplicateIsbnException(final String isbn) {
    super("Book with ISBN already exists: " + isbn);
  }
}
