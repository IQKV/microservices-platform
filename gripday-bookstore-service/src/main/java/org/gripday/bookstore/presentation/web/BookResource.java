package org.gripday.bookstore.presentation.web;

import jakarta.validation.Valid;
import org.gripday.bookstore.domain.dto.*;
import org.gripday.bookstore.domain.service.BookService;
import org.gripday.bookstore.domain.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/bookstore/books")
public class BookResource {
    
    private static final Logger logger = LoggerFactory.getLogger(BookResource.class);
    
    private final BookService bookService;
    private final SearchService searchService;
    
    public BookResource(BookService bookService, SearchService searchService) {
        this.bookService = bookService;
        this.searchService = searchService;
    }
    
    @GetMapping
    public ResponseEntity<Page<BookDto>> getBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean availableOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Getting books with filters - title: {}, author: {}, category: {}", title, author, category);
        
        var criteria = new BookSearchCriteria(title, author, category, minPrice, maxPrice, availableOnly);
        var books = bookService.findBooks(criteria, pageable);
        
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        logger.debug("Getting book by ID: {}", id);
        
        return bookService.findBookById(id)
            .map(book -> ResponseEntity.ok(book))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDto> getBookByIsbn(@PathVariable String isbn) {
        logger.debug("Getting book by ISBN: {}", isbn);
        
        return bookService.findBookByIsbn(isbn)
            .map(book -> ResponseEntity.ok(book))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/available")
    public ResponseEntity<Page<BookDto>> getAvailableBooks(@PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Getting available books");
        
        var books = bookService.findAvailableBooks(pageable);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/in-stock")
    public ResponseEntity<Page<BookDto>> getBooksInStock(@PageableDefault(size = 20) Pageable pageable) {
        logger.debug("Getting books in stock");
        
        var books = bookService.findBooksInStock(pageable);
        return ResponseEntity.ok(books);
    }
    
    @PostMapping
    public ResponseEntity<BookDto> createBook(
            @Valid @RequestBody CreateBookRequest request,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Creating book with title: {} by user: {}", request.title(), userContext.username());
        
        var createdBook = bookService.createBook(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Updating book ID: {} by user: {}", id, userContext.username());
        
        var updatedBook = bookService.updateBook(id, request, userContext);
        return ResponseEntity.ok(updatedBook);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long id,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Deleting book ID: {} by user: {}", id, userContext.username());
        
        bookService.deleteBook(id, userContext);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<BookDto>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean availableOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Searching books with criteria");
        
        var criteria = new BookSearchCriteria(title, author, category, minPrice, maxPrice, availableOnly);
        var books = searchService.searchWithCriteria(criteria, pageable);
        
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/search/title")
    public ResponseEntity<Page<BookDto>> searchByTitle(
            @RequestParam String title,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Searching books by title: {}", title);
        
        var books = searchService.searchByTitle(title, pageable);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/search/author")
    public ResponseEntity<Page<BookDto>> searchByAuthor(
            @RequestParam String author,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Searching books by author: {}", author);
        
        var books = searchService.searchByAuthor(author, pageable);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/search/category")
    public ResponseEntity<Page<BookDto>> searchByCategory(
            @RequestParam String category,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Searching books by category: {}", category);
        
        var books = searchService.searchByCategory(category, pageable);
        return ResponseEntity.ok(books);
    }
    
    @GetMapping("/search/price-range")
    public ResponseEntity<Page<BookDto>> searchByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Searching books by price range: {} - {}", minPrice, maxPrice);
        
        var books = searchService.searchByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(books);
    }
}