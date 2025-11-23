package org.gripday.bookstore.catalog;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.bookstore.shared.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookstore/admin/books")
@Tag(name = "Book Management (Admin)", description = "Administrative operations for managing book catalog - requires ADMIN or SUPERADMIN role")
public class BookManagementResource {

  private static final Logger logger = LoggerFactory.getLogger(BookManagementResource.class);

  private final CatalogApplicationService catalogApplicationService;

  public BookManagementResource(final CatalogApplicationService catalogApplicationService) {
    this.catalogApplicationService = catalogApplicationService;
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
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Authentication required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "Book with ISBN already exists",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      )
  })
  @PostMapping
  public ResponseEntity<BookDto> createBook(
      @Parameter(description = "Book creation request data", required = true)
      @Valid @RequestBody CreateBookCommand command,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Creating book with title: {} by user: {}", command.title(), userContext.username());

    var createdBook = catalogApplicationService.createBook(command, userContext);
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
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Authentication required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
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
  @PutMapping("/{id}")
  public ResponseEntity<BookDto> updateBook(
      @Parameter(description = "Unique identifier of the book to update", required = true, example = "1")
      @PathVariable Long id,
      @Parameter(description = "Book update request data", required = true)
      @Valid @RequestBody UpdateBookCommand command,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Updating book ID: {} by user: {}", id, userContext.username());

    var updatedBook = catalogApplicationService.updateBook(id, command, userContext);
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
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
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
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteBook(
      @Parameter(description = "Unique identifier of the book to delete", required = true, example = "1")
      @PathVariable Long id,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Deleting book ID: {} by user: {}", id, userContext.username());

    catalogApplicationService.deleteBook(id, userContext);
    return ResponseEntity.noContent().build();
  }
}
