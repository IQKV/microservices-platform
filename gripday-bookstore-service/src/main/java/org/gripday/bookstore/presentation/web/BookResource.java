package org.gripday.bookstore.presentation.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Book Management", description = "Book catalog operations including browsing, searching, and administrative management")
public class BookResource {
    
    private static final Logger logger = LoggerFactory.getLogger(BookResource.class);
    
    private final BookService bookService;
    private final SearchService searchService;
    
    public BookResource(BookService bookService, SearchService searchService) {
        this.bookService = bookService;
        this.searchService = searchService;
    }
    
    @Operation(
        summary = "Get paginated book catalog",
        description = """
            Retrieve a paginated list of books with optional filtering by title, author, category, price range, and availability status.
            
            **API Versioning**: This endpoint supports multiple versioning strategies:
            - URL Path: `/api/v1/bookstore/books`
            - Header: `API-Version: 1`
            - Accept: `application/vnd.gripday.bookstore.v1+json`
            - Query param: `?version=1`
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved book catalog",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    name = "Book catalog response",
                    value = """
                        {
                          "content": [
                            {
                              "id": 1,
                              "title": "The Great Gatsby",
                              "author": "F. Scott Fitzgerald",
                              "isbn": "978-0-7432-7356-5",
                              "description": "A classic American novel",
                              "price": 12.99,
                              "categoryName": "Fiction",
                              "available": true,
                              "availableQuantity": 15,
                              "createdAt": "2024-01-15T10:30:00Z",
                              "updatedAt": "2024-01-15T10:30:00Z"
                            }
                          ],
                          "pageable": {
                            "pageNumber": 0,
                            "pageSize": 20
                          },
                          "totalElements": 1,
                          "totalPages": 1,
                          "first": true,
                          "last": true
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<Page<BookDto>> getBooks(
            @Parameter(description = "Filter by book title (case-insensitive partial match)")
            @RequestParam(required = false) String title,
            @Parameter(description = "Filter by author name (case-insensitive partial match)")
            @RequestParam(required = false) String author,
            @Parameter(description = "Filter by category name")
            @RequestParam(required = false) String category,
            @Parameter(description = "Minimum price filter (inclusive)")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter (inclusive)")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter to show only available books")
            @RequestParam(required = false) Boolean availableOnly,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Getting books with filters - title: {}, author: {}, category: {}", title, author, category);
        
        var criteria = new BookSearchCriteria(title, author, category, minPrice, maxPrice, availableOnly);
        var books = bookService.findBooks(criteria, pageable);
        
        return ResponseEntity.ok(books);
    }
    
    @Operation(
        summary = "Get book by ID",
        description = "Retrieve detailed information about a specific book by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Book found and returned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Book not found with the specified ID",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(
            @Parameter(description = "Unique identifier of the book", required = true, example = "1")
            @PathVariable Long id) {
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
    
    @Operation(
        summary = "Create new book (Admin only)",
        description = "Create a new book entry in the catalog. Requires ADMIN or SUPERADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Book created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or validation errors",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions - ADMIN role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Book with ISBN already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<BookDto> createBook(
            @Parameter(description = "Book creation request data", required = true)
            @Valid @RequestBody CreateBookRequest request,
            @Parameter(hidden = true)
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Creating book with title: {} by user: {}", request.title(), userContext.username());
        
        var createdBook = bookService.createBook(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }
    
    @Operation(
        summary = "Update book information (Admin only)",
        description = "Update existing book details. Requires ADMIN or SUPERADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Book updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or validation errors",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions - ADMIN role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Book not found with the specified ID",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBook(
            @Parameter(description = "Unique identifier of the book to update", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Book update request data", required = true)
            @Valid @RequestBody UpdateBookRequest request,
            @Parameter(hidden = true)
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Updating book ID: {} by user: {}", id, userContext.username());
        
        var updatedBook = bookService.updateBook(id, request, userContext);
        return ResponseEntity.ok(updatedBook);
    }
    
    @Operation(
        summary = "Delete book (Admin only)",
        description = "Remove a book from the catalog. Requires ADMIN or SUPERADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Book deleted successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions - ADMIN role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Book not found with the specified ID",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "Unique identifier of the book to delete", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
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