package org.gripday.bookstore.catalog;

import java.util.Optional;

import org.gripday.bookstore.inventory.Inventory;
import org.gripday.bookstore.shared.AuditLogger;
import org.gripday.bookstore.shared.BookstoreMetrics;
import org.gripday.bookstore.shared.CacheConfig;
import org.gripday.bookstore.shared.ISBN;
import org.gripday.bookstore.shared.Money;
import org.gripday.bookstore.shared.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Catalog use cases.
 * Orchestrates domain logic, manages transactions, and handles DTO conversions.
 */
@Service
@Transactional
public class CatalogApplicationService {

  private static final Logger logger = LoggerFactory.getLogger(CatalogApplicationService.class);

  private final BookRepository bookRepository;
  private final CategoryRepository categoryRepository;
  private final DuplicateIsbnChecker duplicateIsbnChecker;
  private final AuditLogger auditLogger;
  private final BookstoreMetrics bookstoreMetrics;
  private final BookCatalogResponseBuilder responseBuilder;

  public CatalogApplicationService(
      final BookRepository bookRepository,
      final CategoryRepository categoryRepository,
      final DuplicateIsbnChecker duplicateIsbnChecker,
      final AuditLogger auditLogger,
      final BookstoreMetrics bookstoreMetrics,
      final BookCatalogResponseBuilder responseBuilder) {
    this.bookRepository = bookRepository;
    this.categoryRepository = categoryRepository;
    this.duplicateIsbnChecker = duplicateIsbnChecker;
    this.auditLogger = auditLogger;
    this.bookstoreMetrics = bookstoreMetrics;
    this.responseBuilder = responseBuilder;
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "#query.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> findBooks(BookSearchQuery query, Pageable pageable) {
    logger.debug("Finding books with query: {}", query);

    var timer = bookstoreMetrics.startBookSearchTimer();
    try {
      var books = bookRepository.findBooksWithInventoryFilter(
          query.title(),
          query.author(),
          query.category(),
          query.minPrice(),
          query.maxPrice(),
          query.availableOnly() != null ? query.availableOnly() : false,
          pageable
      );

      bookstoreMetrics.incrementBookSearch();
      return books.map(this::convertToDto);
    } finally {
      bookstoreMetrics.recordBookSearchTime(timer);
    }
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.BOOK_CACHE, key = "#id")
  public Optional<BookDto> findBookById(Long id) {
    logger.debug("Finding book by ID: {}", id);

    return bookRepository.findById(id)
        .map(this::convertToDto);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  @Caching(evict = {
      @CacheEvict(value = CacheConfig.BOOK_SEARCH_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.POPULAR_BOOKS_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.AUTHOR_CACHE, allEntries = true)
  })
  public BookDto createBook(CreateBookCommand command, UserContext userContext) {
    logger.info("Creating book with title: {} by user: {}", command.title(), userContext.username());

    var timer = bookstoreMetrics.startBookCreationTimer();

    // Use domain service to check ISBN uniqueness
    duplicateIsbnChecker.ensureUnique(command.isbn());

    var category = categoryRepository.findById(command.categoryId())
        .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

    // Use factory method to create book with value objects
    var book = Book.create(
        command.title(),
        command.author(),
        command.isbn(),
        command.price(),
        command.description(),
        category
    );

    // Create inventory using factory method
    var inventory = Inventory.create(book, command.initialQuantity());
    book.setInventory(inventory);

    var savedBook = bookRepository.save(book);

    auditLogger.logBookCreation(savedBook.getId(), savedBook.getTitle(), userContext);

    bookstoreMetrics.incrementBookCreated();
    bookstoreMetrics.recordBookCreationTime(timer);

    logger.info("Successfully created book with ID: {} by user: {}", savedBook.getId(), userContext.username());

    return convertToDto(savedBook);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  @Caching(evict = {
      @CacheEvict(value = CacheConfig.BOOK_CACHE, key = "#id"),
      @CacheEvict(value = CacheConfig.BOOK_SEARCH_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.POPULAR_BOOKS_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.AUTHOR_CACHE, allEntries = true)
  })
  public BookDto updateBook(Long id, UpdateBookCommand command, UserContext userContext) {
    logger.info("Updating book ID: {} by user: {}", id, userContext.username());

    var book = bookRepository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));

    var category = categoryRepository.findById(command.categoryId())
        .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

    book.setTitle(command.title());
    book.setAuthor(command.author());
    book.setDescription(command.description());
    book.setPrice(command.price());
    book.setCategory(category);

    var updatedBook = bookRepository.save(book);

    auditLogger.logBookUpdate(updatedBook.getId(), updatedBook.getTitle(), userContext);

    bookstoreMetrics.incrementBookUpdated();

    logger.info("Successfully updated book ID: {} by user: {}", id, userContext.username());

    return convertToDto(updatedBook);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  @Caching(evict = {
      @CacheEvict(value = CacheConfig.BOOK_CACHE, key = "#id"),
      @CacheEvict(value = CacheConfig.BOOK_SEARCH_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.POPULAR_BOOKS_CACHE, allEntries = true)
  })
  public void deleteBook(Long id, UserContext userContext) {
    logger.info("Deleting book ID: {} by user: {}", id, userContext.username());

    var book = bookRepository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));

    book.setAvailable(false);
    bookRepository.save(book);

    auditLogger.logBookDeletion(book.getId(), book.getTitle(), userContext);

    bookstoreMetrics.incrementBookDeleted();

    logger.info("Successfully deleted (marked unavailable) book ID: {} by user: {}", id, userContext.username());
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.BOOK_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).ISBN + #isbn")
  public Optional<BookDto> findBookByIsbn(String isbn) {
    logger.debug("Finding book by ISBN: {}", isbn);

    return bookRepository.findByIsbn(isbn)
        .map(this::convertToDto);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.POPULAR_BOOKS_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).AVAILABLE + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> findAvailableBooks(Pageable pageable) {
    logger.debug("Finding available books");

    return bookRepository.findAvailableBooks(pageable)
        .map(this::convertToDto);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.POPULAR_BOOKS_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).IN_STOCK + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> findBooksInStock(Pageable pageable) {
    logger.debug("Finding books in stock");

    return bookRepository.findBooksInStock(pageable)
        .map(this::convertToDto);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
             key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).CATALOG_RESPONSE + #query.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public BookCatalogResponse findBooksWithCatalogResponse(BookSearchQuery query, Pageable pageable) {
    logger.debug("Finding books with catalog response for query: {}", query);

    var books = findBooks(query, pageable);
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
