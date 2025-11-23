package org.gripday.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gripday.bookstore.inventory.Inventory;
import org.gripday.bookstore.shared.UserContext;
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
class CatalogApplicationServiceTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private org.gripday.bookstore.shared.AuditLogger auditLogger;

  @Mock
  private org.gripday.bookstore.shared.BookstoreMetrics bookstoreMetrics;

  @Mock
  private DuplicateIsbnChecker duplicateIsbnChecker;

  @Mock
  private BookCatalogResponseBuilder responseBuilder;

  @InjectMocks
  private CatalogApplicationService catalogApplicationService;

  private UserContext adminUser;
  private UserContext regularUser;
  private Book testBook;
  private Category testCategory;
  private Inventory testInventory;
  private CreateBookCommand createBookCommand;
  private UpdateBookCommand updateBookCommand;
  private BookSearchQuery searchQuery;

  @BeforeEach
  void setUp() {
    adminUser = new UserContext(
        1L, "admin", "admin@test.com",
        Set.of("ADMIN"), Set.of(),
        "IT", "org1", Map.of()
    );

    regularUser = new UserContext(
        2L, "user", "user@test.com",
        Set.of("USER"), Set.of(),
        "Sales", "org1", Map.of()
    );

    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setName("Fiction");
    testCategory.setDescription("Fiction books");

    testBook = new Book();
    testBook.setId(1L);
    testBook.setTitle("Test Book");
    testBook.setAuthor("Test Author");
    testBook.setIsbn("9780134685991");
    testBook.setDescription("A test book");
    testBook.setPrice(new BigDecimal("29.99"));
    testBook.setCategory(testCategory);
    testBook.setAvailable(true);
    testBook.setCreatedAt(LocalDateTime.now());
    testBook.setUpdatedAt(LocalDateTime.now());

    testInventory = Inventory.create(testBook, 10);
    testInventory.setId(1L);
    testBook.setInventory(testInventory);

    createBookCommand = new CreateBookCommand(
        "New Book", "New Author", "9780596009205",
        "A new book", new BigDecimal("39.99"), 1L, 5
    );

    updateBookCommand = new UpdateBookCommand(
        "Updated Book", "Updated Author", "Updated description",
        new BigDecimal("49.99"), 1L
    );

    searchQuery = new BookSearchQuery(
        "Test", "Author", "Fiction",
        new BigDecimal("10.00"), new BigDecimal("50.00"), true
    );

    // Setup metrics mocks (lenient to avoid UnnecessaryStubbingException)
    org.mockito.Mockito.lenient().when(bookstoreMetrics.startBookSearchTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
    org.mockito.Mockito.lenient().when(bookstoreMetrics.startBookCreationTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));
  }

  @Test
  void findBooks_ShouldReturnPagedResults() {
    // Given
    var pageable = PageRequest.of(0, 10);
    var books = List.of(testBook);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findBooksWithInventoryFilter(
        anyString(), anyString(), anyString(),
        any(BigDecimal.class), any(BigDecimal.class),
        anyBoolean(), any(Pageable.class)
    )).thenReturn(page);

    // When
    var result = catalogApplicationService.findBooks(searchQuery, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).title()).isEqualTo("Test Book");

    verify(bookRepository).findBooksWithInventoryFilter(
        "Test", "Author", "Fiction",
        new BigDecimal("10.00"), new BigDecimal("50.00"),
        true, pageable
    );
  }

  @Test
  void findBookById_WhenBookExists_ShouldReturnBookDto() {
    // Given
    when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

    // When
    var result = catalogApplicationService.findBookById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(1L);
    assertThat(result.get().title()).isEqualTo("Test Book");
    assertThat(result.get().availableQuantity()).isEqualTo(10);
  }

  @Test
  void findBookById_WhenBookNotExists_ShouldReturnEmpty() {
    // Given
    when(bookRepository.findById(1L)).thenReturn(Optional.empty());

    // When
    var result = catalogApplicationService.findBookById(1L);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void createBook_WithAdminUser_ShouldCreateBook() {
    // Given
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
    when(bookRepository.save(any(Book.class))).thenReturn(testBook);

    // When
    var result = catalogApplicationService.createBook(createBookCommand, adminUser);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("Test Book");

    verify(bookRepository).save(any(Book.class));
  }

  @Test
  void createBook_WithDuplicateIsbn_ShouldThrowDuplicateIsbnException() {
    // Given
    org.mockito.Mockito.doThrow(new DuplicateIsbnException("9780596009205"))
        .when(duplicateIsbnChecker).ensureUnique("9780596009205");

    // When & Then
    assertThatThrownBy(() -> catalogApplicationService.createBook(createBookCommand, adminUser))
        .isInstanceOf(DuplicateIsbnException.class);
  }

  @Test
  void createBook_WithInvalidCategory_ShouldThrowCategoryNotFoundException() {
    // Given
    when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> catalogApplicationService.createBook(createBookCommand, adminUser))
        .isInstanceOf(CategoryNotFoundException.class);
  }

  @Test
  void updateBook_WithAdminUser_ShouldUpdateBook() {
    // Given
    when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
    when(bookRepository.save(any(Book.class))).thenReturn(testBook);

    // When
    var result = catalogApplicationService.updateBook(1L, updateBookCommand, adminUser);

    // Then
    assertThat(result).isNotNull();
    verify(bookRepository).save(testBook);
  }

  @Test
  void updateBook_WithInvalidBookId_ShouldThrowBookNotFoundException() {
    // Given
    when(bookRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> catalogApplicationService.updateBook(1L, updateBookCommand, adminUser))
        .isInstanceOf(BookNotFoundException.class);
  }

  @Test
  void deleteBook_WithAdminUser_ShouldMarkBookUnavailable() {
    // Given
    when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
    when(bookRepository.save(any(Book.class))).thenReturn(testBook);

    // When
    catalogApplicationService.deleteBook(1L, adminUser);

    // Then
    assertThat(testBook.isAvailable()).isFalse();
    verify(bookRepository).save(testBook);
  }

  @Test
  void findBookByIsbn_WhenBookExists_ShouldReturnBookDto() {
    // Given
    when(bookRepository.findByIsbn("9780134685991")).thenReturn(Optional.of(testBook));

    // When
    var result = catalogApplicationService.findBookByIsbn("9780134685991");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().isbn()).isEqualTo("9780134685991");
  }

  @Test
  void findAvailableBooks_ShouldReturnPagedResults() {
    // Given
    var pageable = PageRequest.of(0, 10);
    var books = List.of(testBook);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findAvailableBooks(pageable)).thenReturn(page);

    // When
    var result = catalogApplicationService.findAvailableBooks(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).available()).isTrue();
  }

  @Test
  void findBooksInStock_ShouldReturnPagedResults() {
    // Given
    var pageable = PageRequest.of(0, 10);
    var books = List.of(testBook);
    var page = new PageImpl<>(books, pageable, 1);

    when(bookRepository.findBooksInStock(pageable)).thenReturn(page);

    // When
    var result = catalogApplicationService.findBooksInStock(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).availableQuantity()).isGreaterThan(0);
  }
}
