package org.gripday.bookstore.domain.exception;

public class BookNotFoundException extends RuntimeException {

  public BookNotFoundException(Long bookId) {
    super("Book not found with ID: " + bookId);
  }

  public BookNotFoundException(String message) {
    super(message);
  }

  public static BookNotFoundException byIsbn(String isbn) {
    return new BookNotFoundException("Book not found with ISBN: " + isbn);
  }
}