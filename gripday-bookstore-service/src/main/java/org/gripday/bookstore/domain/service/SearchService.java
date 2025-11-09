package org.gripday.bookstore.domain.service;

import java.math.BigDecimal;
import java.util.List;

import org.gripday.bookstore.domain.dto.BookDto;
import org.gripday.bookstore.domain.dto.BookSearchCriteria;
import org.gripday.bookstore.infrastructure.config.CacheConfig;
import org.gripday.bookstore.infrastructure.entity.Book;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SearchService {

  private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

  private final BookRepository bookRepository;

  public SearchService(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "'title_' + #title + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchByTitle(String title, Pageable pageable) {
    logger.debug("Searching books by title: {}", title);

    return bookRepository.findByTitleContainingIgnoreCase(title, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "'author_' + #author + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchByAuthor(String author, Pageable pageable) {
    logger.debug("Searching books by author: {}", author);

    return bookRepository.findByAuthorContainingIgnoreCase(author, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "'category_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchByCategory(String category, Pageable pageable) {
    logger.debug("Searching books by category: {}", category);

    return bookRepository.findByCategoryName(category, pageable)
        .map(this::convertToDto);
  }

  public Page<BookDto> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
    logger.debug("Searching books by price range: {} - {}", minPrice, maxPrice);

    return bookRepository.findByPriceBetween(minPrice, maxPrice, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "#criteria.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchWithCriteria(BookSearchCriteria criteria, Pageable pageable) {
    logger.debug("Searching books with criteria: {}", criteria);

    return bookRepository.findBooksWithCriteria(
        criteria.title(),
        criteria.author(),
        criteria.category(),
        criteria.minPrice(),
        criteria.maxPrice(),
        criteria.availableOnly() != null ? criteria.availableOnly() : false,
        pageable
    ).map(this::convertToDto);
  }

  public Page<BookDto> searchAvailableBooksWithInventory(BookSearchCriteria criteria, Pageable pageable) {
    logger.debug("Searching available books with inventory filter: {}", criteria);

    return bookRepository.findBooksWithInventoryFilter(
        criteria.title(),
        criteria.author(),
        criteria.category(),
        criteria.minPrice(),
        criteria.maxPrice(),
        criteria.availableOnly() != null ? criteria.availableOnly() : true,
        pageable
    ).map(this::convertToDto);
  }

  public Page<BookDto> findAffordableBooks(BigDecimal maxPrice, Pageable pageable) {
    logger.debug("Finding affordable books under: {}", maxPrice);

    return bookRepository.findAffordableBooks(maxPrice, pageable)
        .map(this::convertToDto);
  }

  public Page<BookDto> findRecentBooks(Pageable pageable) {
    logger.debug("Finding recent books");

    return bookRepository.findRecentBooks(pageable)
        .map(this::convertToDto);
  }

  public Page<BookDto> findAvailableBooksByCategory(Long categoryId, Pageable pageable) {
    logger.debug("Finding available books by category ID: {}", categoryId);

    return bookRepository.findAvailableBooksByCategory(categoryId, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.AUTHOR_CACHE, key = "'distinct_authors'")
  public List<String> getDistinctAuthors() {
    logger.debug("Getting distinct authors");

    return bookRepository.findDistinctAuthors();
  }

  @Cacheable(value = CacheConfig.CATEGORY_CACHE, key = "'distinct_categories'")
  public List<String> getDistinctCategories() {
    logger.debug("Getting distinct categories");

    return bookRepository.findDistinctCategoryNames();
  }

  public long countAvailableBooks() {
    logger.debug("Counting available books");

    return bookRepository.countAvailableBooks();
  }

  public long countBooksByCategory(String categoryName) {
    logger.debug("Counting books by category: {}", categoryName);

    return bookRepository.countBooksByCategory(categoryName);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "'fulltext_' + #searchTerm + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> fullTextSearch(String searchTerm, Pageable pageable) {
    logger.debug("Full-text searching books with term: {}", searchTerm);

    return bookRepository.findByFullTextSearch(searchTerm, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "'fuzzy_' + #searchTerm + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> fuzzySearch(String searchTerm, Pageable pageable) {
    logger.debug("Fuzzy searching books with term: {}", searchTerm);

    return bookRepository.findByFuzzySearch(searchTerm, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "'price_range_' + #minPrice + '_' + #maxPrice + '_' + #categoryId + '_' + #pageable.pageNumber")
  public Page<BookDto> searchByPriceRangeAndCategory(BigDecimal minPrice, BigDecimal maxPrice,
      Long categoryId, Pageable pageable) {
    logger.debug("Searching books by price range: {}-{} and category: {}", minPrice, maxPrice, categoryId);

    return bookRepository.findByPriceRangeAndCategory(minPrice, maxPrice, categoryId, pageable)
        .map(this::convertToDto);
  }

  public boolean isBookAvailableWithQuantity(Long bookId, int requestedQuantity) {
    logger.debug("Checking availability for book ID: {} with quantity: {}", bookId, requestedQuantity);

    return bookRepository.isBookAvailableWithQuantity(bookId, requestedQuantity);
  }

  private BookDto convertToDto(Book book) {
    var availableQuantity = 0;
    if (book.getInventory() != null) {
      availableQuantity = book.getInventory().getAvailableQuantity();
    }

    var categoryName = book.getCategory() != null ? book.getCategory().getName() : null;

    return new BookDto(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getIsbn(),
        book.getDescription(),
        book.getPrice(),
        categoryName,
        book.isAvailable(),
        availableQuantity,
        book.getCreatedAt(),
        book.getUpdatedAt()
    );
  }
}