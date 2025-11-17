package org.gripday.bookstore.catalog;

public record CategoryFilter(
    Long id,
    String name,
    int bookCount
) {

}
