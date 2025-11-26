package com.iqscaffold.bookstore.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "categories")
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Book> books = new ArrayList<>();

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  protected Category() {
    // For JPA only
  }

  private Category(final String name, final String description) {
    validateName(name);
    this.name = name;
    this.description = description;
  }

  /**
   * Factory method to create a new Category following DDD principles.
   * Encapsulates creation logic and ensures invariants are maintained.
   *
   * @param name        the category name (required, must not be blank)
   * @param description the category description (optional)
   * @return a new Category instance
   * @throws IllegalArgumentException if name is null or blank
   */
  public static Category create(final String name, final String description) {
    return new Category(name, description);
  }

  private void validateName(final String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Category name must not be null or blank");
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Book> getBooks() {
    return books;
  }

  public void setBooks(List<Book> books) {
    this.books = books;
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

  public void addBook(Book book) {
    books.add(book);
    book.setCategory(this);
  }

  public void removeBook(Book book) {
    books.remove(book);
    book.setCategory(null);
  }
}
