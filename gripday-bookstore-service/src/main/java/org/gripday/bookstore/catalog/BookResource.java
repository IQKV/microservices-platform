package org.gripday.bookstore.catalog;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/bookstore/books")
@Tag(name = "Book Catalog", description = "Public book catalog operations including browsing and searching")
public class BookResource {

  private static final Logger logger = LoggerFactory.getLogger(BookResource.class);

  private final CatalogApplicationService catalogApplicationService;
  private final SearchApplicationService searchApplicationService;
  private final BookCatalogResponseBuilder responseBuilder;

  public BookResource(final CatalogApplicationService catalogApplicationService, final SearchApplicationService searchApplicationService, final BookCatalogResponseBuilder responseBuilder) {
    this.catalogApplicationService = catalogApplicationService;
    this.searchApplicationService = searchApplicationService;
    this.responseBuilder = responseBuilder;
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
              schema = @Schema(implementation = BookCatalogResponse.class),
              examples = @ExampleObject(
                  name = "Book catalog response",
                  value = """
                      {
                        "books": [
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
                        "pagination": {
                          "currentPage": 0,
                          "totalPages": 1,
                          "totalElements": 1,
                          "pageSize": 20,
                          "hasNext": false,
                          "hasPrevious": false,
                          "nextPageUrl": null,
                          "previousPageUrl": null
                        },
                        "filters": {
                          "categories": [],
                          "priceRange": null,
                          "authors": [],
                          "availability": null
                        },
                        "search": {
                          "query": null,
                          "resultCount": 1,
                          "suggestions": [],
                          "searchType": "catalog"
                        }
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request parameters",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      )
  })
  @GetMapping
  public ResponseEntity<BookCatalogResponse> getBooks(
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

    var criteria = new BookSearchQuery(title, author, category, minPrice, maxPrice, availableOnly);
    var books = catalogApplicationService.findBooks(criteria, pageable);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);

    return ResponseEntity.ok(response);
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
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      )
  })
  @GetMapping("/{id}")
  public ResponseEntity<BookDto> getBookById(
      @Parameter(description = "Unique identifier of the book", required = true, example = "1")
      @PathVariable Long id) {
    logger.debug("Getting book by ID: {}", id);

    return catalogApplicationService.findBookById(id)
        .map(book -> ResponseEntity.ok(book))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/isbn/{isbn}")
  public ResponseEntity<BookDto> getBookByIsbn(@PathVariable String isbn) {
    logger.debug("Getting book by ISBN: {}", isbn);

    return catalogApplicationService.findBookByIsbn(isbn)
        .map(book -> ResponseEntity.ok(book))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/available")
  public ResponseEntity<BookCatalogResponse> getAvailableBooks(@PageableDefault(size = 20) Pageable pageable) {
    logger.debug("Getting available books");

    var books = catalogApplicationService.findAvailableBooks(pageable);
    var criteria = new BookSearchQuery(null, null, null, null, null, true);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/in-stock")
  public ResponseEntity<BookCatalogResponse> getBooksInStock(@PageableDefault(size = 20) Pageable pageable) {
    logger.debug("Getting books in stock");

    var books = catalogApplicationService.findBooksInStock(pageable);
    var criteria = new BookSearchQuery(null, null, null, null, null, true);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search")
  public ResponseEntity<BookCatalogResponse> searchBooks(
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String author,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Boolean availableOnly,
      @PageableDefault(size = 20) Pageable pageable) {

    logger.debug("Searching books with criteria");

    var criteria = new BookSearchQuery(title, author, category, minPrice, maxPrice, availableOnly);
    var books = searchApplicationService.searchWithQuery(criteria, pageable);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/search/title")
  public ResponseEntity<BookCatalogResponse> searchByTitle(
      @RequestParam String title,
      @PageableDefault(size = 20) Pageable pageable) {

    logger.debug("Searching books by title: {}", title);

    var criteria = new BookSearchQuery(title, null, null, null, null, null);
    var books = searchApplicationService.searchByTitle(title, pageable);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search/author")
  public ResponseEntity<BookCatalogResponse> searchByAuthor(
      @RequestParam String author,
      @PageableDefault(size = 20) Pageable pageable) {

    logger.debug("Searching books by author: {}", author);

    var criteria = new BookSearchQuery(null, author, null, null, null, null);
    var books = searchApplicationService.searchByAuthor(author, pageable);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search/category")
  public ResponseEntity<BookCatalogResponse> searchByCategory(
      @RequestParam String category,
      @PageableDefault(size = 20) Pageable pageable) {

    logger.debug("Searching books by category: {}", category);

    var criteria = new BookSearchQuery(null, null, category, null, null, null);
    var books = searchApplicationService.searchByCategory(category, pageable);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search/price-range")
  public ResponseEntity<BookCatalogResponse> searchByPriceRange(
      @RequestParam BigDecimal minPrice,
      @RequestParam BigDecimal maxPrice,
      @PageableDefault(size = 20) Pageable pageable) {

    logger.debug("Searching books by price range: {} - {}", minPrice, maxPrice);

    var criteria = new BookSearchQuery(null, null, null, minPrice, maxPrice, null);
    var books = searchApplicationService.searchByPriceRange(minPrice, maxPrice, pageable);
    var baseUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
    var response = responseBuilder.buildWithUrls(books, criteria, baseUrl);
    return ResponseEntity.ok(response);
  }
}
