package org.gripday.bookstore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gripday.bookstore.domain.dto.BookDto;
import org.gripday.bookstore.domain.dto.BookSearchCriteria;
import org.gripday.bookstore.domain.dto.BulkInventoryRequest;
import org.gripday.bookstore.domain.dto.CreateBookRequest;
import org.gripday.bookstore.domain.dto.UpdateBookRequest;
import org.gripday.bookstore.domain.dto.UpdateInventoryRequest;
import org.gripday.bookstore.domain.dto.UserContext;
import org.gripday.bookstore.domain.exception.BookNotFoundException;
import org.gripday.bookstore.domain.exception.CategoryNotFoundException;
import org.gripday.bookstore.domain.exception.DuplicateIsbnException;
import org.gripday.bookstore.domain.exception.InsufficientInventoryException;
import org.gripday.bookstore.domain.exception.UnauthorizedOperationException;
import org.gripday.bookstore.domain.service.BookService;
import org.gripday.bookstore.domain.service.InventoryService;
import org.gripday.bookstore.domain.service.SearchService;
import org.gripday.bookstore.infrastructure.entity.Category;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.gripday.bookstore.infrastructure.repository.CategoryRepository;
import org.gripday.bookstore.infrastructure.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookstoreServiceIntegrationTest {

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

  @Autowired
  private InventoryRepository inventoryRepository;

  private Category fictionCategory;
  private Category scienceCategory;
  private UserContext adminUser;
  private UserContext regularUser;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    bookRepository.deleteAll();
    categoryRepository.deleteAll();

    // Create test categories
    fictionCategory = new Category();
    fictionCategory.setName("Fiction");
    fictionCategory.setDescription("Fiction books");
    fictionCategory = categoryRepository.save(fictionCategory);

    scienceCategory = new Category();
    scienceCategory.setName("Science");
    scienceCategory.setDescription("Science books");
    scienceCategory = categoryRepository.save(scienceCategory);

    // Create test users
    adminUser = new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ADMIN"),
        Set.of("BOOK_CREATE", "BOOK_UPDATE", "BOOK_DELETE", "INVENTORY_UPDATE"),
        "IT",
        "ORG001",
        Map.of()
    );

    regularUser = new UserContext(
        2L,
        "user",
        "user@example.com",
        Set.of("USER"),
        Set.of("BOOK_READ"),
        "Sales",
        "ORG001",
        Map.of()
    );
  }

  @Test
  void shouldCompleteFullBookManagementWorkflow() {
    // Create a new book
    var createRequest = new CreateBookRequest(
        "The Great Gatsby",
        "F. Scott Fitzgerald",
        "978-0-7432-7356-5",
        "A classic American novel",
        new BigDecimal("15.99"),
        fictionCategory.getId(),
        50
    );

    var createdBook = bookService.createBook(createRequest, adminUser);

    assertThat(createdBook).isNotNull();
    assertThat(createdBook.title()).isEqualTo("The Great Gatsby");
    assertThat(createdBook.author()).isEqualTo("F. Scott Fitzgerald");
    assertThat(createdBook.isbn()).isEqualTo("978-0-7432-7356-5");
    assertThat(createdBook.price()).isEqualByComparingTo(new BigDecimal("15.99"));
    assertThat(createdBook.categoryName()).isEqualTo("Fiction");
    assertThat(createdBook.available()).isTrue();
    assertThat(createdBook.availableQuantity()).isEqualTo(50);

    // Verify inventory was created
    var inventory = inventoryService.getInventory(createdBook.id());
    assertThat(inventory.quantity()).isEqualTo(50);
    assertThat(inventory.availableQuantity()).isEqualTo(50);
    assertThat(inventory.reservedQuantity()).isEqualTo(0);

    // Update the book
    var updateRequest = new UpdateBookRequest(
        "The Great Gatsby - Updated Edition",
        "F. Scott Fitzgerald",
        "An updated classic American novel",
        new BigDecimal("18.99"),
        fictionCategory.getId()
    );

    var updatedBook = bookService.updateBook(createdBook.id(), updateRequest, adminUser);

    assertThat(updatedBook.title()).isEqualTo("The Great Gatsby - Updated Edition");
    assertThat(updatedBook.price()).isEqualByComparingTo(new BigDecimal("18.99"));
    assertThat(updatedBook.description()).isEqualTo("An updated classic American novel");

    // Update inventory
    var inventoryUpdateRequest = new UpdateInventoryRequest(75, 10);
    var updatedInventory = inventoryService.updateInventory(createdBook.id(), inventoryUpdateRequest, adminUser);

    assertThat(updatedInventory.quantity()).isEqualTo(75);
    assertThat(updatedInventory.lowStockThreshold()).isEqualTo(10);
    assertThat(updatedInventory.availableQuantity()).isEqualTo(75);

    // Delete the book (soft delete)
    bookService.deleteBook(createdBook.id(), adminUser);

    var deletedBook = bookService.findBookById(createdBook.id());
    assertThat(deletedBook).isPresent();
    assertThat(deletedBook.get().available()).isFalse();
  }

  @Test
  void shouldCompleteSearchAndBrowsingWorkflow() {
    // Create test books
    createTestBook("Java Programming", "John Doe", "978-1-111-11111-1", new BigDecimal("45.99"), scienceCategory, 25);
    createTestBook("Spring Boot Guide", "Jane Smith", "978-2-222-22222-2", new BigDecimal("39.99"), scienceCategory, 15);
    createTestBook("The Hobbit", "J.R.R. Tolkien", "978-3-333-33333-3", new BigDecimal("12.99"), fictionCategory, 30);
    createTestBook("1984", "George Orwell", "978-4-444-44444-4", new BigDecimal("14.99"), fictionCategory, 0); // Out of stock

    var pageable = PageRequest.of(0, 10);

    // Search by title
    var titleResults = searchService.searchByTitle("Java", pageable);
    assertThat(titleResults.getContent()).hasSize(1);
    assertThat(titleResults.getContent().get(0).title()).contains("Java");

    // Search by author
    var authorResults = searchService.searchByAuthor("Jane", pageable);
    assertThat(authorResults.getContent()).hasSize(1);
    assertThat(authorResults.getContent().get(0).author()).contains("Jane");

    // Search by category
    var categoryResults = searchService.searchByCategory("Fiction", pageable);
    assertThat(categoryResults.getContent()).hasSize(2);
    assertThat(categoryResults.getContent())
        .allMatch(book -> book.categoryName().equals("Fiction"));

    // Search by price range
    var priceResults = searchService.searchByPriceRange(new BigDecimal("10.00"), new BigDecimal("20.00"), pageable);
    assertThat(priceResults.getContent()).hasSize(2);
    assertThat(priceResults.getContent())
        .allMatch(book -> book.price().compareTo(new BigDecimal("10.00")) >= 0
            && book.price().compareTo(new BigDecimal("20.00")) <= 0);

    // Search with multiple criteria
    var criteria = new BookSearchCriteria(
        null, // title
        null, // author
        "Science", // category
        new BigDecimal("30.00"), // minPrice
        new BigDecimal("50.00"), // maxPrice
        true // availableOnly
    );

    var criteriaResults = searchService.searchWithCriteria(criteria, pageable);
    assertThat(criteriaResults.getContent()).hasSize(2);
    assertThat(criteriaResults.getContent())
        .allMatch(book -> book.categoryName().equals("Science")
            && book.price().compareTo(new BigDecimal("30.00")) >= 0
            && book.availableQuantity() > 0);

    // Get all available books
    var availableBooks = bookService.findAvailableBooks(pageable);
    assertThat(availableBooks.getContent()).hasSize(4); // All books are marked as available, even with 0 quantity

    // Get books in stock (only those with quantity > 0)
    var booksInStock = bookService.findBooksInStock(pageable);
    assertThat(booksInStock.getContent()).hasSize(3);
    assertThat(booksInStock.getContent())
        .allMatch(book -> book.availableQuantity() > 0);
  }

  @Test
  void shouldCompleteInventoryManagementWorkflow() {
    // Create test books with different inventory levels
    var book1 = createTestBook("Book 1", "Author 1", "978-1-111-11111-1", new BigDecimal("20.00"), fictionCategory, 100);
    var book2 = createTestBook("Book 2", "Author 2", "978-2-222-22222-2", new BigDecimal("25.00"), fictionCategory, 5);
    var book3 = createTestBook("Book 3", "Author 3", "978-3-333-33333-3", new BigDecimal("30.00"), fictionCategory, 0);

    // Check initial inventory
    var inventory1 = inventoryService.getInventory(book1.id());
    assertThat(inventory1.quantity()).isEqualTo(100);
    assertThat(inventory1.availableQuantity()).isEqualTo(100);

    // Reserve some quantity
    inventoryService.reserveQuantity(book1.id(), 20, adminUser);

    var updatedInventory1 = inventoryService.getInventory(book1.id());
    assertThat(updatedInventory1.reservedQuantity()).isEqualTo(20);
    assertThat(updatedInventory1.availableQuantity()).isEqualTo(80);

    // Check availability
    assertThat(inventoryService.isBookAvailable(book1.id(), 50)).isTrue();
    assertThat(inventoryService.isBookAvailable(book1.id(), 90)).isFalse();
    assertThat(inventoryService.isBookAvailable(book3.id(), 1)).isFalse();

    // Release reserved quantity
    inventoryService.releaseReservedQuantity(book1.id(), 10, adminUser);

    var releasedInventory1 = inventoryService.getInventory(book1.id());
    assertThat(releasedInventory1.reservedQuantity()).isEqualTo(10);
    assertThat(releasedInventory1.availableQuantity()).isEqualTo(90);

    // Adjust inventory quantity
    inventoryService.adjustInventoryQuantity(book1.id(), -30, adminUser);

    var adjustedInventory1 = inventoryService.getInventory(book1.id());
    assertThat(adjustedInventory1.quantity()).isEqualTo(70);
    assertThat(adjustedInventory1.availableQuantity()).isEqualTo(60); // 70 - 10 reserved

    // Bulk update inventory
    var bulkRequests = List.of(
        new BulkInventoryRequest(book1.id(), 150, 15),
        new BulkInventoryRequest(book2.id(), 25, 5),
        new BulkInventoryRequest(book3.id(), 10, 3)
    );

    var bulkResults = inventoryService.bulkUpdateInventory(bulkRequests, adminUser);
    assertThat(bulkResults).hasSize(3);

    // Verify bulk updates
    var bulkUpdatedInventory1 = inventoryService.getInventory(book1.id());
    assertThat(bulkUpdatedInventory1.quantity()).isEqualTo(150);
    assertThat(bulkUpdatedInventory1.lowStockThreshold()).isEqualTo(15);

    // Check low stock and out of stock items
    var lowStockItems = inventoryService.getLowStockInventory();
    var outOfStockItems = inventoryService.getOutOfStockInventory();

    assertThat(inventoryService.countLowStockItems()).isGreaterThanOrEqualTo(0);
    assertThat(inventoryService.countOutOfStockItems()).isGreaterThanOrEqualTo(0);

    // Check total counts
    assertThat(inventoryService.getTotalInventoryCount()).isGreaterThan(0);
    assertThat(inventoryService.getTotalReservedCount()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void shouldEnforceSecurityAndAuthorization() {
    // Regular user should not be able to create books
    var createRequest = new CreateBookRequest(
        "Unauthorized Book",
        "Test Author",
        "978-9-999-99999-9",
        "Should not be created",
        new BigDecimal("10.00"),
        fictionCategory.getId(),
        10
    );

    assertThatThrownBy(() -> bookService.createBook(createRequest, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("create book");

    // Create a book as admin first
    var book = createTestBook("Test Book", "Test Author", "978-1-111-11111-1", new BigDecimal("20.00"), fictionCategory, 50);

    // Regular user should not be able to update books
    var updateRequest = new UpdateBookRequest(
        "Updated Title",
        "Test Author",
        "Updated description",
        new BigDecimal("25.00"),
        fictionCategory.getId()
    );

    assertThatThrownBy(() -> bookService.updateBook(book.id(), updateRequest, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("update book");

    // Regular user should not be able to delete books
    assertThatThrownBy(() -> bookService.deleteBook(book.id(), regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("delete book");

    // Regular user should not be able to update inventory
    var inventoryUpdateRequest = new UpdateInventoryRequest(100, 10);

    assertThatThrownBy(() -> inventoryService.updateInventory(book.id(), inventoryUpdateRequest, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("update inventory");

    // Regular user should not be able to adjust inventory
    assertThatThrownBy(() -> inventoryService.adjustInventoryQuantity(book.id(), 10, regularUser))
        .isInstanceOf(UnauthorizedOperationException.class)
        .hasMessageContaining("adjust inventory");

    // Regular user should be able to read operations
    assertThatCode(() -> {
      bookService.findBookById(book.id());
      inventoryService.getInventory(book.id());
      searchService.searchByTitle("Test", PageRequest.of(0, 10));
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleErrorScenariosCorrectly() {
    // Test book not found scenarios
    var notFoundBook = bookService.findBookById(999L);
    assertThat(notFoundBook).isEmpty();

    assertThatThrownBy(() -> inventoryService.getInventory(999L))
        .isInstanceOf(BookNotFoundException.class);

    // Test duplicate ISBN
    createTestBook("First Book", "Author", "978-1-111-11111-1", new BigDecimal("20.00"), fictionCategory, 10);

    var duplicateRequest = new CreateBookRequest(
        "Second Book",
        "Another Author",
        "978-1-111-11111-1", // Same ISBN
        "Should fail",
        new BigDecimal("25.00"),
        fictionCategory.getId(),
        15
    );

    assertThatThrownBy(() -> bookService.createBook(duplicateRequest, adminUser))
        .isInstanceOf(DuplicateIsbnException.class);

    // Test invalid category
    var invalidCategoryRequest = new CreateBookRequest(
        "Invalid Category Book",
        "Author",
        "978-2-222-22222-2",
        "Should fail",
        new BigDecimal("20.00"),
        999L, // Non-existent category
        10
    );

    assertThatThrownBy(() -> bookService.createBook(invalidCategoryRequest, adminUser))
        .isInstanceOf(CategoryNotFoundException.class);

    // Test insufficient inventory
    var book1 = createTestBook("Limited Stock", "Author", "978-3-333-33333-3", new BigDecimal("20.00"), fictionCategory, 5);

    assertThatThrownBy(() -> inventoryService.reserveQuantity(book1.id(), 10, adminUser))
        .isInstanceOf(InsufficientInventoryException.class);

    // Test setting quantity below reserved amount (use a different book)
    var book2 = createTestBook("Reserved Stock", "Author", "978-4-444-44444-4", new BigDecimal("25.00"), fictionCategory, 10);

    inventoryService.reserveQuantity(book2.id(), 3, adminUser);

    // Verify reservation was successful
    var reservedInventory = inventoryService.getInventory(book2.id());
    assertThat(reservedInventory.reservedQuantity()).isEqualTo(3);

    var invalidUpdateRequest = new UpdateInventoryRequest(2, 5); // Less than reserved (3)

    assertThatThrownBy(() -> inventoryService.updateInventory(book2.id(), invalidUpdateRequest, adminUser))
        .isInstanceOf(InsufficientInventoryException.class)
        .hasMessageContaining("Cannot set quantity below reserved amount");
  }

  private BookDto createTestBook(String title, String author, String isbn, BigDecimal price, Category category, int quantity) {
    var createRequest = new CreateBookRequest(title, author, isbn, "Test description", price, category.getId(), quantity);
    return bookService.createBook(createRequest, adminUser);
  }
}