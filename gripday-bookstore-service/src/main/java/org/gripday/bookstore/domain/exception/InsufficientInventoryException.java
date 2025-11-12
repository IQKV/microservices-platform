package org.gripday.bookstore.domain.exception;

public class InsufficientInventoryException extends RuntimeException {

  public InsufficientInventoryException(final Long bookId, final int requested, final int available) {
    super("Insufficient inventory for book ID: " + bookId +
        ". Requested: " + requested + ", Available: " + available);
  }

  public InsufficientInventoryException(final String message) {
    super(message);
  }
}