package org.gripday.bookstore.domain.dto;

record CategoryFilter(
    Long id,
    String name,
    int bookCount
) {

}
