package com.iqscaffold.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.iqscaffold.bookstore.catalog.Book;
import com.iqscaffold.bookstore.catalog.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class InventoryRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private InventoryRepository inventoryRepository;

  private Category fictionCategory;
  private Category scienceCategory;
  private Book book1;
  private Book book2;
  private Book book3;
  private Book book4;
  private Inventory inventory1;
  private Inventory inventory2;
  private Inventory inventory3;
  private Inventory inventory4;

  @BeforeEach
  void setUp() {
    // Create categories
    fictionCategory = Category.create("Fiction", "Fiction books");
    scienceCategory = Category.create("Science", "Science books");
    entityManager.persistAndFlush(fictionCategory);
    entityManager.persistAndFlush(scienceCategory);

    // Create books
    book1 = Book.create("The Great Gatsby", "F. Scott Fitzgerald", "9780134685991", new BigDecimal("15.99"), null, fictionCategory);

    book2 = Book.create("To Kill a Mockingbird", "Harper Lee", "9780596009205", new BigDecimal("12.50"), null, fictionCategory);

    book3 = Book.create("A Brief History of Time", "Stephen Hawking", "9781617294945", new BigDecimal("18.99"), null, scienceCategory);

    book4 = Book.create("Unavailable Book", "Test Author", "9780132350884", new BigDecimal("20.00"), null, scienceCategory);
    book4.setAvailable(false);

    entityManager.persistAndFlush(book1);
    entityManager.persistAndFlush(book2);
    entityManager.persistAndFlush(book3);
    entityManager.persistAndFlush(book4);

    // Create inventory records
    inventory1 = Inventory.create(book1, 15); // Normal stock
    inventory1.setLowStockThreshold(5);
    inventory1.setReservedQuantity(2);

    inventory2 = Inventory.create(book2, 3); // Low stock
    inventory2.setLowStockThreshold(5);
    inventory2.setReservedQuantity(1);

    inventory3 = Inventory.create(book3, 0); // Out of stock
    inventory3.setLowStockThreshold(5);
    inventory3.setReservedQuantity(0);

    inventory4 = Inventory.create(book4, 10); // Unavailable book with stock
    inventory4.setLowStockThreshold(3);
    inventory4.setReservedQuantity(0);

    entityManager.persistAndFlush(inventory1);
    entityManager.persistAndFlush(inventory2);
    entityManager.persistAndFlush(inventory3);
    entityManager.persistAndFlush(inventory4);
    entityManager.clear();
  }

  @Test
  void findByBookId_ShouldReturnInventoryForBook() {
    // When
    var result = inventoryRepository.findByBookId(book1.getId());

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getBook().getTitle()).isEqualTo("The Great Gatsby");
    assertThat(result.get().getQuantity()).isEqualTo(15);
  }

  @Test
  void findByBookIsbn_ShouldReturnInventoryForBookWithIsbn() {
    // When
    var result = inventoryRepository.findByBookIsbn("9780134685991");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getBook().getTitle()).isEqualTo("The Great Gatsby");
  }

  @Test
  void findLowStockInventory_ShouldReturnInventoryBelowThreshold() {
    // When
    var result = inventoryRepository.findLowStockInventory();

    // Then
    assertThat(result).hasSize(2); // book2 and book3
    assertThat(result)
        .extracting(inventory -> inventory.getBook().getTitle())
        .containsExactlyInAnyOrder("To Kill a Mockingbird", "A Brief History of Time");
  }

  @Test
  void findOutOfStockInventory_ShouldReturnInventoryWithZeroQuantity() {
    // When
    var result = inventoryRepository.findOutOfStockInventory();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBook().getTitle()).isEqualTo("A Brief History of Time");
    assertThat(result.get(0).getQuantity()).isEqualTo(0);
  }

  @Test
  void findAvailableInventory_ShouldReturnInventoryForAvailableBooksWithStock() {
    // When
    var result = inventoryRepository.findAvailableInventory();

    // Then
    assertThat(result).hasSize(2); // book1 and book2 (book3 has 0 stock, book4 is unavailable)
    assertThat(result)
        .allMatch(inventory -> inventory.getQuantity() > 0 && inventory.getBook().isAvailable());
  }

  @Test
  void findInventoryWithReservations_ShouldReturnInventoryWithReservedQuantity() {
    // When
    var result = inventoryRepository.findInventoryWithReservations();

    // Then
    assertThat(result).hasSize(2); // book1 and book2 have reservations
    assertThat(result)
        .allMatch(inventory -> inventory.getReservedQuantity() > 0);
  }

  @Test
  void getTotalReservedQuantityForBook_ShouldReturnCorrectReservedAmount() {
    // When
    var result = inventoryRepository.getTotalReservedQuantityForBook(book1.getId());

    // Then
    assertThat(result).isEqualTo(2);
  }

  @Test
  void findByBookIds_ShouldReturnInventoryForSpecifiedBooks() {
    // Given
    var bookIds = List.of(book1.getId(), book2.getId());

    // When
    var result = inventoryRepository.findByBookIds(bookIds);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(inventory -> inventory.getBook().getId())
        .containsExactlyInAnyOrder(book1.getId(), book2.getId());
  }

  @Test
  void updateQuantityByBookId_ShouldUpdateInventoryQuantity() {
    // When
    var updatedRows = inventoryRepository.updateQuantityByBookId(book1.getId(), 25);
    entityManager.flush();
    entityManager.clear();

    // Then
    assertThat(updatedRows).isEqualTo(1);
    var updatedInventory = inventoryRepository.findByBookId(book1.getId()).orElseThrow();
    assertThat(updatedInventory.getQuantity()).isEqualTo(25);
  }

  @Test
  void updateReservedQuantityByBookId_ShouldUpdateReservedQuantity() {
    // When
    var updatedRows = inventoryRepository.updateReservedQuantityByBookId(book1.getId(), 5);
    entityManager.flush();
    entityManager.clear();

    // Then
    assertThat(updatedRows).isEqualTo(1);
    var updatedInventory = inventoryRepository.findByBookId(book1.getId()).orElseThrow();
    assertThat(updatedInventory.getReservedQuantity()).isEqualTo(5);
  }

  @Test
  void updateLowStockThresholdByBookId_ShouldUpdateThreshold() {
    // When
    var updatedRows = inventoryRepository.updateLowStockThresholdByBookId(book1.getId(), 10);
    entityManager.flush();
    entityManager.clear();

    // Then
    assertThat(updatedRows).isEqualTo(1);
    var updatedInventory = inventoryRepository.findByBookId(book1.getId()).orElseThrow();
    assertThat(updatedInventory.getLowStockThreshold()).isEqualTo(10);
  }

  @Test
  void bulkAdjustQuantity_ShouldAdjustQuantityForMultipleBooks() {
    // Given
    var bookIds = List.of(book1.getId(), book2.getId());
    var adjustment = 5;

    // When
    var updatedRows = inventoryRepository.bulkAdjustQuantity(bookIds, adjustment);
    entityManager.flush();
    entityManager.clear();

    // Then
    assertThat(updatedRows).isEqualTo(2);
    var inventory1Updated = inventoryRepository.findByBookId(book1.getId()).orElseThrow();
    var inventory2Updated = inventoryRepository.findByBookId(book2.getId()).orElseThrow();
    assertThat(inventory1Updated.getQuantity()).isEqualTo(20); // 15 + 5
    assertThat(inventory2Updated.getQuantity()).isEqualTo(8);  // 3 + 5
  }

  @Test
  void bulkAdjustQuantity_ShouldNotAllowNegativeQuantity() {
    // Given
    var bookIds = List.of(book2.getId()); // book2 has quantity 3
    var adjustment = -5; // Would result in negative quantity

    // When
    var updatedRows = inventoryRepository.bulkAdjustQuantity(bookIds, adjustment);
    entityManager.flush();
    entityManager.clear();

    // Then
    assertThat(updatedRows).isEqualTo(0); // No rows updated due to constraint
    var inventory = inventoryRepository.findByBookId(book2.getId()).orElseThrow();
    assertThat(inventory.getQuantity()).isEqualTo(3); // Unchanged
  }

  @Test
  void getTotalInventoryCount_ShouldReturnSumOfAllAvailableInventory() {
    // When
    var total = inventoryRepository.getTotalInventoryCount();

    // Then
    assertThat(total).isEqualTo(18); // 15 + 3 + 0 (book4 is unavailable)
  }

  @Test
  void getTotalReservedCount_ShouldReturnSumOfAllReservedQuantity() {
    // When
    var total = inventoryRepository.getTotalReservedCount();

    // Then
    assertThat(total).isEqualTo(3); // 2 + 1 + 0 (book4 is unavailable)
  }

  @Test
  void countLowStockItems_ShouldReturnCountOfLowStockAvailableBooks() {
    // When
    var count = inventoryRepository.countLowStockItems();

    // Then
    assertThat(count).isEqualTo(2); // book2 and book3 (book4 is unavailable)
  }

  @Test
  void countOutOfStockItems_ShouldReturnCountOfOutOfStockAvailableBooks() {
    // When
    var count = inventoryRepository.countOutOfStockItems();

    // Then
    assertThat(count).isEqualTo(1); // Only book3
  }

  @Test
  void findLowStockInventoryByCategory_ShouldReturnLowStockInCategory() {
    // When
    var result = inventoryRepository.findLowStockInventoryByCategory("Fiction");

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBook().getTitle()).isEqualTo("To Kill a Mockingbird");
  }

  @Test
  void getTotalInventoryCountByCategory_ShouldReturnSumForCategory() {
    // When
    var total = inventoryRepository.getTotalInventoryCountByCategory("Fiction");

    // Then
    assertThat(total).isEqualTo(18); // 15 + 3
  }

  @Test
  void isQuantityAvailable_ShouldReturnTrueWhenSufficientStock() {
    // When
    var available = inventoryRepository.isQuantityAvailable(book1.getId(), 10);

    // Then
    assertThat(available).isTrue(); // 15 - 2 = 13 available, requesting 10
  }

  @Test
  void isQuantityAvailable_ShouldReturnFalseWhenInsufficientStock() {
    // When
    var available = inventoryRepository.isQuantityAvailable(book1.getId(), 15);

    // Then
    assertThat(available).isFalse(); // 15 - 2 = 13 available, requesting 15
  }

  @Test
  void findRecentlyUpdatedInventory_ShouldReturnInventoryUpdatedAfterDate() {
    // Given
    var since = LocalDateTime.now().minusHours(1);

    // When
    var result = inventoryRepository.findRecentlyUpdatedInventory(since);

    // Then
    assertThat(result).hasSize(4); // All inventory records were created recently
  }

  @Test
  void findInventoryByAvailableQuantityRange_ShouldReturnInventoryInRange() {
    // When
    var result = inventoryRepository.findInventoryByAvailableQuantityRange(1, 15);

    // Then
    assertThat(result).hasSize(2); // book1 (13 available) and book2 (2 available)
    assertThat(result)
        .allMatch(inventory -> {
          var available = inventory.getQuantity() - inventory.getReservedQuantity();
          return available >= 1 && available <= 15;
        });
  }

  @Test
  void oneToOneRelationship_ShouldWorkCorrectly() {
    // When
    var inventory = inventoryRepository.findByBookId(book1.getId()).orElseThrow();

    // Then
    assertThat(inventory.getBook()).isNotNull();
    assertThat(inventory.getBook().getTitle()).isEqualTo("The Great Gatsby");
    assertThat(inventory.getBook().getInventory()).isEqualTo(inventory);
  }

  @Test
  void businessLogicMethods_ShouldWorkCorrectly() {
    // Given
    var inventory = inventoryRepository.findByBookId(book1.getId()).orElseThrow();

    // When & Then
    assertThat(inventory.getAvailableQuantity()).isEqualTo(13); // 15 - 2
    assertThat(inventory.isLowStock()).isFalse(); // 13 > 5
    assertThat(inventory.isAvailable()).isTrue(); // 13 > 0
    assertThat(inventory.canReserve(10)).isTrue(); // 13 >= 10
    assertThat(inventory.canReserve(15)).isFalse(); // 13 < 15
  }
}
