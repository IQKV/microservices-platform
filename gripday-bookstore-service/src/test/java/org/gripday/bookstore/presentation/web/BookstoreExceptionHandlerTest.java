package org.gripday.bookstore.presentation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.bookstore.domain.dto.*;
import org.gripday.bookstore.domain.exception.*;
import org.gripday.bookstore.domain.service.BookService;
import org.gripday.bookstore.domain.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({BookResource.class, BookstoreExceptionHandler.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestSecurityConfig.class)
class BookstoreExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @MockBean
    private SearchService searchService;

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

    @Test
    void handleBookNotFoundException_ShouldReturn404WithProblemDetail() throws Exception {
        when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
            .thenThrow(new BookNotFoundException(999L));

        var request = new CreateBookRequest(
            "Test Book",
            "Test Author",
            "9780123456786",
            "Description",
            new BigDecimal("29.99"),
            1L,
            5
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userContext", userContext))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("The requested book could not be found"))
            .andExpect(jsonPath("$.detail").exists())
            .andExpect(jsonPath("$.instance").value("/api/v1/bookstore/books"))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.method").value("POST"))
            .andExpect(jsonPath("$.correlationId").exists())
            .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void handleCategoryNotFoundException_ShouldReturn404WithProblemDetail() throws Exception {
        when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
            .thenThrow(new CategoryNotFoundException(999L));

        var request = new CreateBookRequest(
            "Test Book",
            "Test Author",
            "978-0123456789",
            "Description",
            new BigDecimal("29.99"),
            999L, // Non-existent category
            5
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userContext", userContext))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.title").value("The requested category could not be found"));
    }

    @Test
    void handleInsufficientInventoryException_ShouldReturn409WithProblemDetail() throws Exception {
        when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
            .thenThrow(new InsufficientInventoryException(1L, 100, 5));

        var request = new CreateBookRequest(
            "Test Book",
            "Test Author",
            "978-0123456789",
            "Description",
            new BigDecimal("29.99"),
            1L,
            100 // Requesting more than available
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userContext", userContext))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("DOMAIN_INSUFFICIENT_INVENTORY"))
            .andExpect(jsonPath("$.title").value("Not enough inventory available for the requested operation"));
    }

    @Test
    void handleDuplicateIsbnException_ShouldReturn409WithProblemDetail() throws Exception {
        when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
            .thenThrow(new DuplicateIsbnException("9780123456786"));

        var request = new CreateBookRequest(
            "Test Book",
            "Test Author",
            "9780123456786", // Duplicate ISBN
            "Description",
            new BigDecimal("29.99"),
            1L,
            5
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userContext", userContext))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("DOMAIN_DUPLICATE_ISBN"))
            .andExpect(jsonPath("$.title").value("A book with this ISBN already exists in the catalog"));
    }

    @Test
    void handleUnauthorizedOperationException_ShouldReturn403WithProblemDetail() throws Exception {
        when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
            .thenThrow(new UnauthorizedOperationException("Insufficient privileges"));

        var request = new CreateBookRequest(
            "Test Book",
            "Test Author",
            "978-0123456789",
            "Description",
            new BigDecimal("29.99"),
            1L,
            5
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userContext", userContext))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("AUTH_INSUFFICIENT_PRIVILEGES"))
            .andExpect(jsonPath("$.title").value("You do not have sufficient privileges to perform this operation"));
    }

    @Test
    void handleValidationErrors_ShouldReturn400WithProblemDetailFields() throws Exception {
        var invalidRequest = new CreateBookRequest(
            "", // Invalid empty title
            "", // Invalid empty author
            "invalid-isbn", // Invalid ISBN format
            "Description",
            new BigDecimal("-10.00"), // Invalid negative price
            null, // Missing category ID
            -5 // Invalid negative quantity
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .requestAttr("userContext", userContext))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.title").value("Request validation failed"))
            .andExpect(jsonPath("$.detail").value("One or more fields contain invalid values"))
            .andExpect(jsonPath("$.fields").isArray())
            .andExpect(jsonPath("$.fields").isNotEmpty());
    }

    @Test
    void handleTypeMismatchException_ShouldReturn400WithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/bookstore/books/invalid-id")) // Non-numeric ID
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.title").value("Invalid parameter type"))
            .andExpect(jsonPath("$.detail").value("The provided parameter value has an incorrect type"))
            .andExpect(jsonPath("$.fields").isArray())
            .andExpect(jsonPath("$.fields[0].field").value("id"))
            .andExpect(jsonPath("$.fields[0].rejectedValue").value("invalid-id"));
    }

    @Test
    void handleIllegalArgumentException_ShouldReturn400WithProblemDetail() throws Exception {
        when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
            .thenThrow(new IllegalArgumentException("Invalid argument provided"));

        var request = new CreateBookRequest(
            "Test Book",
            "Test Author",
            "978-0123456789",
            "Description",
            new BigDecimal("29.99"),
            1L,
            5
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userContext", userContext))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.title").value("The provided argument is invalid"))
            .andExpect(jsonPath("$.detail").value("Invalid argument provided"));
    }

    @Test
    void handleGenericException_ShouldReturn500WithProblemDetail() throws Exception {
        when(bookService.createBook(any(CreateBookRequest.class), any(UserContext.class)))
            .thenThrow(new RuntimeException("Unexpected system error"));

        var request = new CreateBookRequest(
            "Test Book",
            "Test Author",
            "978-0123456789",
            "Description",
            new BigDecimal("29.99"),
            1L,
            5
        );
        var userContext = createAdminUserContext();

        mockMvc.perform(post("/api/v1/bookstore/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userContext", userContext))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("SYSTEM_INTERNAL_ERROR"))
            .andExpect(jsonPath("$.title").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.detail").value("Please try again later or contact support if the problem persists"));
    }
}