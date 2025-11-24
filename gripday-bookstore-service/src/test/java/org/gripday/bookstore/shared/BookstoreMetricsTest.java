package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.gripday.bookstore.catalog.BookRepository;
import org.gripday.bookstore.inventory.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookstoreMetrics Tests")
class BookstoreMetricsTest {

  private MeterRegistry meterRegistry;

  @Mock
  private BookRepository bookRepository;

  @Mock
  private InventoryRepository inventoryRepository;

  private BookstoreMetrics bookstoreMetrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    bookstoreMetrics = new BookstoreMetrics(meterRegistry, bookRepository, inventoryRepository);
  }

  @Test
  @DisplayName("Should increment book created counter")
  void shouldIncrementBookCreatedCounter() {
    // Act
    bookstoreMetrics.incrementBookCreated();
    bookstoreMetrics.incrementBookCreated();

    // Assert
    var counter = meterRegistry.counter(BookstoreConstants.Metrics.BOOKS_CREATED);
    assertThat(counter.count()).isEqualTo(2.0);
  }

  @Test
  @DisplayName("Should increment book updated counter")
  void shouldIncrementBookUpdatedCounter() {
    // Act
    bookstoreMetrics.incrementBookUpdated();

    // Assert
    var counter = meterRegistry.counter(BookstoreConstants.Metrics.BOOKS_UPDATED);
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should increment book deleted counter")
  void shouldIncrementBookDeletedCounter() {
    // Act
    bookstoreMetrics.incrementBookDeleted();

    // Assert
    var counter = meterRegistry.counter(BookstoreConstants.Metrics.BOOKS_DELETED);
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should increment inventory updated counter")
  void shouldIncrementInventoryUpdatedCounter() {
    // Act
    bookstoreMetrics.incrementInventoryUpdated();
    bookstoreMetrics.incrementInventoryUpdated();
    bookstoreMetrics.incrementInventoryUpdated();

    // Assert
    var counter = meterRegistry.counter(BookstoreConstants.Metrics.INVENTORY_UPDATED);
    assertThat(counter.count()).isEqualTo(3.0);
  }

  @Test
  @DisplayName("Should increment book search counter")
  void shouldIncrementBookSearchCounter() {
    // Act
    bookstoreMetrics.incrementBookSearch();

    // Assert
    var counter = meterRegistry.counter(BookstoreConstants.Metrics.BOOKS_SEARCHED);
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should increment unauthorized access counter")
  void shouldIncrementUnauthorizedAccessCounter() {
    // Act
    bookstoreMetrics.incrementUnauthorizedAccess();

    // Assert
    var counter = meterRegistry.counter(BookstoreConstants.Metrics.UNAUTHORIZED_ACCESS);
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should record book search time")
  void shouldRecordBookSearchTime() {
    // Arrange
    var sample = bookstoreMetrics.startBookSearchTimer();

    // Act
    bookstoreMetrics.recordBookSearchTime(sample);

    // Assert
    var timer = meterRegistry.timer(BookstoreConstants.Metrics.BOOK_SEARCH_DURATION);
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should record book creation time")
  void shouldRecordBookCreationTime() {
    // Arrange
    var sample = bookstoreMetrics.startBookCreationTimer();

    // Act
    bookstoreMetrics.recordBookCreationTime(sample);

    // Assert
    var timer = meterRegistry.timer(BookstoreConstants.Metrics.BOOK_CREATION_DURATION);
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should record inventory update time")
  void shouldRecordInventoryUpdateTime() {
    // Arrange
    var sample = bookstoreMetrics.startInventoryUpdateTimer();

    // Act
    bookstoreMetrics.recordInventoryUpdateTime(sample);

    // Assert
    var timer = meterRegistry.timer(BookstoreConstants.Metrics.INVENTORY_UPDATE_DURATION);
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should track total books gauge")
  void shouldTrackTotalBooksGauge() {
    // Act
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.BOOKS_TOTAL).gauge();

    // Assert
    assertThat(gauge).isNotNull();
  }

  @Test
  @DisplayName("Should track available books gauge")
  void shouldTrackAvailableBooksGauge() {
    // Act
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.BOOKS_AVAILABLE).gauge();

    // Assert
    assertThat(gauge).isNotNull();
  }

  @Test
  @DisplayName("Should update low stock count")
  void shouldUpdateLowStockCount() {
    // Act
    bookstoreMetrics.updateLowStockCount(15);

    // Assert
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.INVENTORY_LOW_STOCK).gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(15.0);
  }

  @Test
  @DisplayName("Should update out of stock count")
  void shouldUpdateOutOfStockCount() {
    // Act
    bookstoreMetrics.updateOutOfStockCount(5);

    // Assert
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.INVENTORY_OUT_OF_STOCK).gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(5.0);
  }

  @Test
  @DisplayName("Should refresh inventory metrics")
  void shouldRefreshInventoryMetrics() {
    // Arrange
    when(inventoryRepository.countLowStockBooks()).thenReturn(10L);
    when(inventoryRepository.countOutOfStockBooks()).thenReturn(3L);

    // Act
    bookstoreMetrics.refreshInventoryMetrics();

    // Assert
    var lowStockGauge = meterRegistry.find(BookstoreConstants.Metrics.INVENTORY_LOW_STOCK).gauge();
    var outOfStockGauge = meterRegistry.find(BookstoreConstants.Metrics.INVENTORY_OUT_OF_STOCK).gauge();

    assertThat(lowStockGauge).isNotNull();
    assertThat(lowStockGauge.value()).isEqualTo(10.0);
    assertThat(outOfStockGauge).isNotNull();
    assertThat(outOfStockGauge.value()).isEqualTo(3.0);
  }

  @Test
  @DisplayName("Should handle repository exception in refresh inventory metrics")
  void shouldHandleRepositoryExceptionInRefreshInventoryMetrics() {
    // Arrange
    when(inventoryRepository.countLowStockBooks()).thenThrow(new RuntimeException("Database error"));

    // Act
    bookstoreMetrics.refreshInventoryMetrics();

    // Assert - Should set error state (-1) and not throw exception
    var lowStockGauge = meterRegistry.find(BookstoreConstants.Metrics.INVENTORY_LOW_STOCK).gauge();
    assertThat(lowStockGauge).isNotNull();
    assertThat(lowStockGauge.value()).isEqualTo(-1.0);
  }

  @Test
  @DisplayName("Should handle repository exception in total books gauge")
  void shouldHandleRepositoryExceptionInTotalBooksGauge() {
    // Arrange
    when(bookRepository.count()).thenThrow(new RuntimeException("Database error"));

    // Act
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.BOOKS_TOTAL).gauge();

    // Assert - Gauge should exist and return -1 on error
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(-1.0);
  }

  @Test
  @DisplayName("Should handle repository exception in available books gauge")
  void shouldHandleRepositoryExceptionInAvailableBooksGauge() {
    // Arrange
    when(bookRepository.countByAvailableTrue()).thenThrow(new RuntimeException("Database error"));

    // Act
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.BOOKS_AVAILABLE).gauge();

    // Assert - Gauge should exist and return -1 on error
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(-1.0);
  }

  @Test
  @DisplayName("Should track total inventory quantity gauge")
  void shouldTrackTotalInventoryQuantityGauge() {
    // Arrange
    when(inventoryRepository.sumTotalQuantity()).thenReturn(500L);

    // Act
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.INVENTORY_TOTAL_QUANTITY).gauge();

    // Assert
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(500.0);
  }

  @Test
  @DisplayName("Should handle repository exception in total inventory quantity")
  void shouldHandleRepositoryExceptionInTotalInventoryQuantity() {
    // Arrange
    when(inventoryRepository.sumTotalQuantity()).thenThrow(new RuntimeException("Database error"));

    // Act
    var gauge = meterRegistry.find(BookstoreConstants.Metrics.INVENTORY_TOTAL_QUANTITY).gauge();

    // Assert - Gauge should exist and return -1 on error
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(-1.0);
  }

  @Test
  @DisplayName("Should have correct metric descriptions")
  void shouldHaveCorrectMetricDescriptions() {
    // Assert
    var bookCreatedCounter = meterRegistry.find(BookstoreConstants.Metrics.BOOKS_CREATED).counter();
    assertThat(bookCreatedCounter).isNotNull();
    assertThat(bookCreatedCounter.getId().getDescription())
        .isEqualTo(BookstoreConstants.Metrics.DESC_BOOKS_CREATED);
  }
}
