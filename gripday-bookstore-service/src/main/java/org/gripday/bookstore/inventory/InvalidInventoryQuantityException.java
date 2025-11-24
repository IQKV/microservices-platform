package org.gripday.bookstore.inventory;

/**
 * Exception thrown when attempting to set inventory quantity below reserved amount.
 */
public class InvalidInventoryQuantityException extends RuntimeException {

  private final Long bookId;
  private final int requestedQuantity;
  private final int reservedQuantity;

  public InvalidInventoryQuantityException(
      final Long bookId,
      final int requestedQuantity,
      final int reservedQuantity) {
    super(String.format(
        "Cannot set inventory quantity to %d for book ID %d: reserved quantity is %d",
        requestedQuantity, bookId, reservedQuantity));
    this.bookId = bookId;
    this.requestedQuantity = requestedQuantity;
    this.reservedQuantity = reservedQuantity;
  }

  public Long getBookId() {
    return bookId;
  }

  public int getRequestedQuantity() {
    return requestedQuantity;
  }

  public int getReservedQuantity() {
    return reservedQuantity;
  }
}
