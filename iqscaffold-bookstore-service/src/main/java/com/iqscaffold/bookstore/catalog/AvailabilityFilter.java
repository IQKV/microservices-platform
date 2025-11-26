package com.iqscaffold.bookstore.catalog;

public record AvailabilityFilter(
    int totalBooks,
    int availableBooks,
    int outOfStockBooks
) {

}
