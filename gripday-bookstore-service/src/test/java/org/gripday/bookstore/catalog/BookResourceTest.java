package org.gripday.bookstore.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookResource.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(org.gripday.bookstore.shared.TestSecurityConfig.class)
class BookResourceTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CatalogService catalogService;

  @MockBean
  private SearchService searchService;

  private BookDto createTestBookDto() {
    return new BookDto(
        1L,
        "Test Book",
        "Test Author",
        "9780123456786",
        "A test book description",
        new BigDecimal("29.99"),
        "Fiction",
        true,
        10,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  @Test
  void getBooks_ShouldReturnPagedBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(catalogService.findBooks(any(BookSearchCriteria.class), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].title").value("Test Book"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void getBooks_WithFilters_ShouldReturnFilteredBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(catalogService.findBooks(any(BookSearchCriteria.class), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books")
            .param("title", "Test")
            .param("author", "Author")
            .param("minPrice", "10.00")
            .param("maxPrice", "50.00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].title").value("Test Book"));
  }

  @Test
  void getBookById_WhenBookExists_ShouldReturnBook() throws Exception {
    var book = createTestBookDto();
    when(catalogService.findBookById(1L)).thenReturn(Optional.of(book));

    mockMvc.perform(get("/api/v1/bookstore/books/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test Book"))
        .andExpect(jsonPath("$.author").value("Test Author"));
  }

  @Test
  void getBookById_WhenBookNotFound_ShouldReturn404() throws Exception {
    when(catalogService.findBookById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/bookstore/books/999"))
        .andExpect(status().isNotFound());
  }

  @Test
  void searchBooks_ShouldReturnSearchResults() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(searchService.searchWithCriteria(any(BookSearchCriteria.class), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/search")
            .param("title", "Test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].title").value("Test Book"));
  }

  @Test
  void searchByTitle_ShouldReturnMatchingBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(searchService.searchByTitle(eq("Test"), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/search/title")
            .param("title", "Test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].title").value("Test Book"));
  }

  @Test
  void searchByAuthor_ShouldReturnMatchingBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(searchService.searchByAuthor(eq("Test Author"), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/search/author")
            .param("author", "Test Author"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].author").value("Test Author"));
  }

  @Test
  void searchByCategory_ShouldReturnMatchingBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(searchService.searchByCategory(eq("Fiction"), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/search/category")
            .param("category", "Fiction"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].categoryName").value("Fiction"));
  }

  @Test
  void searchByPriceRange_ShouldReturnMatchingBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(searchService.searchByPriceRange(any(BigDecimal.class), any(BigDecimal.class), any()))
        .thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/search/price-range")
            .param("minPrice", "20.00")
            .param("maxPrice", "40.00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].price").value(29.99));
  }

  @Test
  void getAvailableBooks_ShouldReturnAvailableBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(catalogService.findAvailableBooks(any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/available"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].available").value(true));
  }

  @Test
  void getBooksInStock_ShouldReturnBooksInStock() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(catalogService.findBooksInStock(any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/in-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].availableQuantity").value(10));
  }
}
