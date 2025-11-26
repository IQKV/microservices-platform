package com.iqscaffold.bookstore.catalog;

public record CategoryFilter(
    Long id,
    String name,
    int bookCount
) {

}
