package org.gripday.bookstore.presentation.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.bookstore.domain.dto.BookDto;
import org.gripday.bookstore.domain.dto.BookSearchCriteria;
import org.gripday.bookstore.domain.dto.CreateBookRequest;
import org.gripday.bookstore.domain.dto.UpdateBookRequest;
import org.gripday.bookstore.domain.dto.UserContext;
import org.gripday.bookstore.domain.exception.BookNotFoundException;
import org.gripday.bookstore.domain.exception.DuplicateIsbnException;
import org.gripday.bookstore.domain.exception.UnauthorizedOperationException;
import org.gripday.bookstore.domain.service.BookService;
import org.gripday.bookstore.domain.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookResource.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestSecurityConfig.class)
class BookResourceTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private BookService bookService;

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

  private UserContext createAdminUserContext() {
    return new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ADMIN"),
        Set.of("BOOK_WRITE"),
        "IT",
        "org1",
        Map.of()
    );
  }

  private UserContext createRegularUserContext() {
    return new UserContext(
        2L,
        "user",
        "user@example.com",
        Set.of("USER"),
        Set.of("BOOK_READ"),
        "Sales",
        "org1",
        Map.of()
    );
  }

  @Test
  void getBooks_ShouldReturnPagedBooks() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(bookService.findBooks(any(BookSearchCriteria.class), any())).thenReturn(page);

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

    when(bookService.findBooks(any(BookSearchCriteria.class), any())).thenReturn(page);

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
    when(bookService.findBookById(1L)).thenReturn(Optional.of(book));

    mockMvc.perform(get("/api/v1/bookstore/books/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test Book"))
        .andExpect(jsonPath("$.author").value("Test Author"));
  }

  @Test
  void getBookById_WhenBookNotFound_ShouldReturn404() throws Exception {
    when(bookService.findBookById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/bookstore/books/999"))
        .andExpect(status().isNotFound());
  }

  @Test
  void createBook_WithValidRequestAndAdminUser_ShouldCreateBook() throws Exception {
    var request = new CreateBookRequest(
        "New Book",
        "New Author",
        "9780123456786",
        "A new book description",
        new BigDecimal("39.99"),
        1L,
        5
    );
    var createdBook = createTestBookDto();
    var userContext = createAdminUserContext();

    when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
        .thenReturn(createdBook);

    mockMvc.perform(post("/api/v1/bookstore/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Test Book"));
  }

  @Test
  void createBook_WithInvalidRequest_ShouldReturn400() throws Exception {
    var invalidRequest = new CreateBookRequest(
        "", // Invalid empty title
        "Author",
        "invalid-isbn",
        "Description",
        new BigDecimal("-10.00"), // Invalid negative price
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void createBook_WithDuplicateIsbn_ShouldReturn409() throws Exception {
    var request = new CreateBookRequest(
        "New Book",
        "New Author",
        "9780123456786", // Duplicate ISBN
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
        .thenThrow(new DuplicateIsbnException("9780123456786"));

    mockMvc.perform(post("/api/v1/bookstore/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DOMAIN_DUPLICATE_ISBN"));
  }

  @Test
  void createBook_WithUnauthorizedUser_ShouldReturn403() throws Exception {
    var request = new CreateBookRequest(
        "New Book",
        "New Author",
        "9780123456786",
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createRegularUserContext();

    when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
        .thenThrow(new UnauthorizedOperationException("Insufficient privileges"));

    mockMvc.perform(post("/api/v1/bookstore/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_INSUFFICIENT_PRIVILEGES"));
  }

  @Test
  void updateBook_WithValidRequestAndAdminUser_ShouldUpdateBook() throws Exception {
    var request = new UpdateBookRequest(
        "Updated Book",
        "Updated Author",
        "Updated description",
        new BigDecimal("49.99"),
        1L
    );
    var updatedBook = createTestBookDto();
    var userContext = createAdminUserContext();

    when(bookService.updateBook(eq(1L), any(UpdateBookRequest.class), any(UserContext.class)))
        .thenReturn(updatedBook);

    mockMvc.perform(put("/api/v1/bookstore/books/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test Book"));
  }

  @Test
  void updateBook_WhenBookNotFound_ShouldReturn404() throws Exception {
    var request = new UpdateBookRequest(
        "Updated Book",
        "Updated Author",
        "Updated description",
        new BigDecimal("49.99"),
        1L
    );
    var userContext = createAdminUserContext();

    when(bookService.updateBook(eq(999L), any(UpdateBookRequest.class), any(UserContext.class)))
        .thenThrow(new BookNotFoundException(999L));

    mockMvc.perform(put("/api/v1/bookstore/books/999")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void deleteBook_WithAdminUser_ShouldDeleteBook() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(delete("/api/v1/bookstore/books/1")
            .requestAttr("userContext", userContext))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteBook_WhenBookNotFound_ShouldReturn404() throws Exception {
    var userContext = createAdminUserContext();

    doThrow(new BookNotFoundException(999L))
        .when(bookService).deleteBook(eq(999L), any(UserContext.class));

    mockMvc.perform(delete("/api/v1/bookstore/books/999")
            .requestAttr("userContext", userContext))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
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

    when(bookService.findAvailableBooks(any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/available"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].available").value(true));
  }

  @Test
  void getBooksInStock_ShouldReturnBooksInStock() throws Exception {
    var books = List.of(createTestBookDto());
    var page = new PageImpl<>(books, PageRequest.of(0, 20), 1);

    when(bookService.findBooksInStock(any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/bookstore/books/in-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].availableQuantity").value(10));
  }
}