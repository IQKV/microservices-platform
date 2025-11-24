package org.gripday.bookstore.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import org.gripday.bookstore.catalog.Book;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "inventory")
public class Inventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(nullable = false)
  private int quantity = 0;

  @Column(nullable = false)
  private int reservedQuantity = 0;

  @Column(nullable = false)
  private int lowStockThreshold = 5;

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  protected Inventory() {
    // For JPA only
  }

  private Inventory(final Book book, final int quantity, final int lowStockThreshold) {
    validateBook(book);
    validateQuantity(quantity);
    validateLowStockThreshold(lowStockThreshold);

    this.book = book;
    this.quantity = quantity;
    this.lowStockThreshold = lowStockThreshold;
    this.reservedQuantity = 0;
  }

  /**
   * Factory method to create a new Inventory following DDD principles.
   * Encapsulates creation logic and ensures invariants are maintained.
   *
   * @param book              the book for this inventory (required, must not be null)
   * @param quantity          the initial quantity (required, must not be negative)
   * @param lowStockThreshold the low stock threshold (optional, defaults to 5 if not provided)
   * @return a new Inventory instance
   * @throws IllegalArgumentException if any required field is invalid
   */
  public static Inventory create(final Book book, final int quantity, final Integer lowStockThreshold) {
    return new Inventory(book, quantity, lowStockThreshold != null ? lowStockThreshold : 5);
  }

  /**
   * Factory method to create a new Inventory with default low stock threshold.
   *
   * @param book     the book for this inventory (required, must not be null)
   * @param quantity the initial quantity (required, must not be negative)
   * @return a new Inventory instance with default low stock threshold of 5
   * @throws IllegalArgumentException if any required field is invalid
   */
  public static Inventory create(final Book book, final int quantity) {
    return create(book, quantity, null);
  }

  private void validateBook(final Book book) {
    if (book == null) {
      throw new IllegalArgumentException("Book must not be null");
    }
  }

  private void validateQuantity(final int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("Inventory quantity must not be negative");
    }
  }

  private void validateLowStockThreshold(final int lowStockThreshold) {
    if (lowStockThreshold < 0) {
      throw new IllegalArgumentException("Low stock threshold must not be negative");
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Book getBook() {
    return book;
  }

  public void setBook(Book book) {
    this.book = book;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public int getReservedQuantity() {
    return reservedQuantity;
  }

  public void setReservedQuantity(int reservedQuantity) {
    this.reservedQuantity = reservedQuantity;
  }

  public int getLowStockThreshold() {
    return lowStockThreshold;
  }

  public void setLowStockThreshold(int lowStockThreshold) {
    this.lowStockThreshold = lowStockThreshold;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public int getAvailableQuantity() {
    return Math.max(0, quantity - reservedQuantity);
  }

  public boolean isLowStock() {
    return getAvailableQuantity() <= lowStockThreshold;
  }

  public boolean isAvailable() {
    return getAvailableQuantity() > 0;
  }

  public boolean canReserve(int requestedQuantity) {
    return getAvailableQuantity() >= requestedQuantity;
  }

  public void reserveQuantity(int quantityToReserve) {
    if (!canReserve(quantityToReserve)) {
      throw new InsufficientInventoryException(
          this.book != null ? this.book.getId() : null,
          quantityToReserve,
          getAvailableQuantity());
    }
    this.reservedQuantity += quantityToReserve;
  }

  public void releaseReservedQuantity(int quantityToRelease) {
    if (quantityToRelease > this.reservedQuantity) {
      throw new InvalidInventoryAdjustmentException(
          "Cannot release more than reserved quantity: requested " + quantityToRelease +
          ", reserved " + this.reservedQuantity);
    }
    this.reservedQuantity -= quantityToRelease;
  }

  public void adjustQuantity(int adjustment) {
    var newQuantity = this.quantity + adjustment;
    if (newQuantity < 0) {
      throw new InvalidInventoryAdjustmentException(
          this.book != null ? this.book.getId() : null,
          this.quantity,
          adjustment);
    }
    this.quantity = newQuantity;
  }
}
