package org.gripday.bookstore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gripday.bookstore.domain.dto.BookDto;
import org.gripday.bookstore.domain.dto.BulkInventoryRequest;
import org.gripday.bookstore.domain.dto.CreateBookRequest;
import org.gripday.bookstore.domain.dto.UpdateBookRequest;
import org.gripday.bookstore.domain.dto.UpdateInventoryRequest;
import org.gripday.bookstore.domain.dto.UserContext;
import org.gripday.bookstore.domain.exception.UnauthorizedOperationException;
import org.gripday.bookstore.domain.service.BookService;
import org.gripday.bookstore.domain.service.InventoryService;
import org.gripday.bookstore.domain.service.SearchService;
import org.gripday.bookstore.infrastructure.entity.Category;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.gripday.bookstore.infrastructure.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests focused on security, authentication, and authorization scenarios. Tests various user roles and permission combinations in realistic security contexts.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookstoreSecurityIntegrationTest {

  @Autowired
  private BookService bookService;

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private SearchService searchService;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  private Category testCategory;
  private UserContext adminUser;
  private UserContext superAdminUser;
  private UserContext regularUser;
  private UserContext guestUser;
  private BookDto testBook;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    bookRepository.deleteAll();
    categoryRepository.deleteAll();

    // Create test category
    testCategory = new Category();
    testCategory.setName("Test Category");
    testCategory.setDescription("Category for security testing");
    testCategory = categoryRepository.save(testCategory);

    // Create different user contexts with various roles and permissions
    adminUser = new UserContext(
        1L,
        "admin_user",
        "admin@bookstore.com",
        Set.of("ADMIN"),
        Set.of("BOOK_CREATE", "BOOK_UPDATE", "BOOK_DELETE", "INVENTORY_UPDATE", "INVENTORY_ADJUST"),
        "Management",
        "BOOKSTORE_ORG",
        Map.of("department", "IT", "clearance_level", "HIGH")
    );

    superAdminUser = new UserContext(
        2L,
        "super_admin",
        "superadmin@bookstore.com",
        Set.of("SUPERADMIN"),
        Set.of("BOOK_CREATE", "BOOK_UPDATE", "BOOK_DELETE", "INVENTORY_UPDATE", "INVENTORY_ADJUST", "SYSTEM_ADMIN"),
        "Executive",
        "BOOKSTORE_ORG",
        Map.of("department", "Executive", "clearance_level", "MAXIMUM")
    );

    regularUser = new UserContext(
        3L,
        "regular_customer",
        "customer@example.com",
        Set.of("USER"),
        Set.of("BOOK_READ", "INVENTORY_READ"),
        "Customer",
        "BOOKSTORE_ORG",
        Map.of("customer_tier", "STANDARD")
    );

    guestUser = new UserContext(
        4L,
        "guest_user",
        "guest@example.com",
        Set.of("GUEST"),
        Set.of("BOOK_READ_LIMITED"),
        "Guest",
        "BOOKSTORE_ORG",
        Map.of("session_type", "ANONYMOUS")
    );

    // Create a test book for security testing
    var createRequest = new CreateBookRequest(
        "Security Test Book",
        "Test Author",
        "978-0-123-45678-9",
        "A book for testing security",
        new BigDecimal("25.99"),
        testCategory.getId(),
        50
    );
    testBook = bookService.createBook(createRequest, adminUser);
  }

  @Test
  void shouldAllowAdminUsersToPerformAllBookOperations() {
    // Admin should be able to create books
    var createRequest = new CreateBookRequest(
        "Admin Created Book",
        "Admin Author",
        "978-1-111-11111-1",
        "Created by admin",
        new BigDecimal("30.00"),
        testCategory.getId(),
        25
    );

    assertThatCode(() -> bookService.createBook(createRequest, adminUser))
        .doesNotThrowAnyException();

    // Admin should be able to update books
    var updateRequest = new UpdateBookRequest(
        "Updated by Admin",
        "Admin Author",
        "Updated description",
        new BigDecimal("32.00"),
        testCategory.getId()
    );

    assertThatCode(() -> bookService.updateBook(testBook.id(), updateRequest, adminUser))
        .doesNotThrowAnyException();

    // Admin should be able to delete books
    assertThatCode(() -> bookService.deleteBook(testBook.id(), adminUser))
        .doesNotThrowAnyException();

    // Verify book was soft deleted
    var deletedBook = bookService.findBookById(testBook.id());
    assertThat(deletedBook).isPresent();
    assertThat(deletedBook.get().available()).isFalse();
  }

  @Test
  void shouldAllowSuperAdminUsersToPerformAllOperations() {
    // SuperAdmin should have all admin capabilities plus more
    var createRequest = new CreateBookRequest(
        "SuperAdmin Book",
        "SuperAdmin Author",
        "978-2-222-22222-2",
        "Created by superadmin",
        new BigDecimal("35.00"),
        testCategory.getId(),
        30
    );

    var createdBook = bookService.createBook(createRequest, superAdminUser);
    assertThat(createdBook).isNotNull();

    // SuperAdmin should be able to perform all inventory operations
    var inventoryUpdate = new UpdateInventoryRequest(100, 15);
    assertThatCode(() -> inventoryService.updateInventory(createdBook.id(), inventoryUpdate, superAdminUser))
        .doesNotThrowAnyException();

    assertThatCode(() -> inventoryService.adjustInventoryQuantity(createdBook.id(), -10, superAdminUser))
        .doesNotThrowAnyException();

    // SuperAdmin should be able to perform bulk operations
    var bulkRequests = List.of(
        new BulkInventoryRequest(createdBook.id(), 150, 20),
        new BulkInventoryRequest(testBook.id(), 75, 10)
    );

    assertThatCode(() -> inventoryService.bulkUpdateInventory(bulkRequests, superAdminUser))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRestrictRegularUsersToReadOnlyOperations() {
    // Regular users should be able to read book information
    assertThatCode(() -> bookService.findBookById(testBook.id()))
        .doesNotThrowAnyException();

    assertThatCode(() -> bookService.findAvailableBooks(PageRequest.of(0, 10)))
        .doesNotThrowAnyException();

    // Regular users should be able to search
    assertThatCode(() -> searchService.searchByTitle("Security", PageRequest.of(0, 10)))
        .doesNotThrowAnyException();

    assertThatCode(() -> searchService.searchByAuthor("Test", PageRequest.of(0, 10)))
        .doesNotThrowAnyException();

    // Regular users should be able to view inventory
    assertThatCode(() -> inventoryService.getInventory(testBook.id()))
        .doesNotThrowAnyException();

    // Regular users should NOT be able to create books
    var createRequest = new CreateBookRequest(
        "Unauthorized Book",
        "Regular User",
        "978-3-333-33333-3",
        "Should not be created",
        new BigDecimal("20.00"),
        testCategory.getId(),
        10
    );

    assertThatThrownBy(() -> bookService.createBook(createRequest, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("create book")
        .hasMessageContaining("ADMIN or SUPERADMIN");

    // Regular users should NOT be able to update books
    var updateRequest = new UpdateBookRequest(
        "Unauthorized Update",
        "Regular User",
        "Should not be updated",
        new BigDecimal("25.00"),
        testCategory.getId()
    );

    assertThatThrownBy(() -> bookService.updateBook(testBook.id(), updateRequest, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("update book");

    // Regular users should NOT be able to delete books
    assertThatThrownBy(() -> bookService.deleteBook(testBook.id(), regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("delete book");

    // Regular users should NOT be able to update inventory
    var inventoryUpdate = new UpdateInventoryRequest(100, 10);
    assertThatThrownBy(() -> inventoryService.updateInventory(testBook.id(), inventoryUpdate, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("update inventory");

    // Regular users should NOT be able to adjust inventory
    assertThatThrownBy(() -> inventoryService.adjustInventoryQuantity(testBook.id(), 10, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("adjust inventory");

    // Regular users should NOT be able to perform bulk inventory updates
    var bulkRequests = List.of(new BulkInventoryRequest(testBook.id(), 100, 10));
    assertThatThrownBy(() -> inventoryService.bulkUpdateInventory(bulkRequests, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("bulk update inventory");
  }

  @Test
  void shouldAllowRegularUsersToPerformInventoryReservations() {
    // Regular users should be able to reserve inventory (simulating purchases)
    assertThatCode(() -> inventoryService.reserveQuantity(testBook.id(), 5, regularUser))
        .doesNotThrowAnyException();

    // Verify reservation was successful
    var inventory = inventoryService.getInventory(testBook.id());
    assertThat(inventory.reservedQuantity()).isEqualTo(5);
    assertThat(inventory.availableQuantity()).isEqualTo(45); // 50 - 5

    // Regular users should be able to release reservations
    assertThatCode(() -> inventoryService.releaseReservedQuantity(testBook.id(), 2, regularUser))
        .doesNotThrowAnyException();

    // Verify release was successful
    var updatedInventory = inventoryService.getInventory(testBook.id());
    assertThat(updatedInventory.reservedQuantity()).isEqualTo(3);
    assertThat(updatedInventory.availableQuantity()).isEqualTo(47); // 50 - 3

    // Regular users should be able to check availability
    assertThat(inventoryService.isBookAvailable(testBook.id(), 10)).isTrue();
    assertThat(inventoryService.isBookAvailable(testBook.id(), 50)).isFalse(); // More than available
  }

  @Test
  void shouldEnforceRoleBasedAccessControl() {
    // Test that role checking works correctly
    assertThat(adminUser.isAdmin()).isTrue();
    assertThat(adminUser.isSuperAdmin()).isFalse();
    assertThat(adminUser.hasRole("ADMIN")).isTrue();
    assertThat(adminUser.hasRole("SUPERADMIN")).isFalse();

    assertThat(superAdminUser.isAdmin()).isTrue(); // SuperAdmin includes Admin
    assertThat(superAdminUser.isSuperAdmin()).isTrue();
    assertThat(superAdminUser.hasRole("SUPERADMIN")).isTrue();

    assertThat(regularUser.isAdmin()).isFalse();
    assertThat(regularUser.isSuperAdmin()).isFalse();
    assertThat(regularUser.hasRole("USER")).isTrue();

    // Test hasAnyRole functionality
    assertThat(adminUser.hasAnyRole("ADMIN", "SUPERADMIN")).isTrue();
    assertThat(regularUser.hasAnyRole("ADMIN", "SUPERADMIN")).isFalse();
    assertThat(regularUser.hasAnyRole("USER", "GUEST")).isTrue();
  }

  @Test
  void shouldHandleMultipleUserOperationsSimultaneously() {
    // Scenario: Multiple users performing operations at the same time

    // Admin creates multiple books
    var adminBook1 = createBookAsUser("Admin Book 1", "978-4-444-44444-4", adminUser);
    var adminBook2 = createBookAsUser("Admin Book 2", "978-5-555-55555-5", adminUser);

    // SuperAdmin creates a book
    var superAdminBook = createBookAsUser("SuperAdmin Book", "978-6-666-66666-6", superAdminUser);

    // Regular users perform read operations
    assertThatCode(() -> {
      bookService.findBookById(adminBook1.id());
      bookService.findBookById(adminBook2.id());
      bookService.findBookById(superAdminBook.id());
    }).doesNotThrowAnyException();

    // Regular users reserve inventory from different books
    inventoryService.reserveQuantity(adminBook1.id(), 5, regularUser);
    inventoryService.reserveQuantity(adminBook2.id(), 3, regularUser);
    inventoryService.reserveQuantity(superAdminBook.id(), 7, regularUser);

    // Admin updates inventory while regular user has reservations
    var inventoryUpdate = new UpdateInventoryRequest(100, 15);
    assertThatCode(() -> inventoryService.updateInventory(adminBook1.id(), inventoryUpdate, adminUser))
        .doesNotThrowAnyException();

    // Verify reservations are maintained after admin update
    var updatedInventory = inventoryService.getInventory(adminBook1.id());
    assertThat(updatedInventory.reservedQuantity()).isEqualTo(5);
    assertThat(updatedInventory.availableQuantity()).isEqualTo(95); // 100 - 5
  }

  @Test
  void shouldPreventUnauthorizedBulkOperations() {
    // Create multiple books for bulk testing
    var book1 = createBookAsUser("Bulk Test Book 1", "978-7-777-77777-7", adminUser);
    var book2 = createBookAsUser("Bulk Test Book 2", "978-8-888-88888-8", adminUser);
    var book3 = createBookAsUser("Bulk Test Book 3", "978-9-999-99999-9", adminUser);

    // Regular user should not be able to perform bulk inventory updates
    var bulkRequests = List.of(
        new BulkInventoryRequest(book1.id(), 100, 10),
        new BulkInventoryRequest(book2.id(), 150, 15),
        new BulkInventoryRequest(book3.id(), 200, 20)
    );

    assertThatThrownBy(() -> inventoryService.bulkUpdateInventory(bulkRequests, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("bulk update inventory");

    // Admin should be able to perform bulk operations
    assertThatCode(() -> inventoryService.bulkUpdateInventory(bulkRequests, adminUser))
        .doesNotThrowAnyException();

    // Verify bulk operations were successful
    var inventory1 = inventoryService.getInventory(book1.id());
    var inventory2 = inventoryService.getInventory(book2.id());
    var inventory3 = inventoryService.getInventory(book3.id());

    assertThat(inventory1.quantity()).isEqualTo(100);
    assertThat(inventory2.quantity()).isEqualTo(150);
    assertThat(inventory3.quantity()).isEqualTo(200);
  }

  @Test
  void shouldMaintainDataIntegrityAcrossUserOperations() {
    // Scenario: Ensure data integrity when multiple users operate on the same data

    // Admin creates a book
    var sharedBook = createBookAsUser("Shared Book", "978-1-234-56789-0", adminUser);

    // Multiple regular users reserve inventory
    inventoryService.reserveQuantity(sharedBook.id(), 10, regularUser);

    var anotherUser = new UserContext(
        5L, "another_user", "another@example.com", Set.of("USER"),
        Set.of("BOOK_READ"), "Customer", "BOOKSTORE_ORG", Map.of()
    );
    inventoryService.reserveQuantity(sharedBook.id(), 15, anotherUser);

    // Verify total reservations
    var inventory = inventoryService.getInventory(sharedBook.id());
    assertThat(inventory.reservedQuantity()).isEqualTo(25);
    assertThat(inventory.availableQuantity()).isEqualTo(25); // 50 - 25

    // Admin updates inventory while reservations exist
    var inventoryUpdate = new UpdateInventoryRequest(100, 20);
    inventoryService.updateInventory(sharedBook.id(), inventoryUpdate, adminUser);

    // Verify reservations are preserved and calculations are correct
    var updatedInventory = inventoryService.getInventory(sharedBook.id());
    assertThat(updatedInventory.quantity()).isEqualTo(100);
    assertThat(updatedInventory.reservedQuantity()).isEqualTo(25);
    assertThat(updatedInventory.availableQuantity()).isEqualTo(75); // 100 - 25

    // Users release their reservations
    inventoryService.releaseReservedQuantity(sharedBook.id(), 10, regularUser);
    inventoryService.releaseReservedQuantity(sharedBook.id(), 15, anotherUser);

    // Verify all reservations are released
    var finalInventory = inventoryService.getInventory(sharedBook.id());
    assertThat(finalInventory.reservedQuantity()).isEqualTo(0);
    assertThat(finalInventory.availableQuantity()).isEqualTo(100);
  }

  private BookDto createBookAsUser(String title, String isbn, UserContext user) {
    var createRequest = new CreateBookRequest(
        title,
        "Test Author",
        isbn,
        "Test description",
        new BigDecimal("25.00"),
        testCategory.getId(),
        50
    );
    return bookService.createBook(createRequest, user);
  }
}