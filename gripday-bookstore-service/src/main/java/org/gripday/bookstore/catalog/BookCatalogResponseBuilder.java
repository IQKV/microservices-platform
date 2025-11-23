package org.gripday.bookstore.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class BookCatalogResponseBuilder {

  public BookCatalogResponse build(Page<BookDto> books, BookSearchQuery criteria) {
    var pagination = buildPaginationMetadata(books);
    var filters = buildFilterMetadata(criteria, books);
    var search = buildSearchMetadata(criteria, books);

    return new BookCatalogResponse(
        books.getContent(),
        pagination,
        filters,
        search
    );
  }

  public BookCatalogResponse buildWithUrls(Page<BookDto> books, BookSearchQuery criteria, String baseUrl) {
    var pagination = buildPaginationMetadataWithUrls(books, baseUrl);
    var filters = buildFilterMetadata(criteria, books);
    var search = buildSearchMetadata(criteria, books);

    return new BookCatalogResponse(
        books.getContent(),
        pagination,
        filters,
        search
    );
  }

  private PaginationMetadata buildPaginationMetadata(Page<BookDto> books) {
    return new PaginationMetadata(
        books.getNumber(),
        books.getTotalPages(),
        books.getTotalElements(),
        books.getSize(),
        books.hasNext(),
        books.hasPrevious(),
        null,
        null
    );
  }

  private PaginationMetadata buildPaginationMetadataWithUrls(Page<BookDto> books, String baseUrl) {
    String nextPageUrl = null;
    String previousPageUrl = null;

    if (books.hasNext()) {
      nextPageUrl = baseUrl + "?page=" + (books.getNumber() + 1) + "&size=" + books.getSize();
    }

    if (books.hasPrevious()) {
      previousPageUrl = baseUrl + "?page=" + (books.getNumber() - 1) + "&size=" + books.getSize();
    }

    return new PaginationMetadata(
        books.getNumber(),
        books.getTotalPages(),
        books.getTotalElements(),
        books.getSize(),
        books.hasNext(),
        books.hasPrevious(),
        nextPageUrl,
        previousPageUrl
    );
  }

  private FilterMetadata buildFilterMetadata(BookSearchQuery criteria, Page<BookDto> books) {
    List<CategoryFilter> categories = extractCategoryFilters(books);
    List<AuthorFilter> authors = extractAuthorFilters(books);
    PriceRangeFilter priceRange = extractPriceRangeFilter(criteria, books);
    AvailabilityFilter availability = extractAvailabilityFilter(books);

    return new FilterMetadata(categories, priceRange, authors, availability);
  }

  private List<CategoryFilter> extractCategoryFilters(Page<BookDto> books) {
    return books.getContent().stream()
        .filter(book -> book.categoryName() != null)
        .collect(Collectors.groupingBy(BookDto::categoryName, Collectors.counting()))
        .entrySet().stream()
        .map(entry -> new CategoryFilter(null, entry.getKey(), entry.getValue().intValue()))
        .collect(Collectors.toList());
  }

  private List<AuthorFilter> extractAuthorFilters(Page<BookDto> books) {
    return books.getContent().stream()
        .collect(Collectors.groupingBy(BookDto::author, Collectors.counting()))
        .entrySet().stream()
        .map(entry -> new AuthorFilter(entry.getKey(), entry.getValue().intValue()))
        .collect(Collectors.toList());
  }

  private PriceRangeFilter extractPriceRangeFilter(BookSearchQuery criteria, Page<BookDto> books) {
    if (books.isEmpty()) {
      return null;
    }

    var prices = books.getContent().stream()
        .map(BookDto::price)
        .collect(Collectors.toList());

    var minPrice = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    var maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

    return new PriceRangeFilter(
        minPrice,
        maxPrice,
        criteria.minPrice(),
        criteria.maxPrice()
    );
  }

  private AvailabilityFilter extractAvailabilityFilter(Page<BookDto> books) {
    var totalBooks = (int) books.getTotalElements();
    var availableBooks = (int) books.getContent().stream()
        .filter(BookDto::available)
        .count();
    var outOfStockBooks = totalBooks - availableBooks;

    return new AvailabilityFilter(totalBooks, availableBooks, outOfStockBooks);
  }

  private SearchMetadata buildSearchMetadata(BookSearchQuery criteria, Page<BookDto> books) {
    String query = buildQueryString(criteria);
    String searchType = determineSearchType(criteria);

    return new SearchMetadata(
        query,
        (int) books.getTotalElements(),
        List.of(),
        searchType
    );
  }

  private String buildQueryString(BookSearchQuery criteria) {
    if (criteria.title() != null) {
      return criteria.title();
    }
    if (criteria.author() != null) {
      return criteria.author();
    }
    if (criteria.category() != null) {
      return criteria.category();
    }
    return null;
  }

  private String determineSearchType(BookSearchQuery criteria) {
    if (criteria.title() != null) {
      return "title";
    }
    if (criteria.author() != null) {
      return "author";
    }
    if (criteria.category() != null) {
      return "category";
    }
    if (criteria.minPrice() != null || criteria.maxPrice() != null) {
      return "price-range";
    }
    return "catalog";
  }
}
