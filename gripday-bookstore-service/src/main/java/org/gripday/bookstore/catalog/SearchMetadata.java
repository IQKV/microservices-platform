package org.gripday.bookstore.catalog;

import java.util.List;

public record SearchMetadata(
    String query,
    int resultCount,
    List<String> suggestions,
    String searchType
) {

}
