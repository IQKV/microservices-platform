package org.gripday.bookstore.domain.dto;

import java.time.LocalDateTime;

public record InventoryDto(
    Long bookId,
    String bookTitle,
    int quantity,
    int reservedQuantity,
    int availableQuantity,
    int lowStockThreshold,
    boolean lowStock,
    LocalDateTime lastUpdated
) {

}