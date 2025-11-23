package org.gripday.bookstore.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Command to update inventory for a single book in bulk operation")
public record BulkInventoryCommand(
    @Schema(description = "ID of the book to update", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Book ID is required")
    Long bookId,

    @Schema(description = "New quantity for the inventory", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    Integer quantity,

    @Schema(description = "Low stock threshold for alerts", example = "10")
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    Integer lowStockThreshold
) {

}
