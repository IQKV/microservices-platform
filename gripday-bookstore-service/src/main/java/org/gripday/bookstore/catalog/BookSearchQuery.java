package org.gripday.bookstore.catalog;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Query parameters for searching books")
public record BookSearchQuery(
    @Schema(description = "Search by book title (partial match)", example = "Gatsby")
    String title,

    @Schema(description = "Search by author name (partial match)", example = "Fitzgerald")
    String author,

    @Schema(description = "Filter by category name", example = "Fiction")
    String category,

    @Schema(description = "Minimum price filter", example = "10.00")
    BigDecimal minPrice,

    @Schema(description = "Maximum price filter", example = "50.00")
    BigDecimal maxPrice,

    @Schema(description = "Filter only available books", example = "true")
    Boolean availableOnly
) {

}
