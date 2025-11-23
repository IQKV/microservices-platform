package org.gripday.bookstore.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.bookstore.shared.UnauthorizedOperationException;
import org.gripday.bookstore.shared.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookManagementResource.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(org.gripday.bookstore.shared.TestSecurityConfig.class)
class BookManagementResourceTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CatalogApplicationService CatalogApplicationService;

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
  void createBook_WithValidRequestAndAdminUser_ShouldCreateBook() throws Exception {
    var request = new CreateBookCommand(
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

    when(CatalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenReturn(createdBook);

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Test Book"));
  }

  @Test
  void createBook_WithInvalidRequest_ShouldReturn400() throws Exception {
    var invalidRequest = new CreateBookCommand(
        "", // Invalid empty title
        "Author",
        "invalid-isbn",
        "Description",
        new BigDecimal("-10.00"), // Invalid negative price
        1L,
        5
    );
    var userContext = createAdminUserContext();

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .requestAttr("userContext", userContext))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void createBook_WithDuplicateIsbn_ShouldReturn409() throws Exception {
    var request = new CreateBookCommand(
        "New Book",
        "New Author",
        "9780123456786",
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createAdminUserContext();

    when(CatalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new DuplicateIsbnException("9780123456786"));

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DOMAIN_DUPLICATE_ISBN"));
  }

  @Test
  void createBook_WithUnauthorizedUser_ShouldReturn403() throws Exception {
    var request = new CreateBookCommand(
        "New Book",
        "New Author",
        "9780123456786",
        "Description",
        new BigDecimal("29.99"),
        1L,
        5
    );
    var userContext = createRegularUserContext();

    when(CatalogApplicationService.createBook(any(CreateBookCommand.class), any(UserContext.class)))
        .thenThrow(new UnauthorizedOperationException("Insufficient privileges"));

    mockMvc.perform(post("/api/v1/bookstore/admin/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_INSUFFICIENT_PRIVILEGES"));
  }

  @Test
  void updateBook_WithValidRequestAndAdminUser_ShouldUpdateBook() throws Exception {
    var request = new UpdateBookCommand(
        "Updated Book",
        "Updated Author",
        "Updated description",
        new BigDecimal("49.99"),
        1L
    );
    var updatedBook = createTestBookDto();
    var userContext = createAdminUserContext();

    when(CatalogApplicationService.updateBook(eq(1L), any(UpdateBookCommand.class), any(UserContext.class)))
        .thenReturn(updatedBook);

    mockMvc.perform(put("/api/v1/bookstore/admin/books/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test Book"));
  }

  @Test
  void updateBook_WhenBookNotFound_ShouldReturn404() throws Exception {
    var request = new UpdateBookCommand(
        "Updated Book",
        "Updated Author",
        "Updated description",
        new BigDecimal("49.99"),
        1L
    );
    var userContext = createAdminUserContext();

    when(CatalogApplicationService.updateBook(eq(999L), any(UpdateBookCommand.class), any(UserContext.class)))
        .thenThrow(new BookNotFoundException(999L));

    mockMvc.perform(put("/api/v1/bookstore/admin/books/999")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .requestAttr("userContext", userContext))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void deleteBook_WithAdminUser_ShouldDeleteBook() throws Exception {
    var userContext = createAdminUserContext();

    mockMvc.perform(delete("/api/v1/bookstore/admin/books/1")
            .requestAttr("userContext", userContext))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteBook_WhenBookNotFound_ShouldReturn404() throws Exception {
    var userContext = createAdminUserContext();

    doThrow(new BookNotFoundException(999L))
        .when(CatalogApplicationService).deleteBook(eq(999L), any(UserContext.class));

    mockMvc.perform(delete("/api/v1/bookstore/admin/books/999")
            .requestAttr("userContext", userContext))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }
}
