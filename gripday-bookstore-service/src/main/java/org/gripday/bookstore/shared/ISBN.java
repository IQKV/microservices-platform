package org.gripday.bookstore.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object representing an ISBN (International Standard Book Number).
 * Immutable and self-validating following DDD principles.
 */
@Embeddable
public class ISBN implements Serializable {

  @Column(name = "isbn", unique = true, nullable = false)
  private String value;

  protected ISBN() {
    // For JPA only
  }

  private ISBN(final String value) {
    this.value = normalize(value);
    validate(this.value);
  }

  /**
   * Creates a new ISBN value object.
   *
   * @param value the ISBN string (ISBN-10 or ISBN-13)
   * @return a new ISBN instance
   * @throws IllegalArgumentException if the ISBN is invalid
   */
  public static ISBN of(final String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("ISBN must not be null or blank");
    }
    return new ISBN(value);
  }

  private String normalize(final String isbn) {
    // Remove hyphens and spaces
    return isbn.replaceAll("[\\s-]", "");
  }

  private void validate(final String isbn) {
    if (!isValidISBN10(isbn) && !isValidISBN13(isbn)) {
      throw new IllegalArgumentException("Invalid ISBN format: " + isbn);
    }
  }

  private boolean isValidISBN10(final String isbn) {
    if (isbn.length() != 10) {
      return false;
    }

    try {
      int sum = 0;
      for (int i = 0; i < 9; i++) {
        int digit = Character.getNumericValue(isbn.charAt(i));
        if (digit < 0 || digit > 9) {
          return false;
        }
        sum += digit * (10 - i);
      }

      char lastChar = isbn.charAt(9);
      if (lastChar == 'X') {
        sum += 10;
      } else {
        int digit = Character.getNumericValue(lastChar);
        if (digit < 0 || digit > 9) {
          return false;
        }
        sum += digit;
      }

      return sum % 11 == 0;
    } catch (final Exception e) {
      return false;
    }
  }

  private boolean isValidISBN13(final String isbn) {
    if (isbn.length() != 13) {
      return false;
    }

    try {
      int sum = 0;
      for (int i = 0; i < 12; i++) {
        int digit = Character.getNumericValue(isbn.charAt(i));
        if (digit < 0 || digit > 9) {
          return false;
        }
        sum += digit * (i % 2 == 0 ? 1 : 3);
      }

      int checkDigit = Character.getNumericValue(isbn.charAt(12));
      if (checkDigit < 0 || checkDigit > 9) {
        return false;
      }

      int calculatedCheck = (10 - (sum % 10)) % 10;
      return checkDigit == calculatedCheck;
    } catch (final Exception e) {
      return false;
    }
  }

  public String getValue() {
    return value;
  }

  /**
   * Returns a formatted ISBN with hyphens for readability.
   */
  public String getFormatted() {
    if (value.length() == 10) {
      return String.format("%s-%s-%s-%s",
          value.substring(0, 1),
          value.substring(1, 5),
          value.substring(5, 9),
          value.substring(9, 10));
    } else if (value.length() == 13) {
      return String.format("%s-%s-%s-%s-%s",
          value.substring(0, 3),
          value.substring(3, 4),
          value.substring(4, 9),
          value.substring(9, 12),
          value.substring(12, 13));
    }
    return value;
  }

  public boolean isISBN10() {
    return value.length() == 10;
  }

  public boolean isISBN13() {
    return value.length() == 13;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ISBN isbn = (ISBN) o;
    return Objects.equals(value, isbn.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
