package org.gripday.bookstore.domain.dto;

import jakarta.validation.constraints.*;

public record UpdateInventoryRequest(
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must not be negative")
    @Max(value = 999999, message = "Quantity must not exceed 999999")
    Integer quantity,
    
    @Min(value = 0, message = "Low stock threshold must not be negative")
    @Max(value = 1000, message = "Low stock threshold must not exceed 1000")
    Integer lowStockThreshold
) {}