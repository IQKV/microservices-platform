package org.gripday.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gripday.bookstore.catalog.Book;
import org.gripday.bookstore.catalog.BookNotFoundException;
import org.gripday.bookstore.catalog.BookRepository;
import org.gripday.bookstore.catalog.Category;
import org.gripday.bookstore.shared.AuditLogger;
import org.gripday.bookstore.shared.BookstoreMetrics;
import org.gripday.bookstore.shared.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryApplicationService Tests")
class InventoryApplicationServiceTest {

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private BookRepository bookRepository;

  @Mock
  private AuditLogger auditLogger;

  @Mock
  private BookstoreMetrics bookstoreMetrics;

  @InjectMocks
  private InventoryApplicationService inventoryApplicationService;

  private Book testBook;
  private Inventory testInventory;
  private UserContext testUserContext;

  @BeforeEach
  void setUp() {
    var category = Category.create("Technology", "Technology books");
    category.setId(1L);

    testBook = Book.create("Test Book", "Test Author", "978-0-596-52068-7", new BigDecimal("29.99"), "Test Description", category);
    testBook.setId(1L);
    testBook.setAvailable(true);

    testInventory = Inventory.create(testBook, 100, 20);
    testInventory.setId(1L);
    testInventory.setReservedQuantity(10);

    testUserContext = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("ADMIN"),
        Set.of(),
        "IT",
        "org1",
        Map.of()
    );
  }

  @Nested
  @DisplayName("Get Inventory Tests")
  class GetInventoryTests {

    @Test
    @DisplayName("Should get inventory successfully")
    void shouldGetInventorySuccessfully() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));

      // Act
      var result = inventoryApplicationService.getInventory(1L);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.bookId()).isEqualTo(1L);
      assertThat(result.bookTitle()).isEqualTo("Test Book");
      assertThat(result.quantity()).isEqualTo(100);
      assertThat(result.reservedQuantity()).isEqualTo(10);
      assertThat(result.availableQuantity()).isEqualTo(90);
      verify(inventoryRepository).findByBookId(1L);
    }

    @Test
    @DisplayName("Should throw BookNotFoundException when inventory not found")
    void shouldThrowExceptionWhenInventoryNotFound() {
      // Arrange
      when(inventoryRepository.findByBookId(999L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> inventoryApplicationService.getInventory(999L))
          .isInstanceOf(BookNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Update Inventory Tests")
  class UpdateInventoryTests {

    @Test
    @DisplayName("Should update inventory successfully")
    void shouldUpdateInventorySuccessfully() {
      // Arrange
      var command = new UpdateInventoryCommand(150, 25);
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
      when(bookstoreMetrics.startInventoryUpdateTimer()).thenReturn(null);

      // Act
      var result = inventoryApplicationService.updateInventory(1L, command, testUserContext);

      // Assert
      assertThat(result).isNotNull();
      assertThat(testInventory.getQuantity()).isEqualTo(150);
      assertThat(testInventory.getLowStockThreshold()).isEqualTo(25);
      verify(inventoryRepository).save(testInventory);
      verify(auditLogger).logInventoryUpdate(eq(1L), anyInt(), eq(150), eq(testUserContext));
      verify(bookstoreMetrics).incrementInventoryUpdated();
    }

    @Test
    @DisplayName("Should throw exception when quantity below reserved amount")
    void shouldThrowExceptionWhenQuantityBelowReserved() {
      // Arrange
      var command = new UpdateInventoryCommand(5, null); // Less than reserved (10)
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(bookstoreMetrics.startInventoryUpdateTimer()).thenReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> inventoryApplicationService.updateInventory(1L, command, testUserContext))
          .isInstanceOf(InsufficientInventoryException.class)
          .hasMessageContaining("Cannot set quantity below reserved amount");

      verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update only quantity when threshold is null")
    void shouldUpdateOnlyQuantityWhenThresholdIsNull() {
      // Arrange
      var command = new UpdateInventoryCommand(200, null);
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
      when(bookstoreMetrics.startInventoryUpdateTimer()).thenReturn(null);

      // Act
      inventoryApplicationService.updateInventory(1L, command, testUserContext);

      // Assert
      assertThat(testInventory.getQuantity()).isEqualTo(200);
      assertThat(testInventory.getLowStockThreshold()).isEqualTo(20); // Unchanged
    }
  }

  @Nested
  @DisplayName("Bulk Update Inventory Tests")
  class BulkUpdateInventoryTests {

    @Test
    @DisplayName("Should bulk update multiple inventories successfully")
    void shouldBulkUpdateSuccessfully() {
      // Arrange
      var commands = List.of(
          new BulkInventoryCommand(1L, 150, 25),
          new BulkInventoryCommand(1L, 200, 30)
      );

      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

      // Act
      var results = inventoryApplicationService.bulkUpdateInventory(commands, testUserContext);

      // Assert
      assertThat(results).hasSize(2);
      verify(auditLogger).logBulkInventoryUpdate(2, testUserContext);
    }

    @Test
    @DisplayName("Should skip items with quantity below reserved amount")
    void shouldSkipItemsWithInsufficientQuantity() {
      // Arrange
      var commands = List.of(
          new BulkInventoryCommand(1L, 5, null), // Below reserved (10)
          new BulkInventoryCommand(1L, 150, null) // Valid
      );

      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

      // Act
      var results = inventoryApplicationService.bulkUpdateInventory(commands, testUserContext);

      // Assert
      assertThat(results).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Availability Check Tests")
  class AvailabilityCheckTests {

    @Test
    @DisplayName("Should return true when book is available")
    void shouldReturnTrueWhenBookIsAvailable() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));

      // Act
      var result = inventoryApplicationService.isBookAvailable(1L, 50);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when requested quantity exceeds available")
    void shouldReturnFalseWhenQuantityExceedsAvailable() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));

      // Act
      var result = inventoryApplicationService.isBookAvailable(1L, 100); // Available is 90

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when inventory not found")
    void shouldReturnFalseWhenInventoryNotFound() {
      // Arrange
      when(inventoryRepository.findByBookId(999L)).thenReturn(Optional.empty());

      // Act
      var result = inventoryApplicationService.isBookAvailable(999L, 10);

      // Assert
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Reserve Quantity Tests")
  class ReserveQuantityTests {

    @Test
    @DisplayName("Should reserve quantity successfully")
    void shouldReserveQuantitySuccessfully() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

      // Act
      inventoryApplicationService.reserveQuantity(1L, 20, testUserContext);

      // Assert
      assertThat(testInventory.getReservedQuantity()).isEqualTo(30); // 10 + 20
      verify(inventoryRepository).save(testInventory);
    }

    @Test
    @DisplayName("Should throw exception when cannot reserve quantity")
    void shouldThrowExceptionWhenCannotReserve() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));

      // Act & Assert
      assertThatThrownBy(() -> inventoryApplicationService.reserveQuantity(1L, 100, testUserContext))
          .isInstanceOf(InsufficientInventoryException.class);

      verify(inventoryRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Release Reserved Quantity Tests")
  class ReleaseReservedQuantityTests {

    @Test
    @DisplayName("Should release reserved quantity successfully")
    void shouldReleaseReservedQuantitySuccessfully() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

      // Act
      inventoryApplicationService.releaseReservedQuantity(1L, 5, testUserContext);

      // Assert
      assertThat(testInventory.getReservedQuantity()).isEqualTo(5); // 10 - 5
      verify(inventoryRepository).save(testInventory);
    }
  }

  @Nested
  @DisplayName("Adjust Inventory Tests")
  class AdjustInventoryTests {

    @Test
    @DisplayName("Should adjust inventory quantity positively")
    void shouldAdjustInventoryPositively() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

      // Act
      inventoryApplicationService.adjustInventoryQuantity(1L, 50, testUserContext);

      // Assert
      assertThat(testInventory.getQuantity()).isEqualTo(150); // 100 + 50
      verify(inventoryRepository).save(testInventory);
    }

    @Test
    @DisplayName("Should adjust inventory quantity negatively")
    void shouldAdjustInventoryNegatively() {
      // Arrange
      when(inventoryRepository.findByBookId(1L)).thenReturn(Optional.of(testInventory));
      when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

      // Act
      inventoryApplicationService.adjustInventoryQuantity(1L, -20, testUserContext);

      // Assert
      assertThat(testInventory.getQuantity()).isEqualTo(80); // 100 - 20
      verify(inventoryRepository).save(testInventory);
    }
  }

  @Nested
  @DisplayName("Inventory Statistics Tests")
  class InventoryStatisticsTests {

    @Test
    @DisplayName("Should get total inventory count")
    void shouldGetTotalInventoryCount() {
      // Arrange
      when(inventoryRepository.getTotalInventoryCount()).thenReturn(1000L);

      // Act
      var result = inventoryApplicationService.getTotalInventoryCount();

      // Assert
      assertThat(result).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Should return zero when total inventory count is null")
    void shouldReturnZeroWhenTotalInventoryCountIsNull() {
      // Arrange
      when(inventoryRepository.getTotalInventoryCount()).thenReturn(null);

      // Act
      var result = inventoryApplicationService.getTotalInventoryCount();

      // Assert
      assertThat(result).isZero();
    }

    @Test
    @DisplayName("Should get total reserved count")
    void shouldGetTotalReservedCount() {
      // Arrange
      when(inventoryRepository.getTotalReservedCount()).thenReturn(150L);

      // Act
      var result = inventoryApplicationService.getTotalReservedCount();

      // Assert
      assertThat(result).isEqualTo(150L);
    }

    @Test
    @DisplayName("Should count low stock items")
    void shouldCountLowStockItems() {
      // Arrange
      when(inventoryRepository.countLowStockItems()).thenReturn(5L);

      // Act
      var result = inventoryApplicationService.countLowStockItems();

      // Assert
      assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should count out of stock items")
    void shouldCountOutOfStockItems() {
      // Arrange
      when(inventoryRepository.countOutOfStockItems()).thenReturn(3L);

      // Act
      var result = inventoryApplicationService.countOutOfStockItems();

      // Assert
      assertThat(result).isEqualTo(3L);
    }
  }

  @Nested
  @DisplayName("Get Low Stock and Out of Stock Tests")
  class GetLowStockAndOutOfStockTests {

    @Test
    @DisplayName("Should get low stock inventory")
    void shouldGetLowStockInventory() {
      // Arrange
      when(inventoryRepository.findLowStockInventory()).thenReturn(List.of(testInventory));

      // Act
      var results = inventoryApplicationService.getLowStockInventory();

      // Assert
      assertThat(results).hasSize(1);
      assertThat(results.get(0).bookId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get out of stock inventory")
    void shouldGetOutOfStockInventory() {
      // Arrange
      testInventory.setQuantity(0);
      when(inventoryRepository.findOutOfStockInventory()).thenReturn(List.of(testInventory));

      // Act
      var results = inventoryApplicationService.getOutOfStockInventory();

      // Assert
      assertThat(results).hasSize(1);
      assertThat(results.get(0).quantity()).isZero();
    }
  }
}
