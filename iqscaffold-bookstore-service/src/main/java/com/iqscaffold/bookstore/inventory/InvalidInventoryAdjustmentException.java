package com.iqscaffold.bookstore.inventory;

/**
 * Exception thrown when inventory adjustment would result in invalid state.
 */
public class InvalidInventoryAdjustmentException extends RuntimeException {

  private final Long bookId;
  private final int currentQuantity;
  private final int adjustment;
  private final int resultingQuantity;

  public InvalidInventoryAdjustmentException(
      final Long bookId,
      final int currentQuantity,
      final int adjustment) {
    super(String.format(
        "Invalid inventory adjustment for book ID %d: current quantity %d, adjustment %d would result in negative quantity",
        bookId, currentQuantity, adjustment));
    this.bookId = bookId;
    this.currentQuantity = currentQuantity;
    this.adjustment = adjustment;
    this.resultingQuantity = currentQuantity + adjustment;
  }

  public InvalidInventoryAdjustmentException(final String message) {
    super(message);
    this.bookId = null;
    this.currentQuantity = 0;
    this.adjustment = 0;
    this.resultingQuantity = 0;
  }

  public Long getBookId() {
    return bookId;
  }

  public int getCurrentQuantity() {
    return currentQuantity;
  }

  public int getAdjustment() {
    return adjustment;
  }

  public int getResultingQuantity() {
    return resultingQuantity;
  }
}
