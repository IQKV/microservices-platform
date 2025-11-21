package org.gripday.bookstore.catalog;

import java.util.Optional;

import org.gripday.bookstore.inventory.Inventory;
import org.gripday.bookstore.shared.AuditLogger;
import org.gripday.bookstore.shared.BookstoreMetrics;
import org.gripday.bookstore.shared.CacheConfig;
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

@Service
@Transactional
public class CatalogService {

  private static final Logger logger = LoggerFactory.getLogger(CatalogService.class);

  private final BookRepository bookRepository;
  private final CategoryRepository categoryRepository;
  private final AuditLogger auditLogger;
  private final BookstoreMetrics bookstoreMetrics;
  private final BookCatalogResponseBuilder responseBuilder;

  public CatalogService(final BookRepository bookRepository, final CategoryRepository categoryRepository,
      final AuditLogger auditLogger, final BookstoreMetrics bookstoreMetrics,
      final BookCatalogResponseBuilder responseBuilder) {
    this.bookRepository = bookRepository;
    this.categoryRepository = categoryRepository;
    this.auditLogger = auditLogger;
    this.bookstoreMetrics = bookstoreMetrics;
    this.responseBuilder = responseBuilder;
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE,
      key = "#criteria.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public Page<BookDto> findBooks(BookSearchCriteria criteria, Pageable pageable) {
    logger.debug("Finding books with criteria: {}", criteria);

    var timer = bookstoreMetrics.startBookSearchTimer();
    try {
      var books = bookRepository.findBooksWithInventoryFilter(
          criteria.title(),
          criteria.author(),
          criteria.category(),
          criteria.minPrice(),
          criteria.maxPrice(),
          criteria.availableOnly() != null ? criteria.availableOnly() : false,
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
  public BookDto createBook(CreateBookRequest request, UserContext userContext) {
    logger.info("Creating book with title: {} by user: {}", request.title(), userContext.username());

    var timer = bookstoreMetrics.startBookCreationTimer();

    if (bookRepository.findByIsbn(request.isbn()).isPresent()) {
      throw new DuplicateIsbnException(request.isbn());
    }

    var category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

    var book = new Book();
    book.setTitle(request.title());
    book.setAuthor(request.author());
    book.setIsbn(request.isbn());
    book.setDescription(request.description());
    book.setPrice(request.price());
    book.setCategory(category);
    book.setAvailable(true);

    var inventory = new Inventory(book, request.initialQuantity());
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
  public BookDto updateBook(Long id, UpdateBookRequest request, UserContext userContext) {
    logger.info("Updating book ID: {} by user: {}", id, userContext.username());

    var book = bookRepository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));

    var category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

    book.setTitle(request.title());
    book.setAuthor(request.author());
    book.setDescription(request.description());
    book.setPrice(request.price());
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
  @Cacheable(value = CacheConfig.BOOK_CACHE, key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).ISBN + #isbn")
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
      key = "T(org.gripday.bookstore.shared.BookstoreConstants.CacheKeyPrefixes).CATALOG_RESPONSE + #criteria.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  public BookCatalogResponse findBooksWithCatalogResponse(BookSearchCriteria criteria, Pageable pageable) {
    logger.debug("Finding books with catalog response for criteria: {}", criteria);

    var books = findBooks(criteria, pageable);
    return responseBuilder.build(books, criteria);
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
