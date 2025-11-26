package com.iqscaffold.bookstore.shared;

import java.util.concurrent.atomic.AtomicLong;

import com.iqscaffold.bookstore.catalog.BookRepository;
import com.iqscaffold.bookstore.inventory.InventoryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BookstoreMetrics {

  private final MeterRegistry meterRegistry;
  private final BookRepository bookRepository;
  private final InventoryRepository inventoryRepository;

  // Counters for operations
  private final Counter bookCreatedCounter;
  private final Counter bookUpdatedCounter;
  private final Counter bookDeletedCounter;
  private final Counter inventoryUpdatedCounter;
  private final Counter bookSearchCounter;
  private final Counter unauthorizedAccessCounter;

  // Timers for performance monitoring
  private final Timer bookSearchTimer;
  private final Timer bookCreationTimer;
  private final Timer inventoryUpdateTimer;

  // Atomic counters for real-time metrics
  private final AtomicLong lowStockBooksCount = new AtomicLong(0);
  private final AtomicLong outOfStockBooksCount = new AtomicLong(0);

  public BookstoreMetrics(final MeterRegistry meterRegistry, final BookRepository bookRepository,
                          final InventoryRepository inventoryRepository) {
    this.meterRegistry = meterRegistry;
    this.bookRepository = bookRepository;
    this.inventoryRepository = inventoryRepository;

    // Initialize counters
    this.bookCreatedCounter = Counter.builder(BookstoreConstants.Metrics.BOOKS_CREATED)
        .description(BookstoreConstants.Metrics.DESC_BOOKS_CREATED)
        .register(meterRegistry);

    this.bookUpdatedCounter = Counter.builder(BookstoreConstants.Metrics.BOOKS_UPDATED)
        .description(BookstoreConstants.Metrics.DESC_BOOKS_UPDATED)
        .register(meterRegistry);

    this.bookDeletedCounter = Counter.builder(BookstoreConstants.Metrics.BOOKS_DELETED)
        .description(BookstoreConstants.Metrics.DESC_BOOKS_DELETED)
        .register(meterRegistry);

    this.inventoryUpdatedCounter = Counter.builder(BookstoreConstants.Metrics.INVENTORY_UPDATED)
        .description(BookstoreConstants.Metrics.DESC_INVENTORY_UPDATED)
        .register(meterRegistry);

    this.bookSearchCounter = Counter.builder(BookstoreConstants.Metrics.BOOKS_SEARCHED)
        .description(BookstoreConstants.Metrics.DESC_BOOKS_SEARCHED)
        .register(meterRegistry);

    this.unauthorizedAccessCounter = Counter.builder(BookstoreConstants.Metrics.UNAUTHORIZED_ACCESS)
        .description(BookstoreConstants.Metrics.DESC_UNAUTHORIZED_ACCESS)
        .register(meterRegistry);

    // Initialize timers
    this.bookSearchTimer = Timer.builder(BookstoreConstants.Metrics.BOOK_SEARCH_DURATION)
        .description(BookstoreConstants.Metrics.DESC_BOOK_SEARCH_DURATION)
        .register(meterRegistry);

    this.bookCreationTimer = Timer.builder(BookstoreConstants.Metrics.BOOK_CREATION_DURATION)
        .description(BookstoreConstants.Metrics.DESC_BOOK_CREATION_DURATION)
        .register(meterRegistry);

    this.inventoryUpdateTimer = Timer.builder(BookstoreConstants.Metrics.INVENTORY_UPDATE_DURATION)
        .description(BookstoreConstants.Metrics.DESC_INVENTORY_UPDATE_DURATION)
        .register(meterRegistry);

    // Initialize gauges
    Gauge.builder(BookstoreConstants.Metrics.BOOKS_TOTAL, this, BookstoreMetrics::getTotalBooks)
        .description(BookstoreConstants.Metrics.DESC_BOOKS_TOTAL)
        .register(meterRegistry);

    Gauge.builder(BookstoreConstants.Metrics.BOOKS_AVAILABLE, this, BookstoreMetrics::getAvailableBooks)
        .description(BookstoreConstants.Metrics.DESC_BOOKS_AVAILABLE)
        .register(meterRegistry);

    Gauge.builder(BookstoreConstants.Metrics.INVENTORY_LOW_STOCK, lowStockBooksCount, AtomicLong::get)
        .description(BookstoreConstants.Metrics.DESC_INVENTORY_LOW_STOCK)
        .register(meterRegistry);

    Gauge.builder(BookstoreConstants.Metrics.INVENTORY_OUT_OF_STOCK, outOfStockBooksCount, AtomicLong::get)
        .description(BookstoreConstants.Metrics.DESC_INVENTORY_OUT_OF_STOCK)
        .register(meterRegistry);

    Gauge.builder(BookstoreConstants.Metrics.INVENTORY_TOTAL_QUANTITY, this, BookstoreMetrics::getTotalInventoryQuantity)
        .description(BookstoreConstants.Metrics.DESC_INVENTORY_TOTAL_QUANTITY)
        .register(meterRegistry);
  }

  // Counter methods
  public void incrementBookCreated() {
    bookCreatedCounter.increment();
  }

  public void incrementBookUpdated() {
    bookUpdatedCounter.increment();
  }

  public void incrementBookDeleted() {
    bookDeletedCounter.increment();
  }

  public void incrementInventoryUpdated() {
    inventoryUpdatedCounter.increment();
  }

  public void incrementBookSearch() {
    bookSearchCounter.increment();
  }

  public void incrementUnauthorizedAccess() {
    unauthorizedAccessCounter.increment();
  }

  // Timer methods
  public Timer.Sample startBookSearchTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordBookSearchTime(Timer.Sample sample) {
    sample.stop(bookSearchTimer);
  }

  public Timer.Sample startBookCreationTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordBookCreationTime(Timer.Sample sample) {
    sample.stop(bookCreationTimer);
  }

  public Timer.Sample startInventoryUpdateTimer() {
    return Timer.start(meterRegistry);
  }

  public void recordInventoryUpdateTime(Timer.Sample sample) {
    sample.stop(inventoryUpdateTimer);
  }

  // Gauge data providers
  private double getTotalBooks() {
    try {
      return bookRepository.count();
    } catch (final Exception e) {
      return -1; // Indicate error state
    }
  }

  private double getAvailableBooks() {
    try {
      return bookRepository.countByAvailableTrue();
    } catch (final Exception e) {
      return -1; // Indicate error state
    }
  }

  private double getTotalInventoryQuantity() {
    try {
      return inventoryRepository.sumTotalQuantity();
    } catch (final Exception e) {
      return -1; // Indicate error state
    }
  }

  // Methods to update atomic counters
  public void updateLowStockCount(long count) {
    lowStockBooksCount.set(count);
  }

  public void updateOutOfStockCount(long count) {
    outOfStockBooksCount.set(count);
  }

  // Convenience method to refresh inventory metrics
  public void refreshInventoryMetrics() {
    try {
      var lowStockCount = inventoryRepository.countLowStockBooks();
      var outOfStockCount = inventoryRepository.countOutOfStockBooks();

      updateLowStockCount(lowStockCount);
      updateOutOfStockCount(outOfStockCount);
    } catch (final Exception e) {
      // Log error but don't fail the application
      updateLowStockCount(-1);
      updateOutOfStockCount(-1);
    }
  }
}
