package org.gripday.bookstore.domain.service;

import java.util.ArrayList;
import java.util.List;

import org.gripday.bookstore.domain.dto.BulkInventoryRequest;
import org.gripday.bookstore.domain.dto.InventoryDto;
import org.gripday.bookstore.domain.dto.UpdateInventoryRequest;
import org.gripday.bookstore.domain.dto.UserContext;
import org.gripday.bookstore.domain.exception.BookNotFoundException;
import org.gripday.bookstore.domain.exception.InsufficientInventoryException;
import org.gripday.bookstore.domain.exception.UnauthorizedOperationException;
import org.gripday.bookstore.infrastructure.config.CacheConfig;
import org.gripday.bookstore.infrastructure.entity.Inventory;
import org.gripday.bookstore.infrastructure.metrics.BookstoreMetrics;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.gripday.bookstore.infrastructure.repository.InventoryRepository;
import org.gripday.bookstore.infrastructure.security.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

  private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

  private final InventoryRepository inventoryRepository;
  private final BookRepository bookRepository;
  private final AuditLogger auditLogger;
  private final BookstoreMetrics bookstoreMetrics;

  public InventoryService(final InventoryRepository inventoryRepository, final BookRepository bookRepository,
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

  @Caching(evict = {
      @CacheEvict(value = CacheConfig.BOOK_CACHE, key = "#bookId"),
      @CacheEvict(value = CacheConfig.BOOK_SEARCH_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.POPULAR_BOOKS_CACHE, allEntries = true)
  })
  public InventoryDto updateInventory(Long bookId, UpdateInventoryRequest request, UserContext userContext) {
    logger.info("Updating inventory for book ID: {} by user: {}", bookId, userContext.username());

    var timer = bookstoreMetrics.startInventoryUpdateTimer();

    // Authorization check
    if (!userContext.isAdmin()) {
      auditLogger.logUnauthorizedAccess("update inventory", "INVENTORY", userContext);
      bookstoreMetrics.incrementUnauthorizedAccess();
      throw new UnauthorizedOperationException("update inventory", "ADMIN or SUPERADMIN");
    }

    var inventory = inventoryRepository.findByBookId(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    // Validate new quantity against reserved quantity
    if (request.quantity() < inventory.getReservedQuantity()) {
      throw new InsufficientInventoryException(
          "Cannot set quantity below reserved amount. Reserved: " + inventory.getReservedQuantity() +
              ", Requested: " + request.quantity()
      );
    }

    // Update inventory fields
    inventory.setQuantity(request.quantity());

    if (request.lowStockThreshold() != null) {
      inventory.setLowStockThreshold(request.lowStockThreshold());
    }

    // Update book availability based on inventory
    var book = inventory.getBook();
    book.setAvailable(inventory.isAvailable());

    var oldQuantity = inventory.getQuantity();
    var updatedInventory = inventoryRepository.save(inventory);

    // Audit log the inventory update
    auditLogger.logInventoryUpdate(bookId, oldQuantity, request.quantity(), userContext);

    // Record metrics
    bookstoreMetrics.incrementInventoryUpdated();
    bookstoreMetrics.recordInventoryUpdateTime(timer);

    logger.info("Successfully updated inventory for book ID: {} by user: {}", bookId, userContext.username());

    return convertToDto(updatedInventory);
  }

  @Caching(evict = {
      @CacheEvict(value = CacheConfig.BOOK_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.BOOK_SEARCH_CACHE, allEntries = true),
      @CacheEvict(value = CacheConfig.POPULAR_BOOKS_CACHE, allEntries = true)
  })
  public List<InventoryDto> bulkUpdateInventory(List<BulkInventoryRequest> requests, UserContext userContext) {
    logger.info("Bulk updating inventory for {} books by user: {}", requests.size(), userContext.username());

    // Authorization check
    if (!userContext.isAdmin()) {
      auditLogger.logUnauthorizedAccess("bulk update inventory", "INVENTORY", userContext);
      bookstoreMetrics.incrementUnauthorizedAccess();
      throw new UnauthorizedOperationException("bulk update inventory", "ADMIN or SUPERADMIN");
    }

    var results = new ArrayList<InventoryDto>();

    for (final var request : requests) {
      try {
        var inventory = inventoryRepository.findByBookId(request.bookId())
            .orElseThrow(() -> new BookNotFoundException(request.bookId()));

        // Validate new quantity against reserved quantity
        if (request.quantity() < inventory.getReservedQuantity()) {
          logger.warn("Skipping book ID {} - quantity {} below reserved amount {}",
              request.bookId(), request.quantity(), inventory.getReservedQuantity());
          continue;
        }

        // Update inventory fields
        inventory.setQuantity(request.quantity());

        if (request.lowStockThreshold() != null) {
          inventory.setLowStockThreshold(request.lowStockThreshold());
        }

        // Update book availability
        var book = inventory.getBook();
        book.setAvailable(inventory.isAvailable());

        var updatedInventory = inventoryRepository.save(inventory);
        results.add(convertToDto(updatedInventory));

      } catch (final Exception e) {
        logger.error("Failed to update inventory for book ID: {}", request.bookId(), e);
        // Continue with other updates
      }
    }

    // Audit log the bulk update
    auditLogger.logBulkInventoryUpdate(results.size(), userContext);

    logger.info("Successfully bulk updated {} out of {} inventory records by user: {}",
        results.size(), requests.size(), userContext.username());

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

  public void adjustInventoryQuantity(Long bookId, int adjustment, UserContext userContext) {
    logger.info("Adjusting inventory by {} for book ID: {} by user: {}", adjustment, bookId, userContext.username());

    // Authorization check
    if (!userContext.isAdmin()) {
      auditLogger.logUnauthorizedAccess("adjust inventory", "INVENTORY", userContext);
      throw new UnauthorizedOperationException("adjust inventory", "ADMIN or SUPERADMIN");
    }

    var inventory = inventoryRepository.findByBookId(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    inventory.adjustQuantity(adjustment);

    // Update book availability
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