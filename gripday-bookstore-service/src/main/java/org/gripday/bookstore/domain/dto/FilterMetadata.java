package org.gripday.bookstore.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record FilterMetadata(
    List<CategoryFilter> categories,
    PriceRangeFilter priceRange,
    List<AuthorFilter> authors,
    AvailabilityFilter availability
) {}

record CategoryFilter(
    Long id,
    String name,
    int bookCount
) {}

record PriceRangeFilter(
    BigDecimal minPrice,
    BigDecimal maxPrice,
    BigDecimal currentMinPrice,
    BigDecimal currentMaxPrice
) {}

record AuthorFilter(
    String name,
    int bookCount
) {}

record AvailabilityFilter(
    int totalBooks,
    int availableBooks,
    int outOfStockBooks
) {}