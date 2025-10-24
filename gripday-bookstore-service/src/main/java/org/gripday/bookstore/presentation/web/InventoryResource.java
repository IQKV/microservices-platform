package org.gripday.bookstore.presentation.web;

import jakarta.validation.Valid;
import org.gripday.bookstore.domain.dto.*;
import org.gripday.bookstore.domain.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookstore/inventory")
public class InventoryResource {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryResource.class);
    
    private final InventoryService inventoryService;
    
    public InventoryResource(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    @GetMapping("/{bookId}")
    public ResponseEntity<InventoryDto> getInventory(@PathVariable Long bookId) {
        logger.debug("Getting inventory for book ID: {}", bookId);
        
        var inventory = inventoryService.getInventory(bookId);
        return ResponseEntity.ok(inventory);
    }
    
    @GetMapping("/{bookId}/availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "1") int quantity) {
        
        logger.debug("Checking availability for book ID: {} quantity: {}", bookId, quantity);
        
        var available = inventoryService.isBookAvailable(bookId, quantity);
        return ResponseEntity.ok(available);
    }
    
    @PutMapping("/{bookId}")
    public ResponseEntity<InventoryDto> updateInventory(
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateInventoryRequest request,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Updating inventory for book ID: {} by user: {}", bookId, userContext.username());
        
        var updatedInventory = inventoryService.updateInventory(bookId, request, userContext);
        return ResponseEntity.ok(updatedInventory);
    }
    
    @PostMapping("/bulk-update")
    public ResponseEntity<List<InventoryDto>> bulkUpdateInventory(
            @Valid @RequestBody List<BulkInventoryRequest> requests,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Bulk updating inventory for {} books by user: {}", requests.size(), userContext.username());
        
        var updatedInventories = inventoryService.bulkUpdateInventory(requests, userContext);
        return ResponseEntity.ok(updatedInventories);
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryDto>> getLowStockInventory() {
        logger.debug("Getting low stock inventory");
        
        var lowStockItems = inventoryService.getLowStockInventory();
        return ResponseEntity.ok(lowStockItems);
    }
    
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryDto>> getOutOfStockInventory() {
        logger.debug("Getting out of stock inventory");
        
        var outOfStockItems = inventoryService.getOutOfStockInventory();
        return ResponseEntity.ok(outOfStockItems);
    }
    
    @PostMapping("/{bookId}/reserve")
    public ResponseEntity<Void> reserveQuantity(
            @PathVariable Long bookId,
            @RequestParam int quantity,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Reserving {} units for book ID: {} by user: {}", quantity, bookId, userContext.username());
        
        inventoryService.reserveQuantity(bookId, quantity, userContext);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{bookId}/release")
    public ResponseEntity<Void> releaseReservedQuantity(
            @PathVariable Long bookId,
            @RequestParam int quantity,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Releasing {} reserved units for book ID: {} by user: {}", quantity, bookId, userContext.username());
        
        inventoryService.releaseReservedQuantity(bookId, quantity, userContext);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{bookId}/adjust")
    public ResponseEntity<Void> adjustInventoryQuantity(
            @PathVariable Long bookId,
            @RequestParam int adjustment,
            @RequestAttribute("userContext") UserContext userContext) {
        
        logger.info("Adjusting inventory by {} for book ID: {} by user: {}", adjustment, bookId, userContext.username());
        
        inventoryService.adjustInventoryQuantity(bookId, adjustment, userContext);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/stats/total-count")
    public ResponseEntity<Long> getTotalInventoryCount() {
        logger.debug("Getting total inventory count");
        
        var totalCount = inventoryService.getTotalInventoryCount();
        return ResponseEntity.ok(totalCount);
    }
    
    @GetMapping("/stats/reserved-count")
    public ResponseEntity<Long> getTotalReservedCount() {
        logger.debug("Getting total reserved count");
        
        var reservedCount = inventoryService.getTotalReservedCount();
        return ResponseEntity.ok(reservedCount);
    }
    
    @GetMapping("/stats/low-stock-count")
    public ResponseEntity<Long> getLowStockCount() {
        logger.debug("Getting low stock count");
        
        var lowStockCount = inventoryService.countLowStockItems();
        return ResponseEntity.ok(lowStockCount);
    }
    
    @GetMapping("/stats/out-of-stock-count")
    public ResponseEntity<Long> getOutOfStockCount() {
        logger.debug("Getting out of stock count");
        
        var outOfStockCount = inventoryService.countOutOfStockItems();
        return ResponseEntity.ok(outOfStockCount);
    }
}