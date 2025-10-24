package org.gripday.bookstore.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookDto(
    Long id,
    String title,
    String author,
    String isbn,
    String description,
    BigDecimal price,
    String categoryName,
    boolean available,
    int availableQuantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}