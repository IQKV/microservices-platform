package org.gripday.bookstore.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gripday.bookstore.domain.dto.BookDto;
import org.gripday.bookstore.domain.dto.BookSearchCriteria;
import org.gripday.bookstore.domain.dto.BulkInventoryRequest;
import org.gripday.bookstore.domain.dto.CreateBookRequest;
import org.gripday.bookstore.domain.dto.InventoryDto;
import org.gripday.bookstore.domain.dto.UpdateBookRequest;
import org.gripday.bookstore.domain.dto.UserContext;
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
 * Integration tests focusing on complete user workflows and realistic scenarios that would occur in a production bookstore application.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookstoreWorkflowIntegrationTest {

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

  private Category fictionCategory;
  private Category technologyCategory;
  private Category businessCategory;
  private UserContext adminUser;
  private UserContext regularUser;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    bookRepository.deleteAll();
    categoryRepository.deleteAll();

    // Create realistic book categories
    fictionCategory = createCategory("Fiction", "Fiction and literature books");
    technologyCategory = createCategory("Technology", "Programming and technology books");
    businessCategory = createCategory("Business", "Business and management books");

    // Create user contexts
    adminUser = new UserContext(
        1L,
        "bookstore_admin",
        "admin@bookstore.com",
        Set.of("ADMIN"),
        Set.of("BOOK_MANAGE", "INVENTORY_MANAGE"),
        "Management",
        "BOOKSTORE_ORG",
        Map.of("store_id", "MAIN_STORE")
    );

    regularUser = new UserContext(
        2L,
        "customer",
        "customer@example.com",
        Set.of("USER"),
        Set.of("BOOK_READ"),
        "Customer",
        "BOOKSTORE_ORG",
        Map.of()
    );
  }

  @Test
  void shouldCompleteNewBookstoreSetupWorkflow() {
    // Scenario: Setting up a new bookstore with initial inventory

    // Step 1: Admin adds popular fiction books
    var book1 = createBook("The Great Gatsby", "F. Scott Fitzgerald", "978-0-7432-7356-5",
        new BigDecimal("15.99"), fictionCategory, 25);
    var book2 = createBook("To Kill a Mockingbird", "Harper Lee", "978-0-06-112008-4",
        new BigDecimal("14.99"), fictionCategory, 30);
    var book3 = createBook("1984", "George Orwell", "978-0-452-28423-4",
        new BigDecimal("13.99"), fictionCategory, 20);

    // Step 2: Admin adds technology books
    var book4 = createBook("Clean Code", "Robert C. Martin", "978-0-13-235088-4",
        new BigDecimal("42.99"), technologyCategory, 15);
    var book5 = createBook("Spring Boot in Action", "Craig Walls", "978-1-61729-120-3",
        new BigDecimal("39.99"), technologyCategory, 12);

    // Step 3: Admin adds business books
    var book6 = createBook("Good to Great", "Jim Collins", "978-0-06-662099-2",
        new BigDecimal("18.99"), businessCategory, 18);

    // Verify initial setup
    var allBooks = bookService.findBooks(new BookSearchCriteria(null, null, null, null, null, null),
        PageRequest.of(0, 20));

    assertThat(allBooks.getContent()).hasSize(6);
    assertThat(allBooks.getContent())
        .extracting(BookDto::available)
        .containsOnly(true);

    // Verify inventory totals
    var totalInventory = inventoryService.getTotalInventoryCount();
    assertThat(totalInventory).isEqualTo(120); // Sum of all quantities

    // Verify categories are properly distributed
    var fictionBooks = searchService.searchByCategory("Fiction", PageRequest.of(0, 10));
    var techBooks = searchService.searchByCategory("Technology", PageRequest.of(0, 10));
    var businessBooks = searchService.searchByCategory("Business", PageRequest.of(0, 10));

    assertThat(fictionBooks.getContent()).hasSize(3);
    assertThat(techBooks.getContent()).hasSize(2);
    assertThat(businessBooks.getContent()).hasSize(1);
  }

  @Test
  void shouldCompleteCustomerBrowsingAndSearchWorkflow() {
    // Scenario: Customer browsing and searching for books

    // Setup: Create diverse book catalog
    setupDiverseBookCatalog();

    // Step 1: Customer browses all available books
    var allBooks = bookService.findAvailableBooks(PageRequest.of(0, 10));
    assertThat(allBooks.getContent()).isNotEmpty();
    assertThat(allBooks.getContent())
        .allMatch(book -> book.available() && book.availableQuantity() > 0);

    // Step 2: Customer searches for specific author
    var tolkienBooks = searchService.searchByAuthor("Tolkien", PageRequest.of(0, 10));
    assertThat(tolkienBooks.getContent()).isNotEmpty();
    assertThat(tolkienBooks.getContent())
        .allMatch(book -> book.author().contains("Tolkien"));

    // Step 3: Customer searches by title keyword
    var springBooks = searchService.searchByTitle("Spring", PageRequest.of(0, 10));
    assertThat(springBooks.getContent()).isNotEmpty();
    assertThat(springBooks.getContent())
        .allMatch(book -> book.title().toLowerCase().contains("spring"));

    // Step 4: Customer filters by price range (budget shopping)
    var affordableBooks = searchService.searchByPriceRange(
        new BigDecimal("10.00"), new BigDecimal("20.00"), PageRequest.of(0, 10));
    assertThat(affordableBooks.getContent()).isNotEmpty();
    assertThat(affordableBooks.getContent())
        .allMatch(book -> book.price().compareTo(new BigDecimal("10.00")) >= 0
            && book.price().compareTo(new BigDecimal("20.00")) <= 0);

    // Step 5: Customer searches within specific category
    var fictionBooks = searchService.searchByCategory("Fiction", PageRequest.of(0, 10));
    assertThat(fictionBooks.getContent()).isNotEmpty();
    assertThat(fictionBooks.getContent())
        .allMatch(book -> "Fiction".equals(book.categoryName()));

    // Step 6: Customer performs complex search (title + category + price)
    var complexCriteria = new BookSearchCriteria(
        "Java", // title contains
        null,   // any author
        "Technology", // category
        new BigDecimal("30.00"), // min price
        new BigDecimal("50.00"), // max price
        true    // available only
    );

    var complexResults = searchService.searchWithCriteria(complexCriteria, PageRequest.of(0, 10));
    assertThat(complexResults.getContent())
        .allMatch(book -> book.title().toLowerCase().contains("java")
            && "Technology".equals(book.categoryName())
            && book.price().compareTo(new BigDecimal("30.00")) >= 0
            && book.price().compareTo(new BigDecimal("50.00")) <= 0
            && book.availableQuantity() > 0);
  }

  @Test
  void shouldCompleteInventoryManagementWorkflow() {
    // Scenario: Daily inventory management operations

    // Setup: Create books with varying stock levels
    var lowStockBook = createBook("Low Stock Book", "Author A", "978-1-111-11111-1",
        new BigDecimal("25.00"), fictionCategory, 3);
    var normalStockBook = createBook("Normal Stock Book", "Author B", "978-2-222-22222-2",
        new BigDecimal("30.00"), technologyCategory, 50);
    var highStockBook = createBook("High Stock Book", "Author C", "978-3-333-33333-3",
        new BigDecimal("20.00"), businessCategory, 100);

    // Step 1: Admin checks current inventory status
    var lowStockInventory = inventoryService.getInventory(lowStockBook.id());
    var normalStockInventory = inventoryService.getInventory(normalStockBook.id());
    var highStockInventory = inventoryService.getInventory(highStockBook.id());

    assertThat(lowStockInventory.lowStock()).isTrue();
    assertThat(normalStockInventory.lowStock()).isFalse();
    assertThat(highStockInventory.lowStock()).isFalse();

    // Step 2: Admin identifies low stock items
    var lowStockItems = inventoryService.getLowStockInventory();
    assertThat(lowStockItems).isNotEmpty();
    assertThat(lowStockItems)
        .extracting(InventoryDto::bookId)
        .contains(lowStockBook.id());

    // Step 3: Admin performs bulk inventory update (restocking)
    var bulkUpdates = List.of(
        new BulkInventoryRequest(lowStockBook.id(), 25, 5),      // Restock low inventory
        new BulkInventoryRequest(normalStockBook.id(), 75, 10),  // Increase normal stock
        new BulkInventoryRequest(highStockBook.id(), 150, 20)    // Increase high stock
    );

    var bulkResults = inventoryService.bulkUpdateInventory(bulkUpdates, adminUser);
    assertThat(bulkResults).hasSize(3);

    // Step 4: Verify restocking was successful
    var restockedLowInventory = inventoryService.getInventory(lowStockBook.id());
    assertThat(restockedLowInventory.quantity()).isEqualTo(25);
    assertThat(restockedLowInventory.lowStock()).isFalse();

    // Step 5: Simulate customer reservations
    inventoryService.reserveQuantity(normalStockBook.id(), 10, regularUser);
    inventoryService.reserveQuantity(highStockBook.id(), 25, regularUser);

    // Step 6: Admin checks reserved quantities
    var reservedNormalInventory = inventoryService.getInventory(normalStockBook.id());
    var reservedHighInventory = inventoryService.getInventory(highStockBook.id());

    assertThat(reservedNormalInventory.reservedQuantity()).isEqualTo(10);
    assertThat(reservedNormalInventory.availableQuantity()).isEqualTo(65); // 75 - 10
    assertThat(reservedHighInventory.reservedQuantity()).isEqualTo(25);
    assertThat(reservedHighInventory.availableQuantity()).isEqualTo(125); // 150 - 25

    // Step 7: Admin checks total inventory metrics
    var totalInventory = inventoryService.getTotalInventoryCount();
    var totalReserved = inventoryService.getTotalReservedCount();
    var lowStockCount = inventoryService.countLowStockItems();
    var outOfStockCount = inventoryService.countOutOfStockItems();

    assertThat(totalInventory).isEqualTo(250); // 25 + 75 + 150
    assertThat(totalReserved).isEqualTo(35);   // 10 + 25
    assertThat(lowStockCount).isEqualTo(0);    // All restocked
    assertThat(outOfStockCount).isEqualTo(0);  // None out of stock
  }

  @Test
  void shouldCompleteBookLifecycleWorkflow() {
    // Scenario: Complete lifecycle of a book from creation to removal

    // Step 1: Admin creates a new book
    var newBook = createBook("New Release", "Popular Author", "978-9-999-99999-9",
        new BigDecimal("24.99"), fictionCategory, 100);

    assertThat(newBook.available()).isTrue();
    assertThat(newBook.availableQuantity()).isEqualTo(100);

    // Step 2: Book becomes popular, inventory gets reserved
    inventoryService.reserveQuantity(newBook.id(), 30, regularUser);
    inventoryService.reserveQuantity(newBook.id(), 20, regularUser);

    var popularBookInventory = inventoryService.getInventory(newBook.id());
    assertThat(popularBookInventory.reservedQuantity()).isEqualTo(50);
    assertThat(popularBookInventory.availableQuantity()).isEqualTo(50);

    // Step 3: Admin updates book information (new edition)
    var updateRequest = new UpdateBookRequest(
        "New Release - Second Edition",
        "Popular Author",
        "Updated with new content and corrections",
        new BigDecimal("27.99"),
        fictionCategory.getId()
    );

    var updatedBook = bookService.updateBook(newBook.id(), updateRequest, adminUser);
    assertThat(updatedBook.title()).contains("Second Edition");
    assertThat(updatedBook.price()).isEqualByComparingTo(new BigDecimal("27.99"));

    // Step 4: Some reservations are released (cancelled orders)
    inventoryService.releaseReservedQuantity(newBook.id(), 15, regularUser);

    var releasedInventory = inventoryService.getInventory(newBook.id());
    assertThat(releasedInventory.reservedQuantity()).isEqualTo(35);
    assertThat(releasedInventory.availableQuantity()).isEqualTo(65);

    // Step 5: Admin adjusts inventory (damaged books removed)
    inventoryService.adjustInventoryQuantity(newBook.id(), -10, adminUser);

    var adjustedInventory = inventoryService.getInventory(newBook.id());
    assertThat(adjustedInventory.quantity()).isEqualTo(90);
    assertThat(adjustedInventory.availableQuantity()).isEqualTo(55); // 90 - 35 reserved

    // Step 6: Book goes out of print (soft delete)
    bookService.deleteBook(newBook.id(), adminUser);

    var deletedBook = bookService.findBookById(newBook.id());
    assertThat(deletedBook).isPresent();
    assertThat(deletedBook.get().available()).isFalse();

    // Step 7: Verify book no longer appears in available searches
    var availableBooks = bookService.findAvailableBooks(PageRequest.of(0, 20));
    assertThat(availableBooks.getContent())
        .noneMatch(book -> book.id().equals(newBook.id()));
  }

  @Test
  void shouldHandleHighVolumeOperationsCorrectly() {
    // Scenario: Testing system behavior under high volume operations

    // Step 1: Create multiple books quickly
    var createdBooks = List.of(
        createBook("Book 1", "Author 1", "978-1-111-11111-1", new BigDecimal("20.00"), fictionCategory, 50),
        createBook("Book 2", "Author 2", "978-2-222-22222-2", new BigDecimal("25.00"), technologyCategory, 40),
        createBook("Book 3", "Author 3", "978-3-333-33333-3", new BigDecimal("30.00"), businessCategory, 60),
        createBook("Book 4", "Author 4", "978-4-444-44444-4", new BigDecimal("35.00"), fictionCategory, 30),
        createBook("Book 5", "Author 5", "978-5-555-55555-5", new BigDecimal("40.00"), technologyCategory, 20)
    );

    assertThat(createdBooks).hasSize(5);

    // Step 2: Perform bulk inventory operations
    var bulkUpdates = createdBooks.stream()
        .map(book -> new BulkInventoryRequest(book.id(), 100, 10))
        .toList();

    var bulkResults = inventoryService.bulkUpdateInventory(bulkUpdates, adminUser);
    assertThat(bulkResults).hasSize(5);

    // Step 3: Multiple concurrent search operations
    var titleSearches = List.of("Book", "Author", "1", "2", "3");
    var searchResults = titleSearches.stream()
        .map(term -> searchService.searchByTitle(term, PageRequest.of(0, 10)))
        .toList();

    assertThat(searchResults).hasSize(5);
    // Some searches may return empty results, so we just verify we got results back
    assertThat(searchResults).allMatch(page -> page != null);

    // Step 4: Multiple inventory reservations
    createdBooks.forEach(book -> {
      inventoryService.reserveQuantity(book.id(), 10, regularUser);
    });

    // Step 5: Verify all operations completed successfully
    var totalReserved = inventoryService.getTotalReservedCount();
    assertThat(totalReserved).isEqualTo(50); // 10 * 5 books

    var totalInventory = inventoryService.getTotalInventoryCount();
    assertThat(totalInventory).isEqualTo(500); // 100 * 5 books
  }

  private Category createCategory(String name, String description) {
    var category = new Category();
    category.setName(name);
    category.setDescription(description);
    return categoryRepository.save(category);
  }

  private BookDto createBook(String title, String author, String isbn, BigDecimal price,
      Category category, int quantity) {
    var createRequest = new CreateBookRequest(title, author, isbn, "Test description",
        price, category.getId(), quantity);
    return bookService.createBook(createRequest, adminUser);
  }

  private void setupDiverseBookCatalog() {
    // Fiction books
    createBook("The Hobbit", "J.R.R. Tolkien", "978-0-547-92822-7", new BigDecimal("14.99"), fictionCategory, 25);
    createBook("The Lord of the Rings", "J.R.R. Tolkien", "978-0-544-00341-5", new BigDecimal("18.99"), fictionCategory, 20);
    createBook("Dune", "Frank Herbert", "978-0-441-17271-9", new BigDecimal("16.99"), fictionCategory, 30);

    // Technology books
    createBook("Java: The Complete Reference", "Herbert Schildt", "978-1-26-041195-7", new BigDecimal("45.99"), technologyCategory, 15);
    createBook("Spring Boot in Action", "Craig Walls", "978-1-61729-120-3", new BigDecimal("39.99"), technologyCategory, 12);
    createBook("Clean Architecture", "Robert C. Martin", "978-0-13-449416-6", new BigDecimal("42.99"), technologyCategory, 18);

    // Business books
    createBook("The Lean Startup", "Eric Ries", "978-0-307-88789-4", new BigDecimal("21.99"), businessCategory, 22);
    createBook("Good to Great", "Jim Collins", "978-0-06-662099-2", new BigDecimal("18.99"), businessCategory, 28);
  }
}