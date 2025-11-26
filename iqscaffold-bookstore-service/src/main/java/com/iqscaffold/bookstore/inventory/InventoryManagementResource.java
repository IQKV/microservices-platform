package com.iqscaffold.bookstore.inventory;

import jakarta.validation.Valid;
import java.util.List;

import com.iqscaffold.bookstore.shared.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookstore/admin/inventory")
@Tag(name = "Inventory Management (Admin)", description = "Administrative operations for managing book inventory - requires ADMIN or SUPERADMIN role")
public class InventoryManagementResource {

  private static final Logger logger = LoggerFactory.getLogger(InventoryManagementResource.class);

  private final InventoryApplicationService inventoryApplicationService;

  public InventoryManagementResource(final InventoryApplicationService inventoryApplicationService) {
    this.inventoryApplicationService = inventoryApplicationService;
  }

  @Operation(
      summary = "Update inventory quantity (Admin only)",
      description = "Update the inventory quantity for a specific book. Requires ADMIN or SUPERADMIN role.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Inventory updated successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InventoryDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data or validation errors",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Authentication required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
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
  @PutMapping("/{bookId}")
  public ResponseEntity<InventoryDto> updateInventory(
      @Parameter(description = "Unique identifier of the book", required = true, example = "1")
      @PathVariable Long bookId,
      @Parameter(description = "Inventory update request data", required = true)
      @Valid @RequestBody UpdateInventoryCommand command,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Updating inventory for book ID: {} by user: {}", bookId, userContext.username());

    var updatedInventory = inventoryApplicationService.updateInventory(bookId, command, userContext);
    return ResponseEntity.ok(updatedInventory);
  }

  @Operation(
      summary = "Bulk update inventory (Admin only)",
      description = "Update inventory quantities for multiple books in a single operation. Requires ADMIN or SUPERADMIN role.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Inventory updated successfully for all books",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InventoryDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data or validation errors",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Authentication required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      )
  })
  @PostMapping("/bulk-update")
  public ResponseEntity<List<InventoryDto>> bulkUpdateInventory(
      @Parameter(description = "List of inventory update requests", required = true)
      @Valid @RequestBody List<BulkInventoryCommand> commands,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Bulk updating inventory for {} books by user: {}", commands.size(), userContext.username());

    var updatedInventories = inventoryApplicationService.bulkUpdateInventory(commands, userContext);
    return ResponseEntity.ok(updatedInventories);
  }

  @Operation(
      summary = "Reserve inventory quantity (Admin only)",
      description = "Reserve a specific quantity of books for pending orders. Requires ADMIN or SUPERADMIN role.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Inventory reserved successfully"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Insufficient inventory available for reservation",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Authentication required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
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
  @PostMapping("/{bookId}/reserve")
  public ResponseEntity<Void> reserveQuantity(
      @Parameter(description = "Unique identifier of the book", required = true, example = "1")
      @PathVariable Long bookId,
      @Parameter(description = "Quantity to reserve", required = true, example = "5")
      @RequestParam int quantity,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Reserving {} units for book ID: {} by user: {}", quantity, bookId, userContext.username());

    inventoryApplicationService.reserveQuantity(bookId, quantity, userContext);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Release reserved inventory (Admin only)",
      description = "Release previously reserved inventory back to available stock. Requires ADMIN or SUPERADMIN role.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Reserved inventory released successfully"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid quantity or insufficient reserved inventory",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Authentication required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
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
  @PostMapping("/{bookId}/release")
  public ResponseEntity<Void> releaseReservedQuantity(
      @Parameter(description = "Unique identifier of the book", required = true, example = "1")
      @PathVariable Long bookId,
      @Parameter(description = "Quantity to release", required = true, example = "5")
      @RequestParam int quantity,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Releasing {} reserved units for book ID: {} by user: {}", quantity, bookId, userContext.username());

    inventoryApplicationService.releaseReservedQuantity(bookId, quantity, userContext);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Adjust inventory quantity (Admin only)",
      description = "Adjust inventory by a positive or negative amount (e.g., for corrections, damages, returns). Requires ADMIN or SUPERADMIN role.",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Inventory adjusted successfully"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid adjustment amount",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Authentication required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Insufficient permissions - ADMIN role required",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)
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
  @PostMapping("/{bookId}/adjust")
  public ResponseEntity<Void> adjustInventoryQuantity(
      @Parameter(description = "Unique identifier of the book", required = true, example = "1")
      @PathVariable Long bookId,
      @Parameter(description = "Adjustment amount (positive to add, negative to subtract)", required = true, example = "10")
      @RequestParam int adjustment,
      @Parameter(hidden = true)
      @RequestAttribute("userContext") UserContext userContext) {

    logger.info("Adjusting inventory by {} for book ID: {} by user: {}", adjustment, bookId, userContext.username());

    inventoryApplicationService.adjustInventoryQuantity(bookId, adjustment, userContext);
    return ResponseEntity.ok().build();
  }
}
