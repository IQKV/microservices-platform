package com.iqscaffold.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.iqscaffold.bookstore.inventory.Inventory;
import com.iqscaffold.bookstore.inventory.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookAvailabilityChecker Domain Service Tests")
class BookAvailabilityCheckerTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private InventoryRepository inventoryRepository;

  @InjectMocks
  private BookAvailabilityChecker bookAvailabilityChecker;

  private Book availableBook;
  private Book unavailableBook;
  private Inventory availableInventory;
  private Inventory unavailableInventory;

  @BeforeEach
  void setUp() {
    // Create books using factory method
    availableBook = Book.create(
        "Test Book",
        "Test Author",
        "978-0132350884",
        new java.math.BigDecimal("29.99"),
        "Test Description",
        null
    );
    availableBook.setId(1L);
    availableBook.setAvailable(true);

    unavailableBook = Book.create(
        "Unavailable Book",
        "Test Author",
        "978-0134685991",
        new java.math.BigDecimal("29.99"),
        "Test Description",
        null
    );
    unavailableBook.setId(2L);
    unavailableBook.setAvailable(false);

    // Create inventories using factory method
    availableInventory = Inventory.create(availableBook, 10, 5);
    availableInventory.setId(1L);

    unavailableInventory = Inventory.create(unavailableBook, 0, 5);
    unavailableInventory.setId(2L);
  }

  @Nested
  @DisplayName("canBeSold Tests")
  class CanBeSoldTests {

    @Test
    @DisplayName("Should return true when book is available and has inventory")
    void shouldReturnTrueWhenBookIsAvailableAndHasInventory() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act
      var result = bookAvailabilityChecker.canBeSold(1L);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when book does not exist")
    void shouldReturnFalseWhenBookDoesNotExist() {
      // Arrange
      when(bookRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.canBeSold(999L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when book is not available")
    void shouldReturnFalseWhenBookIsNotAvailable() {
      // Arrange
      when(bookRepository.findById(2L)).thenReturn(Optional.of(unavailableBook));

      // Act
      var result = bookAvailabilityChecker.canBeSold(2L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when inventory does not exist")
    void shouldReturnFalseWhenInventoryDoesNotExist() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.canBeSold(1L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when inventory is not available")
    void shouldReturnFalseWhenInventoryIsNotAvailable() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(unavailableInventory));

      // Act
      var result = bookAvailabilityChecker.canBeSold(1L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.canBeSold(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }
  }

  @Nested
  @DisplayName("canBeSold with Quantity Tests")
  class CanBeSoldWithQuantityTests {

    @Test
    @DisplayName("Should return true when sufficient quantity available")
    void shouldReturnTrueWhenSufficientQuantityAvailable() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act
      var result = bookAvailabilityChecker.canBeSold(1L, 5);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when insufficient quantity")
    void shouldReturnFalseWhenInsufficientQuantity() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act
      var result = bookAvailabilityChecker.canBeSold(1L, 20);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when book does not exist")
    void shouldReturnFalseWhenBookDoesNotExist() {
      // Arrange
      when(bookRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.canBeSold(999L, 5);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when book is not available")
    void shouldReturnFalseWhenBookIsNotAvailable() {
      // Arrange
      when(bookRepository.findById(2L)).thenReturn(Optional.of(unavailableBook));

      // Act
      var result = bookAvailabilityChecker.canBeSold(2L, 5);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when inventory does not exist")
    void shouldReturnFalseWhenInventoryDoesNotExist() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.canBeSold(1L, 5);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.canBeSold(null, 5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }

    @Test
    @DisplayName("Should throw exception when quantity is zero")
    void shouldThrowExceptionWhenQuantityIsZero() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.canBeSold(1L, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive");
    }

    @Test
    @DisplayName("Should throw exception when quantity is negative")
    void shouldThrowExceptionWhenQuantityIsNegative() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.canBeSold(1L, -5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive");
    }
  }

  @Nested
  @DisplayName("isAvailableInCatalog Tests")
  class IsAvailableInCatalogTests {

    @Test
    @DisplayName("Should return true when book is available")
    void shouldReturnTrueWhenBookIsAvailable() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));

      // Act
      var result = bookAvailabilityChecker.isAvailableInCatalog(1L);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when book is not available")
    void shouldReturnFalseWhenBookIsNotAvailable() {
      // Arrange
      when(bookRepository.findById(2L)).thenReturn(Optional.of(unavailableBook));

      // Act
      var result = bookAvailabilityChecker.isAvailableInCatalog(2L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when book does not exist")
    void shouldReturnFalseWhenBookDoesNotExist() {
      // Arrange
      when(bookRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.isAvailableInCatalog(999L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.isAvailableInCatalog(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }
  }

  @Nested
  @DisplayName("hasStock Tests")
  class HasStockTests {

    @Test
    @DisplayName("Should return true when inventory has stock")
    void shouldReturnTrueWhenInventoryHasStock() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act
      var result = bookAvailabilityChecker.hasStock(1L);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when inventory has no stock")
    void shouldReturnFalseWhenInventoryHasNoStock() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(unavailableInventory));

      // Act
      var result = bookAvailabilityChecker.hasStock(1L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when inventory does not exist")
    void shouldReturnFalseWhenInventoryDoesNotExist() {
      // Arrange
      when(inventoryRepository.findByBookId(999L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.hasStock(999L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.hasStock(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }
  }

  @Nested
  @DisplayName("getAvailableQuantity Tests")
  class GetAvailableQuantityTests {

    @Test
    @DisplayName("Should return available quantity when book is available")
    void shouldReturnAvailableQuantityWhenBookIsAvailable() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act
      var result = bookAvailabilityChecker.getAvailableQuantity(1L);

      // Assert
      assertThat(result).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return zero when book is not available in catalog")
    void shouldReturnZeroWhenBookIsNotAvailableInCatalog() {
      // Arrange
      when(bookRepository.findById(2L)).thenReturn(Optional.of(unavailableBook));

      // Act
      var result = bookAvailabilityChecker.getAvailableQuantity(2L);

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("Should return zero when inventory does not exist")
    void shouldReturnZeroWhenInventoryDoesNotExist() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.getAvailableQuantity(1L);

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.getAvailableQuantity(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }
  }

  @Nested
  @DisplayName("isLowStock Tests")
  class IsLowStockTests {

    @Test
    @DisplayName("Should return true when inventory is low stock")
    void shouldReturnTrueWhenInventoryIsLowStock() {
      // Arrange
      var lowStockInventory = Inventory.create(availableBook, 3, 5);
      lowStockInventory.setId(1L);

      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(lowStockInventory));

      // Act
      var result = bookAvailabilityChecker.isLowStock(1L);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when inventory is not low stock")
    void shouldReturnFalseWhenInventoryIsNotLowStock() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act
      var result = bookAvailabilityChecker.isLowStock(1L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when inventory does not exist")
    void shouldReturnFalseWhenInventoryDoesNotExist() {
      // Arrange
      when(inventoryRepository.findByBookId(999L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.isLowStock(999L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.isLowStock(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }
  }

  @Nested
  @DisplayName("isOutOfStock Tests")
  class IsOutOfStockTests {

    @Test
    @DisplayName("Should return true when inventory is out of stock")
    void shouldReturnTrueWhenInventoryIsOutOfStock() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(unavailableInventory));

      // Act
      var result = bookAvailabilityChecker.isOutOfStock(1L);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when inventory is available")
    void shouldReturnFalseWhenInventoryIsAvailable() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act
      var result = bookAvailabilityChecker.isOutOfStock(1L);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when inventory does not exist")
    void shouldReturnTrueWhenInventoryDoesNotExist() {
      // Arrange
      when(inventoryRepository.findByBookId(999L)).thenReturn(Optional.empty());

      // Act
      var result = bookAvailabilityChecker.isOutOfStock(999L);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when book ID is null")
    void shouldThrowExceptionWhenBookIdIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.isOutOfStock(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Book ID must not be null");
    }
  }

  @Nested
  @DisplayName("ensureCanBeSold Tests")
  class EnsureCanBeSoldTests {

    @Test
    @DisplayName("Should not throw exception when book can be sold")
    void shouldNotThrowExceptionWhenBookCanBeSold() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act & Assert
      bookAvailabilityChecker.ensureCanBeSold(1L);
    }

    @Test
    @DisplayName("Should throw BookNotAvailableException when book cannot be sold")
    void shouldThrowBookNotAvailableExceptionWhenBookCannotBeSold() {
      // Arrange
      when(bookRepository.findById(2L)).thenReturn(Optional.of(unavailableBook));

      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.ensureCanBeSold(2L))
          .isInstanceOf(BookNotAvailableException.class)
          .hasMessage("Book with ID 2 is not available for sale");
    }
  }

  @Nested
  @DisplayName("ensureCanBeSold with Quantity Tests")
  class EnsureCanBeSoldWithQuantityTests {

    @Test
    @DisplayName("Should not throw exception when sufficient quantity available")
    void shouldNotThrowExceptionWhenSufficientQuantityAvailable() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act & Assert
      bookAvailabilityChecker.ensureCanBeSold(1L, 5);
    }

    @Test
    @DisplayName("Should throw BookNotAvailableException when book is not available")
    void shouldThrowBookNotAvailableExceptionWhenBookIsNotAvailable() {
      // Arrange
      when(bookRepository.findById(2L)).thenReturn(Optional.of(unavailableBook));

      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.ensureCanBeSold(2L, 5))
          .isInstanceOf(BookNotAvailableException.class)
          .hasMessage("Book with ID 2 is not available for sale");
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when no stock")
    void shouldThrowInsufficientStockExceptionWhenNoStock() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.ensureCanBeSold(1L, 5))
          .isInstanceOf(InsufficientStockException.class)
          .hasMessage("Insufficient stock for book ID 1: requested 5, available 0");
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when insufficient quantity")
    void shouldThrowInsufficientStockExceptionWhenInsufficientQuantity() {
      // Arrange
      when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(availableInventory));

      // Act & Assert
      assertThatThrownBy(() -> bookAvailabilityChecker.ensureCanBeSold(1L, 20))
          .isInstanceOf(InsufficientStockException.class)
          .hasMessage("Insufficient stock for book ID 1: requested 20, available 10");
    }
  }
}
