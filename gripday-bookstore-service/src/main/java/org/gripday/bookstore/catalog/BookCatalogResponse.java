package org.gripday.bookstore.catalog;

import java.util.List;

public record BookCatalogResponse(
    List<BookDto> books,
    PaginationMetadata pagination,
    FilterMetadata filters,
    SearchMetadata search
) {

}
