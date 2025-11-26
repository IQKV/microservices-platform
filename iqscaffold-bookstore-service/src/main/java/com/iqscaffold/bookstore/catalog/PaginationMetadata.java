package com.iqscaffold.bookstore.catalog;

public record PaginationMetadata(
    int currentPage,
    int totalPages,
    long totalElements,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious,
    String nextPageUrl,
    String previousPageUrl
) {

}
