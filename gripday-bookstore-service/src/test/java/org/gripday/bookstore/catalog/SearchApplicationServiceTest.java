package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.gripday.bookstore.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SearchApplicationServiceTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private BookCatalogResponseBuilder responseBuilder;

  @InjectMocks
  private SearchApplicationService searchApplicationService;

  private Book testBook1;
  private Book testBook2;
  private Category testCategory;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setName("Fiction");
    testCategory.setDescription("Fiction books");

    testBook1 = new Book();
    testBook1.setId(1L);
    testBook1.setTitle("Java Programming");
    testBook1.setAuthor("John Doe");
    testBook1.setIsbn("9780134685991");
    testBook1.setDescription("A Java guide");
    testBook1.setPrice(new BigDecimal("39.99"));
    testBook1.setCategory(testCategory);
    testBook1.setAvailable(true);
    testBook1.setCreatedAt(LocalDateTime.now());
    testBook1.setUpdatedAt(LocalDateTime.now());

    var inventory1 = Inventory.create(testBook1, 15);
    inventory1.setId(1L);
    testBook1.setInventory(inventory1);

    testBook2 = new Book();
    testBook2.setId(2L);
    testBook2.setTitle("Spring Boot Guide");
    testBook2.setAuthor("Jane Smith");
    testBook2.setIsbn("9780596009205");
    testBook2.setDescription("Spring Boot development");
    testBook2.setPrice(new BigDecimal("29.99"));
    testBook2.setCategory(testCategory);
    testBook2.setAvailable(true);
    testBook2.setCreatedAt(LocalDateTime.now());
    testBook2.setUpdatedAt(LocalDateTime.now());

    var inventory2 = Inventory.create(testBook2, 8);
    inventory2.setId(2L);
    testBook2.setInventory(inventory2);

    pageable = PageRequest.of(0, 10);
  }

  @Test
  void searchByTitle_ShouldReturnMatchingBooks() {
    // Given
    var books = List.of(testBook1);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findByTitleContainingIgnoreCase("Java", pageable))
        .thenReturn(page);

    // When
    var result = searchApplicationService.searchByTitle("Java", pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).title()).contains("Java");

    verify(bookRepository).findByTitleContainingIgnoreCase("Java", pageable);
  }

  @Test
  void searchByAuthor_ShouldReturnMatchingBooks() {
    // Given
    var books = List.of(testBook1);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findByAuthorContainingIgnoreCase("John", pageable))
        .thenReturn(page);

    // When
    var result = searchApplicationService.searchByAuthor("John", pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).author()).contains("John");

    verify(bookRepository).findByAuthorContainingIgnoreCase("John", pageable);
  }

  @Test
  void searchByCategory_ShouldReturnMatchingBooks() {
    // Given
    var books = List.of(testBook1, testBook2);
    var page = new PageImpl<>(books, pageable, 2);

    when(bookRepository.findByCategoryName("Fiction", pageable))
        .thenReturn(page);

    // When
    var result = searchApplicationService.searchByCategory("Fiction", pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().get(0).categoryName()).isEqualTo("Fiction");

    verify(bookRepository).findByCategoryName("Fiction", pageable);
  }

  @Test
  void searchByPriceRange_ShouldReturnBooksInRange() {
    // Given
    var minPrice = new BigDecimal("25.00");
    var maxPrice = new BigDecimal("35.00");
    var books = List.of(testBook2);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findByPriceBetween(minPrice, maxPrice, pageable))
        .thenReturn(page);

    // When
    var result = searchApplicationService.searchByPriceRange(minPrice, maxPrice, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).price()).isBetween(minPrice, maxPrice);

    verify(bookRepository).findByPriceBetween(minPrice, maxPrice, pageable);
  }

  @Test
  void searchWithQuery_ShouldApplyAllFilters() {
    // Given
    var query = new BookSearchQuery(
        "Java", "John", "Fiction",
        new BigDecimal("30.00"), new BigDecimal("50.00"), true
    );
    var books = List.of(testBook1);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findBooksWithCriteria(
        "Java", "John", "Fiction",
        new BigDecimal("30.00"), new BigDecimal("50.00"),
        true, pageable
    )).thenReturn(page);

    // When
    var result = searchApplicationService.searchWithQuery(query, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).title()).contains("Java");

    verify(bookRepository).findBooksWithCriteria(
        "Java", "John", "Fiction",
        new BigDecimal("30.00"), new BigDecimal("50.00"),
        true, pageable
    );
  }

  @Test
  void searchAvailableBooksWithInventory_ShouldReturnAvailableBooks() {
    // Given
    var query = new BookSearchQuery(
        null, null, null, null, null, true
    );
    var books = List.of(testBook1, testBook2);
    var page = new PageImpl<>(books, pageable, 2);

    when(bookRepository.findBooksWithInventoryFilter(
        null, null, null, null, null, true, pageable
    )).thenReturn(page);

    // When
    var result = searchApplicationService.searchAvailableBooksWithInventory(query, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent()).allMatch(book -> book.available());

    verify(bookRepository).findBooksWithInventoryFilter(
        null, null, null, null, null, true, pageable
    );
  }

  @Test
  void findAffordableBooks_ShouldReturnBooksUnderMaxPrice() {
    // Given
    var maxPrice = new BigDecimal("35.00");
    var books = List.of(testBook2);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findAffordableBooks(maxPrice, pageable))
        .thenReturn(page);

    // When
    var result = searchApplicationService.findAffordableBooks(maxPrice, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).price()).isLessThanOrEqualTo(maxPrice);

    verify(bookRepository).findAffordableBooks(maxPrice, pageable);
  }

  @Test
  void findRecentBooks_ShouldReturnRecentlyAddedBooks() {
    // Given
    var books = List.of(testBook1, testBook2);
    var page = new PageImpl<>(books, pageable, 2);

    when(bookRepository.findRecentBooks(pageable)).thenReturn(page);

    // When
    var result = searchApplicationService.findRecentBooks(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);

    verify(bookRepository).findRecentBooks(pageable);
  }

  @Test
  void findAvailableBooksByCategory_ShouldReturnCategoryBooks() {
    // Given
    var categoryId = 1L;
    var books = List.of(testBook1, testBook2);
    var page = new PageImpl<>(books, pageable, 2);

    when(bookRepository.findAvailableBooksByCategory(categoryId, pageable))
        .thenReturn(page);

    // When
    var result = searchApplicationService.findAvailableBooksByCategory(categoryId, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent()).allMatch(book -> book.available());

    verify(bookRepository).findAvailableBooksByCategory(categoryId, pageable);
  }

  @Test
  void getDistinctAuthors_ShouldReturnAuthorList() {
    // Given
    var authors = List.of("John Doe", "Jane Smith");
    when(bookRepository.findDistinctAuthors()).thenReturn(authors);

    // When
    var result = searchApplicationService.getDistinctAuthors();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result).contains("John Doe", "Jane Smith");

    verify(bookRepository).findDistinctAuthors();
  }

  @Test
  void getDistinctCategories_ShouldReturnCategoryList() {
    // Given
    var categories = List.of("Fiction", "Non-Fiction");
    when(bookRepository.findDistinctCategoryNames()).thenReturn(categories);

    // When
    var result = searchApplicationService.getDistinctCategories();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result).contains("Fiction", "Non-Fiction");

    verify(bookRepository).findDistinctCategoryNames();
  }

  @Test
  void countAvailableBooks_ShouldReturnCount() {
    // Given
    when(bookRepository.countAvailableBooks()).thenReturn(25L);

    // When
    var result = searchApplicationService.countAvailableBooks();

    // Then
    assertThat(result).isEqualTo(25L);

    verify(bookRepository).countAvailableBooks();
  }

  @Test
  void countBooksByCategory_ShouldReturnCategoryCount() {
    // Given
    var categoryName = "Fiction";
    when(bookRepository.countBooksByCategory(categoryName)).thenReturn(15L);

    // When
    var result = searchApplicationService.countBooksByCategory(categoryName);

    // Then
    assertThat(result).isEqualTo(15L);

    verify(bookRepository).countBooksByCategory(categoryName);
  }
}
