package org.gripday.bookstore.shared;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.bookstore.catalog.BookNotFoundException;
import org.gripday.bookstore.catalog.CatalogApplicationService;
import org.gripday.bookstore.catalog.CategoryInUseException;
import org.gripday.bookstore.catalog.CategoryNotFoundException;
import org.gripday.bookstore.catalog.CreateBookCommand;
import org.gripday.bookstore.catalog.DuplicateCategoryException;
import org.gripday.bookstore.catalog.DuplicateIsbnException;
import org.gripday.bookstore.catalog.InvalidPriceRangeException;
import org.gripday.bookstore.catalog.SearchApplicationService;
import org.gripday.bookstore.inventory.BulkOperationException;
import org.gripday.bookstore.inventory.InsufficientInventoryException;
import org.gripday.bookstore.inventory.InvalidInventoryAdjustmentException;
import org.gripday.bookstore.inventory.InvalidInventoryQuantityException;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({org.gripday.bookstore.catalog.BookResource.class, org.gripday.bookstore.shared.web.BookstoreExceptionHandler.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({org.gripday.bookstore.shared.TestSecurityConfig.class, org.gripday.bookstore.catalog.BookManagementResource.class, org.gripday.bookstore.catalog.BookCatalogResponseBuilder.class})
class BookstoreExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CatalogApplicationService catalogApplicationService;

  @MockBean
  private SearchApplicationService searchApplicationService;

  private UserContext createAdminUserContext() {
    return new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ADMIN"),
        Set.of("BOOK_WRITE"),
        "IT",
        "org1",
        Map.of()
    );
  }

  @Test
  void handleBookNotFoundException_ShouldReturn404WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new BookNotFoundException(999L));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567897", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("The requested book could not be found"))
        .andExpect(jsonPath("$.detail").exists())
        .andExpect(jsonPath("$.instance").value("/api/v1/bookstore/admin/books"))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.method").value("POST"))
        .andExpect(jsonPath("$.correlationId").exists())
        .andExpect(jsonPath("$.requestId").exists());
  }

  @Test
  void handleCategoryNotFoundException_ShouldReturn404WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new CategoryNotFoundException(999L));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567890", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        999L, // Non-existent category
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.title").value("The requested category could not be found"));
  }

  @Test
  void handleInsufficientInventoryException_ShouldReturn409WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new InsufficientInventoryException(1L, 100, 5));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567883", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        100 // Requesting more than available
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("DOMAIN_INSUFFICIENT_INVENTORY"))
        .andExpect(jsonPath("$.title").value("Not enough inventory available for the requested operation"));
  }

  @Test
  void handleDuplicateIsbnException_ShouldReturn409WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new DuplicateIsbnException("9781234567876"));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567876", // Valid ISBN-13 - Duplicate
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("DOMAIN_DUPLICATE_ISBN"))
        .andExpect(jsonPath("$.title").value("A book with this ISBN already exists in the catalog"));
  }

  @Test
  void handleUnauthorizedOperationException_ShouldReturn403WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new UnauthorizedOperationException("Insufficient privileges"));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567869", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_INSUFFICIENT_PRIVILEGES"))
        .andExpect(jsonPath("$.title").value("You do not have sufficient privileges to perform this operation"));
  }

  @Test
  void handleValidationErrors_ShouldReturn400WithProblemDetailFields() throws Exception {
    var invalidRequest = new CreateBookCommand(
        "", // Invalid empty title
        "", // Invalid empty author
        "invalid-isbn", // Invalid ISBN format
        "Description",
        new BigDecimal("-10.00"), // Invalid negative price
        null, // Missing category ID
        -5 // Invalid negative quantity
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.detail").value("One or more fields contain invalid values"))
        .andExpect(jsonPath("$.fields").isArray())
        .andExpect(jsonPath("$.fields").isNotEmpty());
  }

  @Test
  void handleTypeMismatchException_ShouldReturn400WithProblemDetail() throws Exception {
    mockMvc.perform(get("/api/v1/bookstore/books/invalid-id")) // Non-numeric ID
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.title").value("Invalid parameter type"))
        .andExpect(jsonPath("$.detail").value("The provided parameter value has an incorrect type"))
        .andExpect(jsonPath("$.fields").isArray())
        .andExpect(jsonPath("$.fields[0].field").value("id"))
        .andExpect(jsonPath("$.fields[0].rejectedValue").value("invalid-id"));
  }

  @Test
  void handleIllegalArgumentException_ShouldReturn400WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new IllegalArgumentException("Invalid argument provided"));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567852", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.title").value("The provided argument is invalid"))
        .andExpect(jsonPath("$.detail").value("Invalid argument provided"));
  }

  @Test
  void handleGenericException_ShouldReturn500WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new RuntimeException("Unexpected system error"));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567845", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("SYSTEM_INTERNAL_ERROR"))
        .andExpect(jsonPath("$.title").value("An unexpected error occurred"))
        .andExpect(jsonPath("$.detail").value("Please try again later or contact support if the problem persists"));
  }

  @Test
  void handleInvalidInventoryQuantityException_ShouldReturn400WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new InvalidInventoryQuantityException(1L, 5, 10));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567838", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("DOMAIN_INVALID_INVENTORY_QUANTITY"))
        .andExpect(jsonPath("$.title").value("Cannot set inventory quantity below reserved amount"))
        .andExpect(jsonPath("$.bookId").value(1))
        .andExpect(jsonPath("$.requestedQuantity").value(5))
        .andExpect(jsonPath("$.reservedQuantity").value(10));
  }

  @Test
  void handleInvalidInventoryAdjustmentException_ShouldReturn400WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new InvalidInventoryAdjustmentException(1L, 10, -15));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567821", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("DOMAIN_INVALID_INVENTORY_ADJUSTMENT"))
        .andExpect(jsonPath("$.title").value("Inventory adjustment would result in invalid state"))
        .andExpect(jsonPath("$.bookId").value(1))
        .andExpect(jsonPath("$.currentQuantity").value(10))
        .andExpect(jsonPath("$.adjustment").value(-15));
  }

  @Test
  void handleInvalidPriceRangeException_ShouldReturn400WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new InvalidPriceRangeException(new BigDecimal("50.00"), new BigDecimal("20.00")));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567814", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_INVALID_PRICE_RANGE"))
        .andExpect(jsonPath("$.title").value("Invalid price range specified"))
        .andExpect(jsonPath("$.minPrice").value(50.00))
        .andExpect(jsonPath("$.maxPrice").value(20.00));
  }

  @Test
  void handleInvalidIsbnFormatException_ShouldReturn400WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new InvalidIsbnFormatException("123456789"));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567807", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_INVALID_ISBN"))
        .andExpect(jsonPath("$.title").value("The provided ISBN format is invalid"))
        .andExpect(jsonPath("$.isbn").value("123456789"));
  }

  @Test
  void handleBulkOperationException_ShouldReturn207WithProblemDetail() throws Exception {
    var failures = List.of(
        new BulkOperationException.BulkOperationFailure(1L, "Quantity below reserved", null),
        new BulkOperationException.BulkOperationFailure(2L, "Book not found", null)
    );
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new BulkOperationException(5, 3, failures));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567791", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isMultiStatus())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("DOMAIN_BULK_OPERATION_PARTIAL_FAILURE"))
        .andExpect(jsonPath("$.title").value("Bulk operation completed with some failures"))
        .andExpect(jsonPath("$.totalRequested").value(5))
        .andExpect(jsonPath("$.successCount").value(3))
        .andExpect(jsonPath("$.failureCount").value(2))
        .andExpect(jsonPath("$.failures").isArray())
        .andExpect(jsonPath("$.failures[0].bookId").value(1))
        .andExpect(jsonPath("$.failures[0].reason").value("Quantity below reserved"));
  }

  @Test
  void handleCategoryInUseException_ShouldReturn409WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new CategoryInUseException(1L, "Fiction", 5));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567784", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("DOMAIN_CATEGORY_IN_USE"))
        .andExpect(jsonPath("$.title").value("Cannot delete category that has books associated with it"))
        .andExpect(jsonPath("$.categoryId").value(1))
        .andExpect(jsonPath("$.categoryName").value("Fiction"))
        .andExpect(jsonPath("$.bookCount").value(5));
  }

  @Test
  void handleDuplicateCategoryException_ShouldReturn409WithProblemDetail() throws Exception {
    when(catalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new DuplicateCategoryException("Fiction"));

    var request = new CreateBookCommand(
        "Test Book",
        "Test Author",
        "9781234567777", // Valid ISBN-13
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("DOMAIN_DUPLICATE_CATEGORY"))
        .andExpect(jsonPath("$.title").value("A category with this name already exists"))
        .andExpect(jsonPath("$.categoryName").value("Fiction"));
  }
}
