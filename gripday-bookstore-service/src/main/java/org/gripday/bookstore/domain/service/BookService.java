package org.gripday.bookstore.domain.service;

import org.gripday.bookstore.domain.dto.*;
import org.gripday.bookstore.domain.exception.*;
import org.gripday.bookstore.infrastructure.config.CacheConfiguration;
import org.gripday.bookstore.infrastructure.entity.Book;
import org.gripday.bookstore.infrastructure.entity.Category;
import org.gripday.bookstore.infrastructure.entity.Inventory;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.gripday.bookstore.infrastructure.repository.CategoryRepository;
import org.gripday.bookstore.infrastructure.security.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class BookService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookService.class);
    
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogger auditLogger;
    
    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository, 
                      AuditLogger auditLogger) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogger = auditLogger;
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.BOOK_SEARCH_CACHE, 
               key = "#criteria.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<BookDto> findBooks(BookSearchCriteria criteria, Pageable pageable) {
        logger.debug("Finding books with criteria: {}", criteria);
        
        var books = bookRepository.findBooksWithInventoryFilter(
            criteria.title(),
            criteria.author(),
            criteria.category(),
            criteria.minPrice(),
            criteria.maxPrice(),
            criteria.availableOnly() != null ? criteria.availableOnly() : false,
            pageable
        );
        
        return books.map(this::convertToDto);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.BOOK_CACHE, key = "#id")
    public Optional<BookDto> findBookById(Long id) {
        logger.debug("Finding book by ID: {}", id);
        
        return bookRepository.findById(id)
            .map(this::convertToDto);
    }
    
    @Caching(evict = {
        @CacheEvict(value = CacheConfiguration.BOOK_SEARCH_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfiguration.POPULAR_BOOKS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfiguration.AUTHOR_CACHE, allEntries = true)
    })
    public BookDto createBook(CreateBookRequest request, UserContext userContext) {
        logger.info("Creating book with title: {} by user: {}", request.title(), userContext.username());
        
        // Authorization check
        if (!userContext.isAdmin()) {
            auditLogger.logUnauthorizedAccess("create book", "BOOK", userContext);
            throw new UnauthorizedOperationException("create book", "ADMIN or SUPERADMIN");
        }
        
        // Validate ISBN uniqueness
        if (bookRepository.findByIsbn(request.isbn()).isPresent()) {
            throw new DuplicateIsbnException(request.isbn());
        }
        
        // Validate category exists
        var category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
        
        // Create book entity
        var book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setDescription(request.description());
        book.setPrice(request.price());
        book.setCategory(category);
        book.setAvailable(true);
        
        // Create initial inventory
        var inventory = new Inventory(book, request.initialQuantity());
        book.setInventory(inventory);
        
        var savedBook = bookRepository.save(book);
        
        // Audit log the creation
        auditLogger.logBookCreation(savedBook.getId(), savedBook.getTitle(), userContext);
        
        logger.info("Successfully created book with ID: {} by user: {}", savedBook.getId(), userContext.username());
        
        return convertToDto(savedBook);
    }
    
    @Caching(evict = {
        @CacheEvict(value = CacheConfiguration.BOOK_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfiguration.BOOK_SEARCH_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfiguration.POPULAR_BOOKS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfiguration.AUTHOR_CACHE, allEntries = true)
    })
    public BookDto updateBook(Long id, UpdateBookRequest request, UserContext userContext) {
        logger.info("Updating book ID: {} by user: {}", id, userContext.username());
        
        // Authorization check
        if (!userContext.isAdmin()) {
            auditLogger.logUnauthorizedAccess("update book", "BOOK", userContext);
            throw new UnauthorizedOperationException("update book", "ADMIN or SUPERADMIN");
        }
        
        var book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));
        
        // Validate category exists
        var category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
        
        // Update book fields
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setDescription(request.description());
        book.setPrice(request.price());
        book.setCategory(category);
        
        var updatedBook = bookRepository.save(book);
        
        // Audit log the update
        auditLogger.logBookUpdate(updatedBook.getId(), updatedBook.getTitle(), userContext);
        
        logger.info("Successfully updated book ID: {} by user: {}", id, userContext.username());
        
        return convertToDto(updatedBook);
    }
    
    @Caching(evict = {
        @CacheEvict(value = CacheConfiguration.BOOK_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfiguration.BOOK_SEARCH_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfiguration.POPULAR_BOOKS_CACHE, allEntries = true)
    })
    public void deleteBook(Long id, UserContext userContext) {
        logger.info("Deleting book ID: {} by user: {}", id, userContext.username());
        
        // Authorization check
        if (!userContext.isAdmin()) {
            auditLogger.logUnauthorizedAccess("delete book", "BOOK", userContext);
            throw new UnauthorizedOperationException("delete book", "ADMIN or SUPERADMIN");
        }
        
        var book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));
        
        // Soft delete by marking as unavailable
        book.setAvailable(false);
        bookRepository.save(book);
        
        // Audit log the deletion
        auditLogger.logBookDeletion(book.getId(), book.getTitle(), userContext);
        
        logger.info("Successfully deleted (marked unavailable) book ID: {} by user: {}", id, userContext.username());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.BOOK_CACHE, key = "'isbn_' + #isbn")
    public Optional<BookDto> findBookByIsbn(String isbn) {
        logger.debug("Finding book by ISBN: {}", isbn);
        
        return bookRepository.findByIsbn(isbn)
            .map(this::convertToDto);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.POPULAR_BOOKS_CACHE, 
               key = "'available_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<BookDto> findAvailableBooks(Pageable pageable) {
        logger.debug("Finding available books");
        
        return bookRepository.findAvailableBooks(pageable)
            .map(this::convertToDto);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.POPULAR_BOOKS_CACHE, 
               key = "'in_stock_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<BookDto> findBooksInStock(Pageable pageable) {
        logger.debug("Finding books in stock");
        
        return bookRepository.findBooksInStock(pageable)
            .map(this::convertToDto);
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