package org.gripday.bookstore.catalog;

import java.math.BigDecimal;

public record BookSearchCriteria(
    String title,
    String author,
    String category,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Boolean availableOnly
) {

}
