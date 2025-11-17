package org.gripday.bookstore.catalog;

record AvailabilityFilter(
    int totalBooks,
    int availableBooks,
    int outOfStockBooks
) {

}
