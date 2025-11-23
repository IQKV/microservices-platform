package org.gripday.bookstore.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object representing a Book identifier.
 * Immutable and self-validating following DDD principles.
 */
@Embeddable
public class BookId implements Serializable {

  @Column(name = "book_id", nullable = false)
  private Long value;

  protected BookId() {
    // For JPA only
  }

  private BookId(final Long value) {
    validate(value);
    this.value = value;
  }

  /**
   * Creates a new BookId value object.
   *
   * @param value the book identifier
   * @return a new BookId instance
   * @throws IllegalArgumentException if the value is null or non-positive
   */
  public static BookId of(final Long value) {
    return new BookId(value);
  }

  private void validate(final Long value) {
    if (value == null) {
      throw new IllegalArgumentException("BookId must not be null");
    }
    if (value <= 0) {
      throw new IllegalArgumentException("BookId must be positive");
    }
  }

  public Long getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookId bookId = (BookId) o;
    return Objects.equals(value, bookId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
