package com.iqscaffold.bookstore.inventory;

import java.util.ArrayList;
import java.util.List;

import com.iqscaffold.bookstore.catalog.BookNotFoundException;
import com.iqscaffold.bookstore.catalog.BookRepository;
import com.iqscaffold.bookstore.shared.AuditLogger;
import com.iqscaffold.bookstore.shared.BookstoreMetrics;
import com.iqscaffold.bookstore.shared.CacheConfig;
import com.iqscaffold.bookstore.shared.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Inventory use cases.
 * Orchestrates domain logic, manages transactions, and handles DTO conversions.
 */
@Service
@Transactional
public class InventoryApplicationService {

  private static final Logger logger = LoggerFactory.getLogger(InventoryApplicationService.class);

  private final InventoryRepository inventoryRepository;
  private final BookRepository bookRepository;
  private final AuditLogger auditLogger;
  private final BookstoreMetrics bookstoreMetrics;

  public InventoryApplicationService(final InventoryRepository inventoryRepository, final BookRepository bookRepository,
                                     final AuditLogger auditLogger, final BookstoreMetrics bookstoreMetrics) {
    this.inventoryRepository = inventoryRepository;
    this.bookRepository = bookRepository;
    this.auditLogger = auditLogger;
    this.bookstoreMetrics = bookstoreMetrics;
  }

