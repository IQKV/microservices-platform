package com.iqscaffold.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iqscaffold.bookstore.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private CategoryRepository categoryRepository;

  private Category fictionCategory;
  private Category scienceCategory;
  private Category emptyCategory;
  private Book book1;
  private Book book2;
  private Book book3;

  @BeforeEach
  void setUp() {
    // Create categories
    fictionCategory = Category.create("Fiction", "Fiction books");
    scienceCategory = Category.create("Science", "Science books");
    emptyCategory = Category.create("Empty", "Category with no books");

    entityManager.persistAndFlush(fictionCategory);
    entityManager.persistAndFlush(scienceCategory);
    entityManager.persistAndFlush(emptyCategory);

    // Create books
    book1 = Book.create("The Great Gatsby", "F. Scott Fitzgerald", "9780134685991", new BigDecimal("15.99"), null, fictionCategory);

    book2 = Book.create("To Kill a Mockingbird", "Harper Lee", "9780596009205", new BigDecimal("12.50"), null, fictionCategory);

    book3 = Book.create("A Brief History of Time", "Stephen Hawking", "9781617294945", new BigDecimal("18.99"), null, scienceCategory);
    book3.setAvailable(false); // Unavailable book

    entityManager.persistAndFlush(book1);
    entityManager.persistAndFlush(book2);
    entityManager.persistAndFlush(book3);

    // Create inventory with low stock for testing
    var inventory1 = Inventory.create(book1, 10);
    var inventory2 = Inventory.create(book2, 2); // Low stock
    inventory2.setLowStockThreshold(5);
    var inventory3 = Inventory.create(book3, 15);

    entityManager.persistAndFlush(inventory1);
    entityManager.persistAndFlush(inventory2);
    entityManager.persistAndFlush(inventory3);
    entityManager.clear();
  }

  @Test
  void findByNameIgnoreCase_ShouldReturnCategoryWithMatchingName() {
    // When
    var result = categoryRepository.findByNameIgnoreCase("fiction");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Fiction");
  }

  @Test
  void findByNameIgnoreCase_ShouldReturnEmptyForNonExistentCategory() {
    // When
    var result = categoryRepository.findByNameIgnoreCase("NonExistent");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void existsByNameIgnoreCase_ShouldReturnTrueForExistingCategory() {
    // When
    var exists = categoryRepository.existsByNameIgnoreCase("FICTION");

    // Then
    assertThat(exists).isTrue();
  }

  @Test
  void existsByNameIgnoreCase_ShouldReturnFalseForNonExistentCategory() {
    // When
    var exists = categoryRepository.existsByNameIgnoreCase("NonExistent");

    // Then
    assertThat(exists).isFalse();
  }

  @Test
  void findCategoriesWithAvailableBooks_ShouldReturnOnlyCategoriesWithAvailableBooks() {
    // When
    var result = categoryRepository.findCategoriesWithAvailableBooks();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Fiction");
  }

  @Test
  void findByNameContainingIgnoreCase_ShouldReturnMatchingCategories() {
    // When
    var result = categoryRepository.findByNameContainingIgnoreCase("sci");

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Science");
  }

  @Test
  void findCategoriesOrderedByBookCount_ShouldReturnCategoriesWithBooks() {
    // When
    var result = categoryRepository.findCategoriesOrderedByBookCount();

    // Then
    assertThat(result).hasSize(2); // Fiction and Science have books
    assertThat(result.get(0).getName()).isEqualTo("Fiction"); // Should have more books
  }

  @Test
  void findEmptyCategories_ShouldReturnCategoriesWithoutBooks() {
    // When
    var result = categoryRepository.findEmptyCategories();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Empty");
  }

  @Test
  void countAvailableBooksInCategory_ShouldReturnCorrectCount() {
    // When
    var count = categoryRepository.countAvailableBooksInCategory(fictionCategory.getId());

    // Then
    assertThat(count).isEqualTo(2);
  }

  @Test
  void countAvailableBooksInCategory_ShouldReturnZeroForEmptyCategory() {
    // When
    var count = categoryRepository.countAvailableBooksInCategory(emptyCategory.getId());

    // Then
    assertThat(count).isEqualTo(0);
  }

  @Test
  void findCategoriesWithLowStockBooks_ShouldReturnCategoriesWithLowStock() {
    // When
    var result = categoryRepository.findCategoriesWithLowStockBooks();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Fiction");
  }

  @Test
  void findAllByOrderByNameAsc_ShouldReturnCategoriesInAlphabeticalOrder() {
    // When
    var result = categoryRepository.findAllByOrderByNameAsc();

    // Then
    assertThat(result).hasSize(3);
    assertThat(result)
        .extracting(Category::getName)
        .containsExactly("Empty", "Fiction", "Science");
  }

  @Test
  void findRecentCategories_ShouldReturnCategoriesCreatedAfterDate() {
    // Given
    var startDate = LocalDateTime.now().minusHours(1);

    // When
    var result = categoryRepository.findRecentCategories(startDate);

    // Then
    assertThat(result).hasSize(3); // All categories were created recently
  }

  @Test
  void bidirectionalRelationship_ShouldWorkCorrectly() {
    // When
    var category = categoryRepository.findByNameIgnoreCase("Fiction").orElseThrow();

    // Then
    assertThat(category.getBooks()).hasSize(2);
    assertThat(category.getBooks())
        .extracting(Book::getTitle)
        .containsExactlyInAnyOrder("The Great Gatsby", "To Kill a Mockingbird");
  }

  @Test
  void cascadeOperations_ShouldWorkCorrectly() {
    // Given
    var newCategory = Category.create("Mystery", "Mystery novels");
    var newBook = Book.create("The Maltese Falcon", "Dashiell Hammett", "9780679722649", new BigDecimal("14.99"), null, null);

    // When
    newCategory.addBook(newBook);
    var savedCategory = categoryRepository.save(newCategory);
    entityManager.flush();
    entityManager.clear();

    // Then
    var retrievedCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();
    assertThat(retrievedCategory.getBooks()).hasSize(1);
    assertThat(retrievedCategory.getBooks().get(0).getTitle()).isEqualTo("The Maltese Falcon");
    assertThat(retrievedCategory.getBooks().get(0).getCategory()).isEqualTo(retrievedCategory);
  }

  @Test
  void helperMethods_ShouldMaintainBidirectionalRelationship() {
    // Given
    var category = categoryRepository.findByNameIgnoreCase("Fiction").orElseThrow();
    var newBook = Book.create("1984", "George Orwell", "9780452284234", new BigDecimal("13.99"), null, null);

    var initialBookCount = category.getBooks().size();

    // When
    category.addBook(newBook);
    entityManager.persistAndFlush(newBook);

    // Then
    assertThat(category.getBooks()).hasSize(initialBookCount + 1);
    assertThat(newBook.getCategory()).isEqualTo(category);
  }

  @Test
  void removeBook_ShouldMaintainBidirectionalRelationship() {
    // Given
    var category = categoryRepository.findByNameIgnoreCase("Fiction").orElseThrow();
    var bookToRemove = category.getBooks().get(0);
    var initialBookCount = category.getBooks().size();

    // When
    category.removeBook(bookToRemove);
    entityManager.flush();

    // Then
    assertThat(category.getBooks()).hasSize(initialBookCount - 1);
    assertThat(bookToRemove.getCategory()).isNull();
  }
}
