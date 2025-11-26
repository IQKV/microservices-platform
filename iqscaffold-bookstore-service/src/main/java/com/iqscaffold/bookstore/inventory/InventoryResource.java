package com.iqscaffold.bookstore.inventory;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookstore/inventory")
@Tag(name = "Inventory Information", description = "Public inventory operations including stock tracking and availability checks")
public class InventoryResource {

  private static final Logger logger = LoggerFactory.getLogger(InventoryResource.class);

  private final InventoryApplicationService inventoryApplicationService;

  public InventoryResource(final InventoryApplicationService inventoryApplicationService) {
    this.inventoryApplicationService = inventoryApplicationService;
  }

  @Operation(
      summary = "Get inventory information",
      description = "Retrieve current inventory details for a specific book including stock levels and availability"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Inventory information retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InventoryDto.class),
              examples = @ExampleObject(
                  name = "Inventory response",
                  value = """
                      {
                        "bookId": 1,
                        "bookTitle": "The Great Gatsby",
                        "quantity": 25,
                        "reservedQuantity": 3,
                        "availableQuantity": 22,
                        "lowStockThreshold": 5,
                        "lowStock": false,
                        "lastUpdated": "2024-01-15T10:30:00Z"
                      }
                      """
              )
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Book not found with the specified ID",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      )
  })
  @GetMapping("/{bookId}")
  public ResponseEntity<InventoryDto> getInventory(
      @Parameter(description = "Unique identifier of the book", required = true, example = "1")
      @PathVariable Long bookId) {
    logger.debug("Getting inventory for book ID: {}", bookId);

    var inventory = inventoryApplicationService.getInventory(bookId);
    return ResponseEntity.ok(inventory);
  }

  @GetMapping("/{bookId}/availability")
  public ResponseEntity<Boolean> checkAvailability(
      @PathVariable Long bookId,
      @RequestParam(defaultValue = "1") int quantity) {

    logger.debug("Checking availability for book ID: {} quantity: {}", bookId, quantity);

    var available = inventoryApplicationService.isBookAvailable(bookId, quantity);
    return ResponseEntity.ok(available);
  }

  @GetMapping("/low-stock")
  public ResponseEntity<List<InventoryDto>> getLowStockInventory() {
    logger.debug("Getting low stock inventory");

    var lowStockItems = inventoryApplicationService.getLowStockInventory();
    return ResponseEntity.ok(lowStockItems);
  }

  @GetMapping("/out-of-stock")
  public ResponseEntity<List<InventoryDto>> getOutOfStockInventory() {
    logger.debug("Getting out of stock inventory");

    var outOfStockItems = inventoryApplicationService.getOutOfStockInventory();
    return ResponseEntity.ok(outOfStockItems);
  }

  @GetMapping("/stats/total-count")
  public ResponseEntity<Long> getTotalInventoryCount() {
    logger.debug("Getting total inventory count");

    var totalCount = inventoryApplicationService.getTotalInventoryCount();
    return ResponseEntity.ok(totalCount);
  }

  @GetMapping("/stats/reserved-count")
  public ResponseEntity<Long> getTotalReservedCount() {
    logger.debug("Getting total reserved count");

    var reservedCount = inventoryApplicationService.getTotalReservedCount();
    return ResponseEntity.ok(reservedCount);
  }

  @GetMapping("/stats/low-stock-count")
  public ResponseEntity<Long> getLowStockCount() {
    logger.debug("Getting low stock count");

    var lowStockCount = inventoryApplicationService.countLowStockItems();
    return ResponseEntity.ok(lowStockCount);
  }

  @GetMapping("/stats/out-of-stock-count")
  public ResponseEntity<Long> getOutOfStockCount() {
    logger.debug("Getting out of stock count");

    var outOfStockCount = inventoryApplicationService.countOutOfStockItems();
    return ResponseEntity.ok(outOfStockCount);
  }
}
