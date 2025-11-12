package org.gripday.bookstore.domain.dto;

import java.util.List;

public record FilterMetadata(
    List<CategoryFilter> categories,
    PriceRangeFilter priceRange,
    List<AuthorFilter> authors,
    AvailabilityFilter availability
) {

}

