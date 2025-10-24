package org.gripday.bookstore.domain.service;

import org.gripday.bookstore.domain.dto.*;
import org.gripday.bookstore.domain.exception.*;
import org.gripday.bookstore.infrastructure.entity.Book;
import org.gripday.bookstore.infrastructure.entity.Category;
import org.gripday.bookstore.infrastructure.entity.Inventory;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.gripday.bookstore.infrastructure.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private BookService bookService;
    
    private UserContext adminUser;
    private UserContext regularUser;
    private Book testBook;
    private Category testCategory;
    private Inventory testInventory;
    private CreateBookRequest createBookRequest;
    private UpdateBookRequest updateBookRequest;
    private BookSearchCriteria searchCriteria;
    
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
        testBook.setIsbn("978-0123456789");
        testBook.setDescription("A test book");
        testBook.setPrice(new BigDecimal("29.99"));
        testBook.setCategory(testCategory);
        testBook.setAvailable(true);
        testBook.setCreatedAt(LocalDateTime.now());
        testBook.setUpdatedAt(LocalDateTime.now());
        
        testInventory = new Inventory(testBook, 10);
        testInventory.setId(1L);
        testBook.setInventory(testInventory);
        
        createBookRequest = new CreateBookRequest(
            "New Book", "New Author", "978-9876543210",
            "A new book", new BigDecimal("39.99"), 1L, 5
        );
        
        updateBookRequest = new UpdateBookRequest(
            "Updated Book", "Updated Author", "Updated description",
            new BigDecimal("49.99"), 1L
        );
        
        searchCriteria = new BookSearchCriteria(
            "Test", "Author", "Fiction", 
            new BigDecimal("10.00"), new BigDecimal("50.00"), true
        );
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
        var result = bookService.findBooks(searchCriteria, pageable);
        
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
        var result = bookService.findBookById(1L);
        
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
        var result = bookService.findBookById(1L);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void createBook_WithAdminUser_ShouldCreateBook() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(bookRepository.findByIsbn("978-9876543210")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        var result = bookService.createBook(createBookRequest, adminUser);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Test Book");
        
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void createBook_WithRegularUser_ShouldThrowUnauthorizedException() {
        // When & Then
        assertThatThrownBy(() -> bookService.createBook(createBookRequest, regularUser))
            .isInstanceOf(UnauthorizedOperationException.class)
            .hasMessageContaining("create book");
    }
    
    @Test
    void createBook_WithDuplicateIsbn_ShouldThrowDuplicateIsbnException() {
        // Given
        when(bookRepository.findByIsbn("978-9876543210")).thenReturn(Optional.of(testBook));
        
        // When & Then
        assertThatThrownBy(() -> bookService.createBook(createBookRequest, adminUser))
            .isInstanceOf(DuplicateIsbnException.class);
    }
    
    @Test
    void createBook_WithInvalidCategory_ShouldThrowCategoryNotFoundException() {
        // Given
        when(bookRepository.findByIsbn("978-9876543210")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> bookService.createBook(createBookRequest, adminUser))
            .isInstanceOf(CategoryNotFoundException.class);
    }
    
    @Test
    void updateBook_WithAdminUser_ShouldUpdateBook() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        var result = bookService.updateBook(1L, updateBookRequest, adminUser);
        
        // Then
        assertThat(result).isNotNull();
        verify(bookRepository).save(testBook);
    }
    
    @Test
    void updateBook_WithRegularUser_ShouldThrowUnauthorizedException() {
        // When & Then
        assertThatThrownBy(() -> bookService.updateBook(1L, updateBookRequest, regularUser))
            .isInstanceOf(UnauthorizedOperationException.class)
            .hasMessageContaining("update book");
    }
    
    @Test
    void updateBook_WithInvalidBookId_ShouldThrowBookNotFoundException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> bookService.updateBook(1L, updateBookRequest, adminUser))
            .isInstanceOf(BookNotFoundException.class);
    }
    
    @Test
    void deleteBook_WithAdminUser_ShouldMarkBookUnavailable() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        bookService.deleteBook(1L, adminUser);
        
        // Then
        assertThat(testBook.isAvailable()).isFalse();
        verify(bookRepository).save(testBook);
    }
    
    @Test
    void deleteBook_WithRegularUser_ShouldThrowUnauthorizedException() {
        // When & Then
        assertThatThrownBy(() -> bookService.deleteBook(1L, regularUser))
            .isInstanceOf(UnauthorizedOperationException.class)
            .hasMessageContaining("delete book");
    }
    
    @Test
    void findBookByIsbn_WhenBookExists_ShouldReturnBookDto() {
        // Given
        when(bookRepository.findByIsbn("978-0123456789")).thenReturn(Optional.of(testBook));
        
        // When
        var result = bookService.findBookByIsbn("978-0123456789");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().isbn()).isEqualTo("978-0123456789");
    }
    
    @Test
    void findAvailableBooks_ShouldReturnPagedResults() {
        // Given
        var pageable = PageRequest.of(0, 10);
        var books = List.of(testBook);
        var page = new PageImpl<>(books, pageable, 1);
        
        when(bookRepository.findAvailableBooks(pageable)).thenReturn(page);
        
        // When
        var result = bookService.findAvailableBooks(pageable);
        
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
        var result = bookService.findBooksInStock(pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).availableQuantity()).isGreaterThan(0);
    }
}