  @Transactional(readOnly = true)
  public InventoryDto getInventory(Long bookId) {
    logger.debug("Getting inventory for book ID: {}", bookId);

    var inventory = inventoryRepository.findByBookId(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    return convertToDto(inventory);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  @Caching(evict = {
      @CacheEvict(value = CacheConfig.BOOK_CACHE, key = "#bookId"),
      @CacheEvict(value = CacheConfig.BOOK_SEARCH_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.POPULAR_BOOKS_CACHE, allEntries = true)
  })
  public InventoryDto updateInventory(Long bookId, UpdateInventoryCommand command, UserContext userContext) {
    logger.info("Updating inventory for book ID: {} by user: {}", bookId, userContext.username());

    var timer = bookstoreMetrics.startInventoryUpdateTimer();

    var inventory = inventoryRepository.findByBookId(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    if (command.quantity() < inventory.getReservedQuantity()) {
      throw new InvalidInventoryQuantityException(
          bookId,
          command.quantity(),
          inventory.getReservedQuantity());
    }

    inventory.setQuantity(command.quantity());

    if (command.lowStockThreshold() != null) {
      inventory.setLowStockThreshold(command.lowStockThreshold());
    }

    var book = inventory.getBook();
    book.setAvailable(inventory.isAvailable());

    var oldQuantity = inventory.getQuantity();
    var updatedInventory = inventoryRepository.save(inventory);

    auditLogger.logInventoryUpdate(bookId, oldQuantity, command.quantity(), userContext);

    bookstoreMetrics.incrementInventoryUpdated();
    bookstoreMetrics.recordInventoryUpdateTime(timer);

    logger.info("Successfully updated inventory for book ID: {} by user: {}", bookId, userContext.username());

    return convertToDto(updatedInventory);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  @Caching(evict = {
      @CacheEvict(value = CacheConfig.BOOK_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.BOOK_SEARCH_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.POPULAR_BOOKS_CACHE, allEntries = true)
  })
  public List<InventoryDto> bulkUpdateInventory(List<BulkInventoryCommand> commands, UserContext userContext) {
    logger.info("Bulk updating inventory for {} books by user: {}", commands.size(), userContext.username());

    var results = new ArrayList<InventoryDto>();
    var failures = new ArrayList<BulkOperationException.BulkOperationFailure>();

    for (final var command : commands) {
      try {
        var inventory = inventoryRepository.findByBookId(command.bookId())
            .orElseThrow(() -> new BookNotFoundException(command.bookId()));

        if (command.quantity() < inventory.getReservedQuantity()) {
          var reason = String.format("Quantity %d below reserved amount %d",
              command.quantity(), inventory.getReservedQuantity());
          failures.add(new BulkOperationException.BulkOperationFailure(
              command.bookId(), reason, null));
          logger.warn("Skipping book ID {} - {}", command.bookId(), reason);
          continue;
        }

        inventory.setQuantity(command.quantity());

        if (command.lowStockThreshold() != null) {
          inventory.setLowStockThreshold(command.lowStockThreshold());
        }

        var book = inventory.getBook();
        book.setAvailable(inventory.isAvailable());

        var updatedInventory = inventoryRepository.save(inventory);
        results.add(convertToDto(updatedInventory));

      } catch (final Exception e) {
        logger.error("Failed to update inventory for book ID: {}", command.bookId(), e);
        failures.add(new BulkOperationException.BulkOperationFailure(
            command.bookId(), e.getMessage(), e));
      }
    }

    auditLogger.logBulkInventoryUpdate(results.size(), userContext);

    logger.info("Successfully bulk updated {} out of {} inventory records by user: {}",
        results.size(), commands.size(), userContext.username());

    if (!failures.isEmpty()) {
      throw new BulkOperationException(commands.size(), results.size(), failures);
    }

    return results;
  }

  @Transactional(readOnly = true)
  public boolean isBookAvailable(Long bookId, int requestedQuantity) {
    logger.debug("Checking availability for book ID: {} quantity: {}", bookId, requestedQuantity);

    return inventoryRepository.findByBookId(bookId)
        .map(inventory -> inventory.canReserve(requestedQuantity))
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public List<InventoryDto> getLowStockInventory() {
    logger.debug("Getting low stock inventory");

    return inventoryRepository.findLowStockInventory()
        .stream()
        .map(this::convertToDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InventoryDto> getOutOfStockInventory() {
    logger.debug("Getting out of stock inventory");

    return inventoryRepository.findOutOfStockInventory()
        .stream()
        .map(this::convertToDto)
        .toList();
  }

  public void reserveQuantity(Long bookId, int quantity, UserContext userContext) {
    logger.info("Reserving {} units for book ID: {} by user: {}", quantity, bookId, userContext.username());

    var inventory = inventoryRepository.findByBookId(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    if (!inventory.canReserve(quantity)) {
      throw new InsufficientInventoryException(bookId, quantity, inventory.getAvailableQuantity());
    }

    inventory.reserveQuantity(quantity);
    inventoryRepository.save(inventory);

    logger.info("Successfully reserved {} units for book ID: {}", quantity, bookId);
  }

  public void releaseReservedQuantity(Long bookId, int quantity, UserContext userContext) {
    logger.info("Releasing {} reserved units for book ID: {} by user: {}", quantity, bookId, userContext.username());

    var inventory = inventoryRepository.findByBookId(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    inventory.releaseReservedQuantity(quantity);
    inventoryRepository.save(inventory);

    logger.info("Successfully released {} reserved units for book ID: {}", quantity, bookId);
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
  public void adjustInventoryQuantity(Long bookId, int adjustment, UserContext userContext) {
    logger.info("Adjusting inventory by {} for book ID: {} by user: {}", adjustment, bookId, userContext.username());

    var inventory = inventoryRepository.findByBookId(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    inventory.adjustQuantity(adjustment);

    var book = inventory.getBook();
    book.setAvailable(inventory.isAvailable());

    inventoryRepository.save(inventory);

    logger.info("Successfully adjusted inventory by {} for book ID: {}", adjustment, bookId);
  }

  @Transactional(readOnly = true)
  public long getTotalInventoryCount() {
    logger.debug("Getting total inventory count");

    var count = inventoryRepository.getTotalInventoryCount();
    return count != null ? count : 0L;
  }

  @Transactional(readOnly = true)
  public long getTotalReservedCount() {
    logger.debug("Getting total reserved count");

    var count = inventoryRepository.getTotalReservedCount();
    return count != null ? count : 0L;
  }

  @Transactional(readOnly = true)
  public long countLowStockItems() {
    logger.debug("Counting low stock items");

    return inventoryRepository.countLowStockItems();
  }

  @Transactional(readOnly = true)
  public long countOutOfStockItems() {
    logger.debug("Counting out of stock items");

    return inventoryRepository.countOutOfStockItems();
  }

  private InventoryDto convertToDto(Inventory inventory) {
    var book = inventory.getBook();

    return new InventoryDto(
        book.getId(),
        book.getTitle(),
        inventory.getQuantity(),
        inventory.getReservedQuantity(),
        inventory.getAvailableQuantity(),
        inventory.getLowStockThreshold(),
        inventory.isLowStock(),
        inventory.getUpdatedAt()
    );
  }
}
