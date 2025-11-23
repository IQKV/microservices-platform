package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.gripday.bookstore.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class BookRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private BookRepository bookRepository;

  private Category fictionCategory;
  private Category scienceCategory;
  private Book book1;
  private Book book2;
  private Book book3;
  private Inventory inventory1;
  private Inventory inventory2;
  private Inventory inventory3;

  @BeforeEach
  void setUp() {
    // Create categories
    fictionCategory = Category.create("Fiction", "Fiction books");
    scienceCategory = Category.create("Science", "Science books");
    entityManager.persistAndFlush(fictionCategory);
    entityManager.persistAndFlush(scienceCategory);

    // Create books
    book1 = Book.create("The Great Gatsby", "F. Scott Fitzgerald", "978-0-7432-7356-5", new BigDecimal("15.99"), "A classic American novel", fictionCategory);

    book2 = Book.create("To Kill a Mockingbird", "Harper Lee", "978-0-06-112008-4", new BigDecimal("12.50"), "A gripping tale of racial injustice", fictionCategory);

    book3 = Book.create("A Brief History of Time", "Stephen Hawking", "978-0-553-38016-3", new BigDecimal("18.99"), "Cosmology for the general reader", scienceCategory);
    book3.setAvailable(false); // Unavailable book for testing

    entityManager.persistAndFlush(book1);
    entityManager.persistAndFlush(book2);
    entityManager.persistAndFlush(book3);

    // Create inventory records
    inventory1 = new Inventory(book1, 10);
    inventory1.setLowStockThreshold(3);
    book1.setInventory(inventory1);

    inventory2 = new Inventory(book2, 2); // Low stock
    inventory2.setLowStockThreshold(5);
    book2.setInventory(inventory2);

    inventory3 = new Inventory(book3, 0); // Out of stock
    inventory3.setLowStockThreshold(5);
    book3.setInventory(inventory3);

    entityManager.persistAndFlush(inventory1);
    entityManager.persistAndFlush(inventory2);
    entityManager.persistAndFlush(inventory3);
    entityManager.clear();
  }

  @Test
  void findByTitleContainingIgnoreCase_ShouldReturnMatchingBooks() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findByTitleContainingIgnoreCase("great", pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Great Gatsby");
  }

  @Test
  void findByAuthorContainingIgnoreCase_ShouldReturnMatchingBooks() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findByAuthorContainingIgnoreCase("harper", pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getAuthor()).isEqualTo("Harper Lee");
  }

  @Test
  void findByCategoryName_ShouldReturnBooksInCategory() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findByCategoryName("Fiction", pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .extracting(Book::getTitle)
        .containsExactlyInAnyOrder("The Great Gatsby", "To Kill a Mockingbird");
  }

  @Test
  void findByPriceBetween_ShouldReturnBooksInPriceRange() {
    // Given
    var pageable = PageRequest.of(0, 10);
    var minPrice = new BigDecimal("12.00");
    var maxPrice = new BigDecimal("16.00");

    // When
    var result = bookRepository.findByPriceBetween(minPrice, maxPrice, pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .extracting(Book::getTitle)
        .containsExactlyInAnyOrder("The Great Gatsby", "To Kill a Mockingbird");
  }

  @Test
  void findAvailableBooks_ShouldReturnOnlyAvailableBooks() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findAvailableBooks(pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .allMatch(Book::isAvailable);
  }

  @Test
  void findBooksInStock_ShouldReturnBooksWithInventory() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findBooksInStock(pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .allMatch(book -> book.getInventory().getQuantity() > 0);
  }

  @Test
  void findBooksWithCriteria_ShouldReturnMatchingBooks() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When - Search by title and category
    var result = bookRepository.findBooksWithCriteria(
        "great", null, "Fiction", null, null, true, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Great Gatsby");
  }

  @Test
  void findBooksWithInventoryFilter_ShouldReturnBooksWithStock() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findBooksWithInventoryFilter(
        null, null, null, null, null, true, pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .allMatch(book -> book.getInventory().getQuantity() > 0);
  }

  @Test
  void findByIsbn_ShouldReturnBookWithMatchingIsbn() {
    // When
    var result = bookRepository.findByIsbn("978-0-7432-7356-5");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("The Great Gatsby");
  }

  @Test
  void findAvailableBooksByCategory_ShouldReturnAvailableBooksInCategory() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findAvailableBooksByCategory(fictionCategory.getId(), pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .allMatch(book -> book.getCategory().getName().equals("Fiction") && book.isAvailable());
  }

  @Test
  void findAffordableBooks_ShouldReturnBooksUnderMaxPrice() {
    // Given
    var pageable = PageRequest.of(0, 10);
    var maxPrice = new BigDecimal("16.00");

    // When
    var result = bookRepository.findAffordableBooks(maxPrice, pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .allMatch(book -> book.getPrice().compareTo(maxPrice) <= 0);
  }

  @Test
  void findRecentBooks_ShouldReturnBooksOrderedByCreationDate() {
    // Given
    var pageable = PageRequest.of(0, 10);

    // When
    var result = bookRepository.findRecentBooks(pageable);

    // Then
    assertThat(result.getContent()).hasSize(2); // Only available books
    assertThat(result.getContent())
        .allMatch(Book::isAvailable);
  }

  @Test
  void findLowStockBooks_ShouldReturnBooksWithLowInventory() {
    // When
    var result = bookRepository.findLowStockBooks();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("To Kill a Mockingbird");
  }

  @Test
  void countAvailableBooks_ShouldReturnCorrectCount() {
    // When
    var count = bookRepository.countAvailableBooks();

    // Then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void countBooksByCategory_ShouldReturnCorrectCount() {
    // When
    var count = bookRepository.countBooksByCategory("Fiction");

    // Then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void findDistinctAuthors_ShouldReturnUniqueAuthors() {
    // When
    var authors = bookRepository.findDistinctAuthors();

    // Then
    assertThat(authors).hasSize(2);
    assertThat(authors).containsExactlyInAnyOrder("F. Scott Fitzgerald", "Harper Lee");
  }

  @Test
  void findDistinctCategoryNames_ShouldReturnUniqueCategories() {
    // When
    var categories = bookRepository.findDistinctCategoryNames();

    // Then
    assertThat(categories).hasSize(1); // Only Fiction has available books
    assertThat(categories).contains("Fiction");
  }

  @Test
  void pagination_ShouldWorkCorrectly() {
    // Given
    var pageable = PageRequest.of(0, 1);

    // When
    var result = bookRepository.findAvailableBooks(pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getTotalPages()).isEqualTo(2);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  void relationshipMapping_ShouldLoadCategoryAndInventory() {
    // When
    var book = bookRepository.findByIsbn("978-0-7432-7356-5").orElseThrow();

    // Then
    assertThat(book.getCategory()).isNotNull();
    assertThat(book.getCategory().getName()).isEqualTo("Fiction");
    assertThat(book.getInventory()).isNotNull();
    assertThat(book.getInventory().getQuantity()).isEqualTo(10);
  }
}
