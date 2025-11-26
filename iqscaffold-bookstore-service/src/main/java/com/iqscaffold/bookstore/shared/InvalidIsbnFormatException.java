package com.iqscaffold.bookstore.shared;

/**
 * Exception thrown when ISBN format is invalid.
 */
public class InvalidIsbnFormatException extends RuntimeException {

  private final String isbn;

  public InvalidIsbnFormatException(final String isbn) {
    super(String.format("Invalid ISBN format: %s. Must be valid ISBN-10 or ISBN-13", isbn));
    this.isbn = isbn;
  }

  public InvalidIsbnFormatException(final String isbn, final String reason) {
    super(String.format("Invalid ISBN format: %s. Reason: %s", isbn, reason));
    this.isbn = isbn;
  }

  public String getIsbn() {
    return isbn;
  }
}
