package org.gripday.bookstore.domain.dto;

record AvailabilityFilter(
    int totalBooks,
    int availableBooks,
    int outOfStockBooks
) {

}
