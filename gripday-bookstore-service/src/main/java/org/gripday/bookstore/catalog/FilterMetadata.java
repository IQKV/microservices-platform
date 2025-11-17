package org.gripday.bookstore.catalog;

import java.util.List;

public record FilterMetadata(
    List<CategoryFilter> categories,
    PriceRangeFilter priceRange,
    List<AuthorFilter> authors,
    AvailabilityFilter availability
) {

}
