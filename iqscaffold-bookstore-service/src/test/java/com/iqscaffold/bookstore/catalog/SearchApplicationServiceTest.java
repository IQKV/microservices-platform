package com.iqscaffold.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.iqscaffold.bookstore.inventory.Inventory;
import com.iqscaffold.bookstore.shared.ISBN;
import com.iqscaffold.bookstore.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchApplicationService Tests")
class SearchApplicationServiceTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private BookCatalogResponseBuilder responseBuilder;

  @Mock
  private BookCatalogResponse catalogResponse;

  private SearchApplicationService searchService;

  private Book testBook;
  private Category testCategory;
  private Inventory testInventory;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    searchService = new SearchApplicationService(bookRepository, responseBuilder);
    pageable = PageRequest.of(0, 10);

    testCategory = Category.create("Fiction", "Fiction books");
    testCategory.setId(1L);

    testBook = Book.create(
        "Test Book",
        "Test Author",
        ISBN.of("978-0-7432-7356-5"),
        Money.usd(new BigDecimal("19.99")),
        "Test Description",
        testCategory
    );
    testBook.setId(1L);
    testBook.setCreatedAt(LocalDateTime.now());
    testBook.setUpdatedAt(LocalDateTime.now());

    testInventory = Inventory.create(testBook, 10, 0);
    testBook.setInventory(testInventory);
  }

  @Test
  @DisplayName("Should search by title")
  void shouldSearchByTitle() {
    // Arrange
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByTitleContainingIgnoreCase(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByTitle("Test", pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(bookRepository).findByTitleContainingIgnoreCase("Test", pageable);
  }

  @Test
  @DisplayName("Should search by author")
  void shouldSearchByAuthor() {
    // Arrange
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByAuthorContainingIgnoreCase(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByAuthor("Author", pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(bookRepository).findByAuthorContainingIgnoreCase("Author", pageable);
  }

  @Test
  @DisplayName("Should search by category")
  void shouldSearchByCategory() {
    // Arrange
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByCategoryName(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByCategory("Fiction", pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(bookRepository).findByCategoryName("Fiction", pageable);
  }

  @Test
  @DisplayName("Should search by price range with valid range")
  void shouldSearchByPriceRangeWithValidRange() {
    // Arrange
    var minPrice = new BigDecimal("10.00");
    var maxPrice = new BigDecimal("30.00");
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByPriceBetween(any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByPriceRange(minPrice, maxPrice, pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(bookRepository).findByPriceBetween(minPrice, maxPrice, pageable);
  }

  @Test
  @DisplayName("Should throw exception when min price greater than max price")
  void shouldThrowExceptionWhenMinPriceGreaterThanMaxPrice() {
    // Arrange
    var minPrice = new BigDecimal("30.00");
    var maxPrice = new BigDecimal("10.00");

    // Act & Assert
    assertThatThrownBy(() -> searchService.searchByPriceRange(minPrice, maxPrice, pageable))
        .isInstanceOf(InvalidPriceRangeException.class);
  }

  @Test
  @DisplayName("Should search by price range with null min price")
  void shouldSearchByPriceRangeWithNullMinPrice() {
    // Arrange
    var maxPrice = new BigDecimal("30.00");
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByPriceBetween(any(), any(BigDecimal.class), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByPriceRange(null, maxPrice, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findByPriceBetween(null, maxPrice, pageable);
  }

  @Test
  @DisplayName("Should search by price range with null max price")
  void shouldSearchByPriceRangeWithNullMaxPrice() {
    // Arrange
    var minPrice = new BigDecimal("10.00");
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByPriceBetween(any(BigDecimal.class), any(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByPriceRange(minPrice, null, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findByPriceBetween(minPrice, null, pageable);
  }

  @Test
  @DisplayName("Should search with query when availableOnly is null")
  void shouldSearchWithQueryWhenAvailableOnlyIsNull() {
    // Arrange
    var query = new BookSearchQuery("Test", "Author", "Fiction", null, null, null);
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findBooksWithCriteria(
        anyString(), anyString(), anyString(), any(), any(), eq(false), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchWithQuery(query, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findBooksWithCriteria(
        "Test", "Author", "Fiction", null, null, false, pageable);
  }

  @Test
  @DisplayName("Should search with query when availableOnly is true")
  void shouldSearchWithQueryWhenAvailableOnlyIsTrue() {
    // Arrange
    var query = new BookSearchQuery("Test", "Author", "Fiction", null, null, true);
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findBooksWithCriteria(
        anyString(), anyString(), anyString(), any(), any(), eq(true), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchWithQuery(query, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findBooksWithCriteria(
        "Test", "Author", "Fiction", null, null, true, pageable);
  }

  @Test
  @DisplayName("Should search available books with inventory when availableOnly is null")
  void shouldSearchAvailableBooksWithInventoryWhenAvailableOnlyIsNull() {
    // Arrange
    var query = new BookSearchQuery("Test", null, null, null, null, null);
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findBooksWithInventoryFilter(
        anyString(), any(), any(), any(), any(), eq(true), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchAvailableBooksWithInventory(query, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findBooksWithInventoryFilter(
        "Test", null, null, null, null, true, pageable);
  }

  @Test
  @DisplayName("Should search available books with inventory when availableOnly is false")
  void shouldSearchAvailableBooksWithInventoryWhenAvailableOnlyIsFalse() {
    // Arrange
    var query = new BookSearchQuery("Test", null, null, null, null, false);
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findBooksWithInventoryFilter(
        anyString(), any(), any(), any(), any(), eq(false), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchAvailableBooksWithInventory(query, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findBooksWithInventoryFilter(
        "Test", null, null, null, null, false, pageable);
  }

  @Test
  @DisplayName("Should find affordable books")
  void shouldFindAffordableBooks() {
    // Arrange
    var maxPrice = new BigDecimal("25.00");
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findAffordableBooks(any(BigDecimal.class), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.findAffordableBooks(maxPrice, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findAffordableBooks(maxPrice, pageable);
  }

  @Test
  @DisplayName("Should find recent books")
  void shouldFindRecentBooks() {
    // Arrange
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findRecentBooks(any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.findRecentBooks(pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findRecentBooks(pageable);
  }

  @Test
  @DisplayName("Should find available books by category")
  void shouldFindAvailableBooksByCategory() {
    // Arrange
    var categoryId = 1L;
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findAvailableBooksByCategory(anyLong(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.findAvailableBooksByCategory(categoryId, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findAvailableBooksByCategory(categoryId, pageable);
  }

  @Test
  @DisplayName("Should get distinct authors")
  void shouldGetDistinctAuthors() {
    // Arrange
    var authors = List.of("Author 1", "Author 2");
    when(bookRepository.findDistinctAuthors()).thenReturn(authors);

    // Act
    var result = searchService.getDistinctAuthors();

    // Assert
    assertThat(result).hasSize(2);
    verify(bookRepository).findDistinctAuthors();
  }

  @Test
  @DisplayName("Should get distinct categories")
  void shouldGetDistinctCategories() {
    // Arrange
    var categories = List.of("Fiction", "Non-Fiction");
    when(bookRepository.findDistinctCategoryNames()).thenReturn(categories);

    // Act
    var result = searchService.getDistinctCategories();

    // Assert
    assertThat(result).hasSize(2);
    verify(bookRepository).findDistinctCategoryNames();
  }

  @Test
  @DisplayName("Should count available books")
  void shouldCountAvailableBooks() {
    // Arrange
    when(bookRepository.countAvailableBooks()).thenReturn(10L);

    // Act
    var result = searchService.countAvailableBooks();

    // Assert
    assertThat(result).isEqualTo(10L);
    verify(bookRepository).countAvailableBooks();
  }

  @Test
  @DisplayName("Should count books by category")
  void shouldCountBooksByCategory() {
    // Arrange
    when(bookRepository.countBooksByCategory(anyString())).thenReturn(5L);

    // Act
    var result = searchService.countBooksByCategory("Fiction");

    // Assert
    assertThat(result).isEqualTo(5L);
    verify(bookRepository).countBooksByCategory("Fiction");
  }

  @Test
  @DisplayName("Should perform full text search")
  void shouldPerformFullTextSearch() {
    // Arrange
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByFullTextSearch(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.fullTextSearch("search term", pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findByFullTextSearch("search term", pageable);
  }

  @Test
  @DisplayName("Should perform fuzzy search")
  void shouldPerformFuzzySearch() {
    // Arrange
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByFuzzySearch(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.fuzzySearch("fuzzy term", pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findByFuzzySearch("fuzzy term", pageable);
  }

  @Test
  @DisplayName("Should search by price range and category with valid range")
  void shouldSearchByPriceRangeAndCategoryWithValidRange() {
    // Arrange
    var minPrice = new BigDecimal("10.00");
    var maxPrice = new BigDecimal("30.00");
    var categoryId = 1L;
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByPriceRangeAndCategory(
        any(BigDecimal.class), any(BigDecimal.class), anyLong(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByPriceRangeAndCategory(minPrice, maxPrice, categoryId, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(bookRepository).findByPriceRangeAndCategory(minPrice, maxPrice, categoryId, pageable);
  }

  @Test
  @DisplayName("Should throw exception in searchByPriceRangeAndCategory when min price greater than max")
  void shouldThrowExceptionInSearchByPriceRangeAndCategoryWhenMinGreaterThanMax() {
    // Arrange
    var minPrice = new BigDecimal("30.00");
    var maxPrice = new BigDecimal("10.00");
    var categoryId = 1L;

    // Act & Assert
    assertThatThrownBy(() ->
        searchService.searchByPriceRangeAndCategory(minPrice, maxPrice, categoryId, pageable))
        .isInstanceOf(InvalidPriceRangeException.class);
  }

  @Test
  @DisplayName("Should check book availability with quantity")
  void shouldCheckBookAvailabilityWithQuantity() {
    // Arrange
    when(bookRepository.isBookAvailableWithQuantity(anyLong(), any(int.class)))
        .thenReturn(true);

    // Act
    var result = searchService.isBookAvailableWithQuantity(1L, 5);

    // Assert
    assertThat(result).isTrue();
    verify(bookRepository).isBookAvailableWithQuantity(1L, 5);
  }

  @Test
  @DisplayName("Should search with catalog response")
  void shouldSearchWithCatalogResponse() {
    // Arrange
    var query = new BookSearchQuery("Test", null, null, null, null, null);
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findBooksWithCriteria(
        anyString(), any(), any(), any(), any(), anyBoolean(), any(Pageable.class)))
        .thenReturn(books);
    when(responseBuilder.build(any(Page.class), any(BookSearchQuery.class)))
        .thenReturn(catalogResponse);

    // Act
    var result = searchService.searchWithCatalogResponse(query, pageable);

    // Assert
    assertThat(result).isNotNull();
    verify(responseBuilder).build(any(Page.class), eq(query));
  }

  @Test
  @DisplayName("Should convert book to DTO with inventory")
  void shouldConvertBookToDtoWithInventory() {
    // Arrange
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByTitleContainingIgnoreCase(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByTitle("Test", pageable);

    // Assert
    assertThat(result.getContent()).hasSize(1);
    var dto = result.getContent().get(0);
    assertThat(dto.availableQuantity()).isEqualTo(10);
    assertThat(dto.categoryName()).isEqualTo("Fiction");
  }

  @Test
  @DisplayName("Should convert book to DTO with null inventory")
  void shouldConvertBookToDtoWithNullInventory() {
    // Arrange
    testBook.setInventory(null);
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByTitleContainingIgnoreCase(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByTitle("Test", pageable);

    // Assert
    assertThat(result.getContent()).hasSize(1);
    var dto = result.getContent().get(0);
    assertThat(dto.availableQuantity()).isZero();
  }

  @Test
  @DisplayName("Should convert book to DTO with null category")
  void shouldConvertBookToDtoWithNullCategory() {
    // Arrange
    testBook.setCategory(null);
    var books = new PageImpl<>(List.of(testBook));
    when(bookRepository.findByTitleContainingIgnoreCase(anyString(), any(Pageable.class)))
        .thenReturn(books);

    // Act
    var result = searchService.searchByTitle("Test", pageable);

    // Assert
    assertThat(result.getContent()).hasSize(1);
    var dto = result.getContent().get(0);
    assertThat(dto.categoryName()).isNull();
  }
}
