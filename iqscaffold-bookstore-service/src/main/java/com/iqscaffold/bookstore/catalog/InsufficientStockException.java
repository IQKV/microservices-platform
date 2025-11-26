package com.iqscaffold.bookstore.catalog;

/**
 * Exception thrown when there is insufficient stock to fulfill a request.
 */
public class InsufficientStockException extends RuntimeException {

  private final Long bookId;
  private final int requestedQuantity;
  private final int availableQuantity;

  public InsufficientStockException(
      final Long bookId,
      final int requestedQuantity,
      final int availableQuantity) {
    super(String.format(
        "Insufficient stock for book ID %d: requested %d, available %d",
        bookId, requestedQuantity, availableQuantity));
    this.bookId = bookId;
    this.requestedQuantity = requestedQuantity;
    this.availableQuantity = availableQuantity;
  }

  public Long getBookId() {
    return bookId;
  }

  public int getRequestedQuantity() {
    return requestedQuantity;
  }

  public int getAvailableQuantity() {
    return availableQuantity;
  }
}
