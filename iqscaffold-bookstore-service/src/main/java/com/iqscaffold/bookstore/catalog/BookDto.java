package com.iqscaffold.bookstore.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Book information with inventory details")
public record BookDto(
    @Schema(description = "Unique identifier of the book", example = "1")
    Long id,

    @Schema(description = "Title of the book", example = "The Great Gatsby")
    String title,

    @Schema(description = "Author of the book", example = "F. Scott Fitzgerald")
    String author,

    @Schema(description = "ISBN-13 of the book", example = "978-0-7432-7356-5")
    String isbn,

    @Schema(description = "Detailed description of the book", example = "A classic American novel set in the Jazz Age")
    String description,

    @Schema(description = "Price of the book in USD", example = "12.99")
    BigDecimal price,

    @Schema(description = "Category name of the book", example = "Fiction")
    String categoryName,

    @Schema(description = "Whether the book is available for purchase", example = "true")
    boolean available,

    @Schema(description = "Number of copies available in inventory", example = "15")
    int availableQuantity,

    @Schema(description = "Timestamp when the book was created", example = "2024-01-15T10:30:00Z")
    LocalDateTime createdAt,

    @Schema(description = "Timestamp when the book was last updated", example = "2024-01-15T10:30:00Z")
    LocalDateTime updatedAt
) {

}
