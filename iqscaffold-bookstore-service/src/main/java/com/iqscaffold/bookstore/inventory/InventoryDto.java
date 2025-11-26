package com.iqscaffold.bookstore.inventory;

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
