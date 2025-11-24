package org.gripday.bookstore.catalog;

import org.gripday.bookstore.inventory.InventoryRepository;
import org.springframework.stereotype.Service;

/**
 * Domain Service for checking book availability.
 * Coordinates between catalog and inventory contexts following DDD principles.
 */
@Service
public class BookAvailabilityChecker {

  private final BookRepository bookRepository;
  private final InventoryRepository inventoryRepository;

  public BookAvailabilityChecker(
      final BookRepository bookRepository,
      final InventoryRepository inventoryRepository) {
    this.bookRepository = bookRepository;
    this.inventoryRepository = inventoryRepository;
  }

  /**
   * Checks if a book can be sold based on availability flag and inventory.
   *
   * @param bookId the book ID to check
   * @return true if the book can be sold, false otherwise
   */
  public boolean canBeSold(final Long bookId) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }

    var book = bookRepository.findById(bookId);
    if (book.isEmpty() || !book.get().isAvailable()) {
      return false;
    }

    var inventory = inventoryRepository.findByBookId(bookId);
    return inventory.isPresent() && inventory.get().isAvailable();
  }

  /**
   * Checks if a specific quantity of a book can be sold.
   *
   * @param bookId   the book ID to check
   * @param quantity the quantity requested
   * @return true if the requested quantity can be sold, false otherwise
   */
  public boolean canBeSold(final Long bookId, final int quantity) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }

    var book = bookRepository.findById(bookId);
    if (book.isEmpty() || !book.get().isAvailable()) {
      return false;
    }

    var inventory = inventoryRepository.findByBookId(bookId);
    return inventory.isPresent() && inventory.get().canReserve(quantity);
  }

  /**
   * Checks if a book is available for sale (marked as available in catalog).
   *
   * @param bookId the book ID to check
   * @return true if the book is marked as available, false otherwise
   */
  public boolean isAvailableInCatalog(final Long bookId) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }

    return bookRepository.findById(bookId)
        .map(Book::isAvailable)
        .orElse(false);
  }

  /**
   * Checks if a book has stock available in inventory.
   *
   * @param bookId the book ID to check
   * @return true if the book has stock, false otherwise
   */
  public boolean hasStock(final Long bookId) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }

    return inventoryRepository.findByBookId(bookId)
        .map(inventory -> inventory.getAvailableQuantity() > 0)
        .orElse(false);
  }

  /**
   * Gets the available quantity for a book.
   *
   * @param bookId the book ID to check
   * @return the available quantity, or 0 if not found or unavailable
   */
  public int getAvailableQuantity(final Long bookId) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }

    if (!isAvailableInCatalog(bookId)) {
      return 0;
    }

    return inventoryRepository.findByBookId(bookId)
        .map(inventory -> inventory.getAvailableQuantity())
        .orElse(0);
  }

  /**
   * Checks if a book is in low stock.
   *
   * @param bookId the book ID to check
   * @return true if the book is in low stock, false otherwise
   */
  public boolean isLowStock(final Long bookId) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }

    return inventoryRepository.findByBookId(bookId)
        .map(inventory -> inventory.isLowStock())
        .orElse(false);
  }

  /**
   * Checks if a book is out of stock.
   *
   * @param bookId the book ID to check
   * @return true if the book is out of stock, false otherwise
   */
  public boolean isOutOfStock(final Long bookId) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }

    return inventoryRepository.findByBookId(bookId)
        .map(inventory -> !inventory.isAvailable())
        .orElse(true);
  }

  /**
   * Validates that a book can be sold and throws an exception if it cannot.
   *
   * @param bookId the book ID to validate
   * @throws BookNotAvailableException if the book cannot be sold
   */
  public void ensureCanBeSold(final Long bookId) {
    if (!canBeSold(bookId)) {
      throw new BookNotAvailableException(bookId);
    }
  }

  /**
   * Validates that a specific quantity of a book can be sold.
   *
   * @param bookId   the book ID to validate
   * @param quantity the quantity requested
   * @throws BookNotAvailableException  if the book cannot be sold
   * @throws InsufficientStockException if there is not enough stock
   */
  public void ensureCanBeSold(final Long bookId, final int quantity) {
    if (!isAvailableInCatalog(bookId)) {
      throw new BookNotAvailableException(bookId);
    }

    if (!hasStock(bookId)) {
      throw new InsufficientStockException(bookId, quantity, 0);
    }

    var availableQuantity = getAvailableQuantity(bookId);
    if (availableQuantity < quantity) {
      throw new InsufficientStockException(bookId, quantity, availableQuantity);
    }
  }
}
