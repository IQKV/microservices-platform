package org.gripday.bookstore.domain.dto;

import java.util.List;

public record BookCatalogResponse(
    List<BookDto> books,
    PaginationMetadata pagination,
    FilterMetadata filters,
    SearchMetadata search
) {

}