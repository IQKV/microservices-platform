package org.gripday.bookstore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.bookstore.domain.dto.CreateBookRequest;
import org.gripday.bookstore.infrastructure.entity.Category;
import org.gripday.bookstore.infrastructure.repository.BookRepository;
import org.gripday.bookstore.infrastructure.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests verifying platform integration including: - Gateway Service routing and BFF pattern - JWT authentication flow with Auth Service - Unified API access pattern for React 19
 * frontend - Observability stack integration (Prometheus, Grafana)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BookstorePlatformIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  private Category testCategory;
  private String baseUrl;
  private String mockJwtToken;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port;

    // Clean up existing data
    bookRepository.deleteAll();
    categoryRepository.deleteAll();

    // Create test category
    testCategory = new Category();
    testCategory.setName("Integration Test");
    testCategory.setDescription("Category for integration testing");
    testCategory = categoryRepository.save(testCategory);

    // Create mock JWT token for testing (in real scenario, this would come from Auth Service)
    mockJwtToken = createMockJwtToken();
  }

  @Test
  void shouldVerifyGatewayServiceRoutingIntegration() throws Exception {
    // Test that bookstore endpoints are accessible through expected routing patterns
    // This simulates how Gateway Service would route requests to bookstore service

    // Test GET /api/v1/bookstore/books endpoint (public access)
    mockMvc.perform(get("/api/v1/bookstore/books")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.pageable").exists())
        .andExpect(jsonPath("$.totalElements").exists());

    // Test that admin endpoints require authentication
    mockMvc.perform(post("/api/v1/bookstore/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBookRequestJson()))
        .andExpect(status().isUnauthorized());

    // Test with mock JWT token (simulating Gateway Service authentication)
    mockMvc.perform(post("/api/v1/bookstore/books")
            .header("Authorization", "Bearer " + mockJwtToken)
            .header("X-User-Context", createUserContextHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBookRequestJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Platform Integration Test Book"));
  }

  @Test
  void shouldVerifyJwtAuthenticationFlowIntegration() throws Exception {
    // Test JWT token validation and user context extraction
    // This simulates the authentication flow between Gateway Service and Auth Service

    // Test with valid JWT token and user context
    var validUserContext = """
        {
            "userId": 1,
            "username": "admin",
            "email": "admin@bookstore.com",
            "roles": ["ADMIN"],
            "permissions": ["BOOK_CREATE", "BOOK_UPDATE", "INVENTORY_UPDATE"],
            "department": "Management",
            "organizationId": "BOOKSTORE_ORG",
            "customClaims": {}
        }
        """;

    mockMvc.perform(post("/api/v1/bookstore/books")
            .header("Authorization", "Bearer " + mockJwtToken)
            .header("X-User-Context", validUserContext)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBookRequestJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("Platform Integration Test Book"));

    // Test with invalid/expired JWT token
    mockMvc.perform(post("/api/v1/bookstore/books")
            .header("Authorization", "Bearer invalid_token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBookRequestJson()))
        .andExpect(status().isUnauthorized());

    // Test with valid token but insufficient permissions
    var userContext = """
        {
            "userId": 2,
            "username": "user",
            "email": "user@bookstore.com",
            "roles": ["USER"],
            "permissions": ["BOOK_READ"],
            "department": "Customer",
            "organizationId": "BOOKSTORE_ORG",
            "customClaims": {}
        }
        """;

    mockMvc.perform(post("/api/v1/bookstore/books")
            .header("Authorization", "Bearer " + mockJwtToken)
            .header("X-User-Context", userContext)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBookRequestJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldVerifyUnifiedApiAccessPatternForReactFrontend() throws Exception {
    // Test API responses optimized for React 19 frontend consumption
    // This verifies the BFF pattern implementation

    // Create test book first
    var createResponse = mockMvc.perform(post("/api/v1/bookstore/books")
            .header("Authorization", "Bearer " + mockJwtToken)
            .header("X-User-Context", createUserContextHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBookRequestJson()))
        .andExpect(status().isCreated())
        .andReturn();

    var bookId = extractBookIdFromResponse(createResponse.getResponse().getContentAsString());

    // Test paginated book listing with frontend-optimized response
    mockMvc.perform(get("/api/v1/bookstore/books")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.pageable.pageNumber").value(0))
        .andExpect(jsonPath("$.pageable.pageSize").value(10))
        .andExpect(jsonPath("$.totalElements").exists())
        .andExpect(jsonPath("$.totalPages").exists())
        .andExpect(jsonPath("$.first").exists())
        .andExpect(jsonPath("$.last").exists())
        .andExpect(jsonPath("$.numberOfElements").exists());

    // Test individual book details with complete information
    mockMvc.perform(get("/api/v1/bookstore/books/" + bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(bookId))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.author").exists())
        .andExpect(jsonPath("$.isbn").exists())
        .andExpect(jsonPath("$.price").exists())
        .andExpect(jsonPath("$.categoryName").exists())
        .andExpect(jsonPath("$.available").exists())
        .andExpect(jsonPath("$.availableQuantity").exists())
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());

    // Test search functionality with frontend-friendly filters
    mockMvc.perform(get("/api/v1/bookstore/books/search")
            .param("title", "Platform")
            .param("category", "Integration Test")
            .param("minPrice", "10.00")
            .param("maxPrice", "50.00")
            .param("availableOnly", "true")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].title").value("Platform Integration Test Book"));

    // Test inventory information access
    mockMvc.perform(get("/api/v1/bookstore/inventory/" + bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookId").value(bookId))
        .andExpect(jsonPath("$.bookTitle").exists())
        .andExpect(jsonPath("$.quantity").exists())
        .andExpect(jsonPath("$.availableQuantity").exists())
        .andExpect(jsonPath("$.lowStock").exists())
        .andExpect(jsonPath("$.lastUpdated").exists());
  }

  @Test
  void shouldVerifyObservabilityStackIntegration() throws Exception {
    // Test Prometheus metrics endpoints
    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/plain;version=0.0.4;charset=utf-8"))
        .andExpect(content().string(containsString("bookstore_service")))
        .andExpect(content().string(containsString("http_server_requests")))
        .andExpect(content().string(containsString("jvm_memory")));

    // Test health check endpoint (used by Grafana and monitoring)
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components.db.status").value("UP"))
        .andExpect(jsonPath("$.components.redis.status").value("UP"));

    // Test metrics endpoint
    mockMvc.perform(get("/actuator/metrics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.names").isArray())
        .andExpect(jsonPath("$.names[*]").value(hasItem("http.server.requests")))
        .andExpect(jsonPath("$.names[*]").value(hasItem("jvm.memory.used")));

    // Test specific bookstore metrics
    mockMvc.perform(get("/actuator/metrics/http.server.requests"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("http.server.requests"))
        .andExpect(jsonPath("$.measurements").isArray());

    // Perform some operations to generate metrics
    mockMvc.perform(get("/api/v1/bookstore/books"));
    mockMvc.perform(post("/api/v1/bookstore/books")
        .header("Authorization", "Bearer " + mockJwtToken)
        .header("X-User-Context", createUserContextHeader())
        .contentType(MediaType.APPLICATION_JSON)
        .content(createBookRequestJson()));

    // Verify metrics are being collected
    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("http_server_requests_seconds_count")));
  }

  @Test
  void shouldVerifyCorrelationIdPropagation() throws Exception {
    // Test correlation ID propagation for distributed tracing
    var correlationId = "test-correlation-" + System.currentTimeMillis();

    mockMvc.perform(get("/api/v1/bookstore/books")
            .header("X-Correlation-ID", correlationId))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Correlation-ID", correlationId));

    // Test with admin operation
    mockMvc.perform(post("/api/v1/bookstore/books")
            .header("Authorization", "Bearer " + mockJwtToken)
            .header("X-User-Context", createUserContextHeader())
            .header("X-Correlation-ID", correlationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBookRequestJson()))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Correlation-ID", correlationId));
  }

  @Test
  void shouldVerifyErrorHandlingAndResponseFormat() throws Exception {
    // Test consistent error response format across the platform

    // Test 404 error
    mockMvc.perform(get("/api/v1/bookstore/books/999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.error.message").exists())
        .andExpect(jsonPath("$.error.timestamp").exists())
        .andExpect(jsonPath("$.error.path").value("/api/v1/bookstore/books/999999"));

    // Test validation error
    var invalidBookRequest = """
        {
            "title": "",
            "author": "",
            "isbn": "invalid-isbn",
            "price": -10.00,
            "categoryId": null,
            "initialQuantity": -5
        }
        """;

    mockMvc.perform(post("/api/v1/bookstore/books")
            .header("Authorization", "Bearer " + mockJwtToken)
            .header("X-User-Context", createUserContextHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidBookRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.fields").isArray());

    // Test authorization error
    mockMvc.perform(delete("/api/v1/bookstore/books/1")
            .header("Authorization", "Bearer " + mockJwtToken)
            .header("X-User-Context", createRegularUserContextHeader()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("AUTH_INSUFFICIENT_PERMISSIONS"));
  }

  @Test
  void shouldVerifyRateLimitingAndCircuitBreakerIntegration() throws Exception {
    // Test rate limiting headers (simulating Gateway Service behavior)
    var response = mockMvc.perform(get("/api/v1/bookstore/books"))
        .andExpect(status().isOk())
        .andReturn();

    // Verify rate limiting headers are present (would be added by Gateway Service)
    // In a real integration, these would be set by the Gateway Service
    assertThat(response.getResponse().getHeaderNames())
        .contains("X-RateLimit-Remaining", "X-RateLimit-Limit");

    // Test circuit breaker behavior simulation
    // Perform multiple requests to test resilience
    for (int i = 0; i < 5; i++) {
      mockMvc.perform(get("/api/v1/bookstore/books"))
          .andExpect(status().isOk());
    }
  }

  @Test
  void shouldVerifySecurityHeadersAndCorsConfiguration() throws Exception {
    // Test CORS configuration for React 19 frontend
    mockMvc.perform(options("/api/v1/bookstore/books")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
        .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

    // Test security headers
    mockMvc.perform(get("/api/v1/bookstore/books"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Content-Type-Options"))
        .andExpect(header().exists("X-Frame-Options"))
        .andExpect(header().exists("X-XSS-Protection"));
  }

  // Helper methods
  private String createMockJwtToken() {
    // In a real scenario, this would be a valid JWT token from Auth Service
    // For testing purposes, we create a mock token
    return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwibmFtZSI6ImFkbWluIiwiaWF0IjoxNTE2MjM5MDIyfQ.mock-signature";
  }

  private String createUserContextHeader() {
    return """
        {
            "userId": 1,
            "username": "admin",
            "email": "admin@bookstore.com",
            "roles": ["ADMIN"],
            "permissions": ["BOOK_CREATE", "BOOK_UPDATE", "BOOK_DELETE", "INVENTORY_UPDATE"],
            "department": "Management",
            "organizationId": "BOOKSTORE_ORG",
            "customClaims": {}
        }
        """;
  }

  private String createRegularUserContextHeader() {
    return """
        {
            "userId": 2,
            "username": "user",
            "email": "user@bookstore.com",
            "roles": ["USER"],
            "permissions": ["BOOK_READ"],
            "department": "Customer",
            "organizationId": "BOOKSTORE_ORG",
            "customClaims": {}
        }
        """;
  }

  private String createBookRequestJson() throws Exception {
    var request = new CreateBookRequest(
        "Platform Integration Test Book",
        "Test Author",
        "978-0-123-45678-9",
        "A book for testing platform integration",
        new BigDecimal("29.99"),
        testCategory.getId(),
        50
    );
    return objectMapper.writeValueAsString(request);
  }

  private Long extractBookIdFromResponse(String responseContent) throws Exception {
    var responseMap = objectMapper.readValue(responseContent, Map.class);
    return Long.valueOf(responseMap.get("id").toString());
  }
}