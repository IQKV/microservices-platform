package com.iqscaffold.bookstore.catalog;

public class BookNotFoundException extends RuntimeException {

  public BookNotFoundException(final Long bookId) {
    super("Book not found with ID: " + bookId);
  }

  public BookNotFoundException(final String message) {
    super(message);
  }

  public static BookNotFoundException byIsbn(String isbn) {
    return new BookNotFoundException("Book not found with ISBN: " + isbn);
  }
}
