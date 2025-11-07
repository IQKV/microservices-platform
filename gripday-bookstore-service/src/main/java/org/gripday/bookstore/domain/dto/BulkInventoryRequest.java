package org.gripday.bookstore.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BulkInventoryRequest(
    @NotNull(message = "Book ID is required")
    Long bookId,

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    Integer quantity,

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    Integer lowStockThreshold
) {

}