package org.gripday.bookstore.domain.dto;

import java.math.BigDecimal;

record PriceRangeFilter(
    BigDecimal minPrice,
    BigDecimal maxPrice,
    BigDecimal currentMinPrice,
    BigDecimal currentMaxPrice
) {

}
