package com.iqscaffold.bookstore.catalog;

import java.math.BigDecimal;

public record PriceRangeFilter(
    BigDecimal minPrice,
    BigDecimal maxPrice,
    BigDecimal currentMinPrice,
    BigDecimal currentMaxPrice
) {

}
