package org.gripday.bookstore.domain.dto;

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