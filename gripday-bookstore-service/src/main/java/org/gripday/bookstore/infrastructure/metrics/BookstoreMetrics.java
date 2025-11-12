package org.gripday.bookstore.infrastructure.metrics;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.gripday.bookstore.infrastructure.repository.InventoryRepository;
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
    this.bookCreatedCounter = Counter.builder("bookstore.books.created")
        .description("Number of books created")
        .register(meterRegistry);

    this.bookUpdatedCounter = Counter.builder("bookstore.books.updated")
        .description("Number of books updated")
        .register(meterRegistry);

    this.bookDeletedCounter = Counter.builder("bookstore.books.deleted")
        .description("Number of books deleted")
        .register(meterRegistry);

    this.inventoryUpdatedCounter = Counter.builder("bookstore.inventory.updated")
        .description("Number of inventory updates")
        .register(meterRegistry);

    this.bookSearchCounter = Counter.builder("bookstore.books.searched")
        .description("Number of book searches performed")
        .register(meterRegistry);

    this.unauthorizedAccessCounter = Counter.builder("bookstore.security.unauthorized_access")
        .description("Number of unauthorized access attempts")
        .register(meterRegistry);

    // Initialize timers
    this.bookSearchTimer = Timer.builder("bookstore.books.search.duration")
        .description("Time taken for book searches")
        .register(meterRegistry);

    this.bookCreationTimer = Timer.builder("bookstore.books.creation.duration")
        .description("Time taken for book creation")
        .register(meterRegistry);

    this.inventoryUpdateTimer = Timer.builder("bookstore.inventory.update.duration")
        .description("Time taken for inventory updates")
        .register(meterRegistry);

    // Initialize gauges
    Gauge.builder("bookstore.books.total", this, BookstoreMetrics::getTotalBooks)
        .description("Total number of books in catalog")
        .register(meterRegistry);

    Gauge.builder("bookstore.books.available", this, BookstoreMetrics::getAvailableBooks)
        .description("Number of available books")
        .register(meterRegistry);

    Gauge.builder("bookstore.inventory.low_stock", lowStockBooksCount, AtomicLong::get)
        .description("Number of books with low stock")
        .register(meterRegistry);

    Gauge.builder("bookstore.inventory.out_of_stock", outOfStockBooksCount, AtomicLong::get)
        .description("Number of books out of stock")
        .register(meterRegistry);

    Gauge.builder("bookstore.inventory.total_quantity", this, BookstoreMetrics::getTotalInventoryQuantity)
        .description("Total inventory quantity across all books")
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
    } catch (Exception e) {
      return -1; // Indicate error state
    }
  }

  private double getAvailableBooks() {
    try {
      return bookRepository.countByAvailableTrue();
    } catch (Exception e) {
      return -1; // Indicate error state
    }
  }

  private double getTotalInventoryQuantity() {
    try {
      return inventoryRepository.sumTotalQuantity();
    } catch (Exception e) {
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
    } catch (Exception e) {
      // Log error but don't fail the application
      updateLowStockCount(-1);
      updateOutOfStockCount(-1);
    }
  }
}