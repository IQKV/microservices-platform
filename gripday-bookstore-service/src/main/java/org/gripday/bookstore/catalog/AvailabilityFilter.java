package org.gripday.bookstore.catalog;

public record AvailabilityFilter(
    int totalBooks,
    int availableBooks,
    int outOfStockBooks
) {

}
