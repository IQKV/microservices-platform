package org.gripday.bookstore.catalog;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.gripday.bookstore.inventory.Inventory;
import org.gripday.bookstore.shared.AggregateRoot;
import org.gripday.bookstore.shared.ISBN;
import org.gripday.bookstore.shared.Money;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "books")
public class Book extends AggregateRoot<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String author;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "isbn", unique = true, nullable = false))
  private ISBN isbn;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Embedded
  @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false, precision = 10, scale = 2))
  @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
  private Money price;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @OneToOne(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Inventory inventory;

  @Column(nullable = false)
  private boolean available = true;

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  protected Book() {
    // For JPA only
  }

  private Book(
      final String title,
      final String author,
      final ISBN isbn,
      final Money price,
      final String description,
      final Category category) {
    validateTitle(title);
    validateAuthor(author);

    this.title = title;
    this.author = author;
    this.isbn = isbn;
    this.price = price;
    this.description = description;
    this.category = category;
    this.available = true;
  }

  /**
   * Factory method to create a new Book following DDD principles.
   * Encapsulates creation logic and ensures invariants are maintained.
   *
   * @param title the book title (required, must not be blank)
   * @param author the book author (required, must not be blank)
   * @param isbn the book ISBN (required, must be valid ISBN-10 or ISBN-13)
   * @param price the book price (required, must be positive)
   * @param description the book description (optional)
   * @param category the book category (optional)
   * @return a new Book instance
   * @throws IllegalArgumentException if any required field is invalid
   */
  public static Book create(
      final String title,
      final String author,
      final String isbn,
      final BigDecimal price,
      final String description,
      final Category category) {
    return new Book(
        title,
        author,
        ISBN.of(isbn),
        Money.usd(price),
        description,
        category
    );
  }

  /**
   * Factory method to create a new Book with value objects.
   *
   * @param title the book title (required, must not be blank)
   * @param author the book author (required, must not be blank)
   * @param isbn the ISBN value object
   * @param price the Money value object
   * @param description the book description (optional)
   * @param category the book category (optional)
   * @return a new Book instance
   * @throws IllegalArgumentException if any required field is invalid
   */
  public static Book create(
      final String title,
      final String author,
      final ISBN isbn,
      final Money price,
      final String description,
      final Category category) {
    return new Book(title, author, isbn, price, description, category);
  }

  private void validateTitle(final String title) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Book title must not be null or blank");
    }
  }

  private void validateAuthor(final String author) {
    if (author == null || author.isBlank()) {
      throw new IllegalArgumentException("Book author must not be null or blank");
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public ISBN getIsbn() {
    return isbn;
  }

  public void setIsbn(String isbn) {
    this.isbn = ISBN.of(isbn);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Money getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = Money.usd(price);
  }

  public void setPrice(Money price) {
    this.price = price;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public Inventory getInventory() {
    return inventory;
  }

  public void setInventory(Inventory inventory) {
    this.inventory = inventory;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
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

  // ========================================
  // Business Methods
  // ========================================

  /**
   * Updates the book details with validation.
   * Business method that encapsulates the logic for updating book information.
   *
   * @param title the new title
   * @param author the new author
   * @param description the new description
   * @param price the new price
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public void updateDetails(
      final String title,
      final String author,
      final String description,
      final Money price) {
    validateTitle(title);
    validateAuthor(author);

    if (price == null) {
      throw new IllegalArgumentException("Price must not be null");
    }

    this.title = title;
    this.author = author;
    this.description = description;
    this.price = price;
  }

  /**
   * Updates the book details with BigDecimal price (convenience method).
   *
   * @param title the new title
   * @param author the new author
   * @param description the new description
   * @param price the new price as BigDecimal
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public void updateDetails(
      final String title,
      final String author,
      final String description,
      final BigDecimal price) {
    updateDetails(title, author, description, Money.usd(price));
  }

  /**
   * Changes the category of this book.
   * Business method that encapsulates category change logic.
   *
   * @param newCategory the new category (can be null)
   */
  public void changeCategory(final Category newCategory) {
    if (this.category != null && this.category.equals(newCategory)) {
      return; // No change needed
    }

    // Remove from old category if exists
    if (this.category != null) {
      this.category.removeBook(this);
    }

    // Add to new category if exists
    this.category = newCategory;
    if (newCategory != null) {
      newCategory.addBook(this);
    }
  }

  /**
   * Marks this book as unavailable for sale.
   * Business method that encapsulates the logic for making a book unavailable.
   */
  public void markAsUnavailable() {
    this.available = false;
  }

  /**
   * Marks this book as available for sale.
   * Business method that encapsulates the logic for making a book available.
   */
  public void markAsAvailable() {
    this.available = true;
  }

  /**
   * Checks if this book's price is within the specified range.
   *
   * @param minPrice the minimum price (inclusive)
   * @param maxPrice the maximum price (inclusive)
   * @return true if the book price is within the range, false otherwise
   * @throws IllegalArgumentException if minPrice or maxPrice is null
   */
  public boolean isInPriceRange(final Money minPrice, final Money maxPrice) {
    if (minPrice == null || maxPrice == null) {
      throw new IllegalArgumentException("Price range bounds must not be null");
    }

    return !this.price.isLessThan(minPrice) && !this.price.isGreaterThan(maxPrice);
  }

  /**
   * Checks if this book's price is within the specified range (convenience method).
   *
   * @param minPrice the minimum price (inclusive)
   * @param maxPrice the maximum price (inclusive)
   * @return true if the book price is within the range, false otherwise
   */
  public boolean isInPriceRange(final BigDecimal minPrice, final BigDecimal maxPrice) {
    if (minPrice == null || maxPrice == null) {
      throw new IllegalArgumentException("Price range bounds must not be null");
    }

    return isInPriceRange(Money.usd(minPrice), Money.usd(maxPrice));
  }

  /**
   * Checks if this book is affordable (price is less than or equal to the specified amount).
   *
   * @param maxPrice the maximum affordable price
   * @return true if the book is affordable, false otherwise
   */
  public boolean isAffordable(final Money maxPrice) {
    if (maxPrice == null) {
      throw new IllegalArgumentException("Max price must not be null");
    }

    return !this.price.isGreaterThan(maxPrice);
  }

  /**
   * Checks if this book has inventory associated with it.
   *
   * @return true if inventory exists, false otherwise
   */
  public boolean hasInventory() {
    return this.inventory != null;
  }

  /**
   * Checks if this book is in stock (has inventory with available quantity).
   *
   * @return true if the book is in stock, false otherwise
   */
  public boolean isInStock() {
    return hasInventory() && this.inventory.isAvailable();
  }

  /**
   * Checks if this book can be sold (is available and in stock).
   *
   * @return true if the book can be sold, false otherwise
   */
  public boolean canBeSold() {
    return this.available && isInStock();
  }

  /**
   * Gets the available quantity from inventory.
   *
   * @return the available quantity, or 0 if no inventory exists
   */
  public int getAvailableQuantity() {
    return hasInventory() ? this.inventory.getAvailableQuantity() : 0;
  }
}
