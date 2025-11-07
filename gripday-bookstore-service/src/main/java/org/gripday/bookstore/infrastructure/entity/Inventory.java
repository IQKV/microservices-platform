package org.gripday.bookstore.infrastructure.entity;

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

  // Default constructor
  public Inventory() {
  }

  // Constructor with required fields
  public Inventory(Book book, int quantity) {
    this.book = book;
    this.quantity = quantity;
  }

  // Constructor with all fields
  public Inventory(Book book, int quantity, int lowStockThreshold) {
    this.book = book;
    this.quantity = quantity;
    this.lowStockThreshold = lowStockThreshold;
  }

  // Getters and setters
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

  // Business logic methods
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
      throw new IllegalArgumentException("Insufficient inventory to reserve " + quantityToReserve + " items");
    }
    this.reservedQuantity += quantityToReserve;
  }

  public void releaseReservedQuantity(int quantityToRelease) {
    if (quantityToRelease > this.reservedQuantity) {
      throw new IllegalArgumentException("Cannot release more than reserved quantity");
    }
    this.reservedQuantity -= quantityToRelease;
  }

  public void adjustQuantity(int adjustment) {
    var newQuantity = this.quantity + adjustment;
    if (newQuantity < 0) {
      throw new IllegalArgumentException("Inventory quantity cannot be negative");
    }
    this.quantity = newQuantity;
  }
}