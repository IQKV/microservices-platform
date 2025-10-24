package org.gripday.bookstore.domain.dto;

import java.util.List;

public record SearchMetadata(
    String query,
    int resultCount,
    List<String> suggestions,
    String searchType
) {}