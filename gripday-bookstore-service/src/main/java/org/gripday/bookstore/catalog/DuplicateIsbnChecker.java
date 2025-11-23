package org.gripday.bookstore.catalog;

import org.gripday.bookstore.shared.ISBN;
import org.springframework.stereotype.Service;

/**
 * Domain Service for checking ISBN uniqueness.
 * Extracted from CatalogService to follow DDD principles.
 */
@Service
public class DuplicateIsbnChecker {

  private final BookRepository bookRepository;

  public DuplicateIsbnChecker(final BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  /**
   * Checks if an ISBN is already in use by another book.
   *
   * @param isbn the ISBN to check
   * @return true if the ISBN is already used, false otherwise
   */
  public boolean isDuplicate(final ISBN isbn) {
    if (isbn == null) {
      throw new IllegalArgumentException("ISBN must not be null");
    }
    return bookRepository.findByIsbn(isbn.getValue()).isPresent();
  }

  /**
   * Checks if an ISBN is already in use by another book.
   *
   * @param isbn the ISBN string to check
   * @return true if the ISBN is already used, false otherwise
   */
  public boolean isDuplicate(final String isbn) {
    if (isbn == null || isbn.isBlank()) {
      throw new IllegalArgumentException("ISBN must not be null or blank");
    }
    return bookRepository.findByIsbn(isbn).isPresent();
  }

  /**
   * Checks if an ISBN is already in use by a book other than the specified one.
   *
   * @param isbn the ISBN to check
   * @param excludeBookId the book ID to exclude from the check
   * @return true if the ISBN is used by another book, false otherwise
   */
  public boolean isDuplicateExcluding(final ISBN isbn, final Long excludeBookId) {
    if (isbn == null) {
      throw new IllegalArgumentException("ISBN must not be null");
    }
    if (excludeBookId == null) {
      throw new IllegalArgumentException("Book ID must not be null");
    }

    return bookRepository.findByIsbn(isbn.getValue())
        .map(Book::getId)
        .filter(id -> !id.equals(excludeBookId))
        .isPresent();
  }

  /**
   * Validates that an ISBN is not a duplicate and throws an exception if it is.
   *
   * @param isbn the ISBN to validate
   * @throws DuplicateIsbnException if the ISBN is already in use
   */
  public void ensureUnique(final ISBN isbn) {
    if (isDuplicate(isbn)) {
      throw new DuplicateIsbnException(isbn.getValue());
    }
  }

  /**
   * Validates that an ISBN is not a duplicate and throws an exception if it is.
   *
   * @param isbn the ISBN string to validate
   * @throws DuplicateIsbnException if the ISBN is already in use
   */
  public void ensureUnique(final String isbn) {
    if (isDuplicate(isbn)) {
      throw new DuplicateIsbnException(isbn);
    }
  }

  /**
   * Validates that an ISBN is not used by another book (excluding the specified book).
   *
   * @param isbn the ISBN to validate
   * @param excludeBookId the book ID to exclude from the check
   * @throws DuplicateIsbnException if the ISBN is used by another book
   */
  public void ensureUniqueExcluding(final ISBN isbn, final Long excludeBookId) {
    if (isDuplicateExcluding(isbn, excludeBookId)) {
      throw new DuplicateIsbnException(isbn.getValue());
    }
  }
}
