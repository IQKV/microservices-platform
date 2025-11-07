package org.gripday.bookstore.domain.exception;

public class InsufficientInventoryException extends RuntimeException {

  public InsufficientInventoryException(Long bookId, int requested, int available) {
    super("Insufficient inventory for book ID: " + bookId +
        ". Requested: " + requested + ", Available: " + available);
  }

  public InsufficientInventoryException(String message) {
    super(message);
  }
}