package org.gripday.bookstore.catalog;

import java.time.LocalDateTime;

public record CategoryDto(
    Long id,
    String name,
    String description,
    int bookCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
