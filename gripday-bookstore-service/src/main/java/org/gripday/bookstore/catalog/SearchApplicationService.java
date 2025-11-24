package org.gripday.bookstore.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.gripday.bookstore.shared.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Search use cases.
 * Handles various search operations and query patterns.
 */
@Service
@Transactional(readOnly = true)
public class SearchApplicationService {

  private static final Logger logger = LoggerFactory.getLogger(SearchApplicationService.class);

  private final BookRepository bookRepository;
  private final BookCatalogResponseBuilder responseBuilder;

  public SearchApplicationService(
      final BookRepository bookRepository,
      final BookCatalogResponseBuilder responseBuilder) {
    this.bookRepository = bookRepository;
    this.responseBuilder = responseBuilder;
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).TITLE + #title + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchByTitle(String title, Pageable pageable) {
    logger.debug("Searching books by title: {}", title);

    return bookRepository.findByTitleContainingIgnoreCase(title, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).AUTHOR + #author + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchByAuthor(String author, Pageable pageable) {
    logger.debug("Searching books by author: {}", author);

    return bookRepository.findByAuthorContainingIgnoreCase(author, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).CATEGORY + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchByCategory(String category, Pageable pageable) {
    logger.debug("Searching books by category: {}", category);

    return bookRepository.findByCategoryName(category, pageable)
        .map(this::convertToDto);
  }

  public Page<BookDto> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
    logger.debug("Searching books by price range: {} - {}", minPrice, maxPrice);

    if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
      throw new InvalidPriceRangeException(minPrice, maxPrice);
    }

    return bookRepository.findByPriceBetween(minPrice, maxPrice, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "#query.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> searchWithQuery(BookSearchQuery query, Pageable pageable) {
    logger.debug("Searching books with query: {}", query);

    return bookRepository.findBooksWithCriteria(
        query.title(),
        query.author(),
        query.category(),
        query.minPrice(),
        query.maxPrice(),
        query.availableOnly() != null ? query.availableOnly() : false,
        pageable
    ).map(this::convertToDto);
  }

  public Page<BookDto> searchAvailableBooksWithInventory(BookSearchQuery query, Pageable pageable) {
    logger.debug("Searching available books with inventory filter: {}", query);

    return bookRepository.findBooksWithInventoryFilter(
        query.title(),
        query.author(),
        query.category(),
        query.minPrice(),
        query.maxPrice(),
        query.availableOnly() != null ? query.availableOnly() : true,
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

  @Cacheable(value = CacheConfig.AUTHOR_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).DISTINCT_AUTHORS")
  public List<String> getDistinctAuthors() {
    logger.debug("Getting distinct authors");

    return bookRepository.findDistinctAuthors();
  }

  @Cacheable(value = CacheConfig.CATEGORY_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).DISTINCT_CATEGORIES")
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
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).FULLTEXT + #searchTerm + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> fullTextSearch(String searchTerm, Pageable pageable) {
    logger.debug("Full-text searching books with term: {}", searchTerm);

    return bookRepository.findByFullTextSearch(searchTerm, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).FUZZY + #searchTerm + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> fuzzySearch(String searchTerm, Pageable pageable) {
    logger.debug("Fuzzy searching books with term: {}", searchTerm);

    return bookRepository.findByFuzzySearch(searchTerm, pageable)
        .map(this::convertToDto);
  }

  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).PRICE_RANGE + #minPrice + '_' + #maxPrice + '_' + #categoryId + '_' + #pageable.pageNumber")
  public Page<BookDto> searchByPriceRangeAndCategory(
      BigDecimal minPrice,
      BigDecimal maxPrice,
      Long categoryId,
      Pageable pageable) {
    logger.debug("Searching books by price range: {}-{} and category: {}", minPrice, maxPrice, categoryId);

    if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
      throw new InvalidPriceRangeException(minPrice, maxPrice);
    }

    return bookRepository.findByPriceRangeAndCategory(minPrice, maxPrice, categoryId, pageable)
        .map(this::convertToDto);
  }

  public boolean isBookAvailableWithQuantity(Long bookId, int requestedQuantity) {
    logger.debug("Checking availability for book ID: {} with quantity: {}", bookId, requestedQuantity);

    return bookRepository.isBookAvailableWithQuantity(bookId, requestedQuantity);
  }

  public BookCatalogResponse searchWithCatalogResponse(BookSearchQuery query, Pageable pageable) {
    logger.debug("Searching books with query and building catalog response: {}", query);

    var books = searchWithQuery(query, pageable);
    return responseBuilder.build(books, query);
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
        book.getIsbn().getValue(),
        book.getDescription(),
        book.getPrice().getAmount(),
        categoryName,
        book.isAvailable(),
        availableQuantity,
        book.getCreatedAt(),
        book.getUpdatedAt()
    );
  }
}
