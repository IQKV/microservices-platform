package org.gripday.authservice.config;

import java.util.Map;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for API documentation. Provides interactive Swagger UI with security schemes and examples.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Gripday User Service API",
        version = "1.0.0",
        description = """
            Centralized authentication and user management service for the Gripday microservices platform.
            
            ## Features
            - JWT-based authentication with refresh tokens
            - User registration and management
            - Role-based access control (RBAC)
            - Multi-tenant architecture support
            - Rate limiting and security measures
            - Comprehensive audit logging
            
            ## Authentication
            Most endpoints require JWT authentication. Include the Bearer token in the Authorization header:
            ```
            Authorization: Bearer <your-jwt-token>
            ```
            
            ## Error Handling
            All errors follow a consistent format with correlation IDs for tracing.
            """,
        contact = @Contact(
            name = "Gripday Platform Team",
            email = "api-support@gripday.com",
            url = "https://docs.gripday.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "https://api.gripday.com",
            description = "Production Server"
        ),
        @Server(
            url = "https://api.gripday.website",
            description = "Staging Server"
        ),
        @Server(
            url = "http://localhost:8080",
            description = "Local Development Server"
        )
    },
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecuritySchemes({
    @SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = """
            JWT Bearer token authentication. 
            
            Obtain a token by calling the `/api/v1/auth/login` endpoint with valid credentials.
            The token should be included in the Authorization header for protected endpoints.
            
            Example: `Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
            """
    ),
    @SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "API Key authentication for service-to-service communication"
    )
})
public class OpenApiConfig {

  @Bean
  public GroupedOpenApi authenticationApi() {
    return GroupedOpenApi.builder()
        .group("authentication")
        .displayName("Authentication APIs")
        .pathsToMatch("/api/*/auth/**")
        .addOpenApiCustomizer(authenticationCustomizer())
        .build();
  }

  @Bean
  public GroupedOpenApi userManagementApi() {
    return GroupedOpenApi.builder()
        .group("user-management")
        .displayName("User Management APIs")
        .pathsToMatch("/api/*/users/**")
        .addOpenApiCustomizer(userManagementCustomizer())
        .build();
  }

  @Bean
  public GroupedOpenApi tenantManagementApi() {
    return GroupedOpenApi.builder()
        .group("tenant-management")
        .displayName("Tenant Management APIs")
        .pathsToMatch("/api/*/tenants/**")
        .addOpenApiCustomizer(tenantManagementCustomizer())
        .build();
  }

  @Bean
  public GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
        .group("public")
        .displayName("Public APIs")
        .pathsToMatch("/api/**")
        .pathsToExclude("/api/*/internal/**")
        .addOpenApiCustomizer(publicApiCustomizer())
        .build();
  }

  @Bean
  public GroupedOpenApi internalApi() {
    return GroupedOpenApi.builder()
        .group("internal")
        .displayName("Internal APIs")
        .pathsToMatch("/api/*/internal/**")
        .addOpenApiCustomizer(internalApiCustomizer())
        .build();
  }

  private OpenApiCustomizer authenticationCustomizer() {
    return openApi -> {
      openApi.info(openApi.getInfo()
          .title("Authentication Service API")
          .description("User authentication, registration, and token management endpoints"));

      addCommonResponses(openApi);
      addAuthenticationExamples(openApi);
    };
  }

  private OpenApiCustomizer userManagementCustomizer() {
    return openApi -> {
      openApi.info(openApi.getInfo()
          .title("User Management API")
          .description("Admin-only user management operations with role-based access control"));

      addCommonResponses(openApi);
      addUserManagementExamples(openApi);
    };
  }

  private OpenApiCustomizer tenantManagementCustomizer() {
    return openApi -> {
      openApi.info(openApi.getInfo()
          .title("Tenant Management API")
          .description("Multi-tenant architecture management and configuration"));

      addCommonResponses(openApi);
    };
  }

  private OpenApiCustomizer publicApiCustomizer() {
    return openApi -> {
      openApi.info(openApi.getInfo()
          .title("Gripday User Service - Public APIs")
          .description("All public-facing APIs for authentication and user management"));

      addCommonResponses(openApi);
      addAllExamples(openApi);
    };
  }

  private OpenApiCustomizer internalApiCustomizer() {
    return openApi -> {
      openApi.info(openApi.getInfo()
          .title("Internal Service APIs")
          .description("Internal service-to-service communication endpoints"));

      addCommonResponses(openApi);
    };
  }

  private void addCommonResponses(OpenAPI openApi) {
    var components = openApi.getComponents();
    if (components == null) {
      components = new Components();
      openApi.setComponents(components);
    }

    // Add common error responses
    components.addResponses("BadRequest", createErrorResponse(
        "400", "Bad Request", "Invalid request data or validation errors"));
    components.addResponses("Unauthorized", createErrorResponse(
        "401", "Unauthorized", "Authentication required or invalid credentials"));
    components.addResponses("Forbidden", createErrorResponse(
        "403", "Forbidden", "Insufficient permissions for this operation"));
    components.addResponses("NotFound", createErrorResponse(
        "404", "Not Found", "Requested resource not found"));
    components.addResponses("Conflict", createErrorResponse(
        "409", "Conflict", "Resource conflict (e.g., username already exists)"));
    components.addResponses("Locked", createErrorResponse(
        "423", "Locked", "Account temporarily locked due to security measures"));
    components.addResponses("TooManyRequests", createErrorResponse(
        "429", "Too Many Requests", "Rate limit exceeded"));
    components.addResponses("InternalServerError", createErrorResponse(
        "500", "Internal Server Error", "Unexpected server error"));
  }

  private ApiResponse createErrorResponse(String code, String description, String exampleMessage) {
    var errorMap = new java.util.HashMap<String, Object>();
    errorMap.put("type", "https://problems.gripday.com/example");
    errorMap.put("title", description);
    errorMap.put("status", Integer.valueOf(code));
    errorMap.put("detail", exampleMessage);
    errorMap.put("instance", "/api/v1/endpoint");
    errorMap.put("code", "ERROR_CODE");
    errorMap.put("path", "/api/v1/endpoint");
    errorMap.put("method", "POST");
    errorMap.put("correlationId", "abc123-def456-ghi789");
    errorMap.put("requestId", "req-001-2024");
    errorMap.put("fields", java.util.List.of());

    var errorExample = new Example()
        .summary("Error Response")
        .value(errorMap);

    var mediaType = new MediaType()
        .addExamples("error", errorExample);

    return new ApiResponse()
        .description(description)
        .content(new Content().addMediaType("application/problem+json", mediaType));
  }

  private void addAuthenticationExamples(OpenAPI openApi) {
    var components = openApi.getComponents();

    // Login success example
    var loginSuccessExample = new Example()
        .summary("Successful Login")
        .description("Example of successful user authentication")
        .value(Map.of(
            "accessToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "refreshToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "tokenType", "Bearer",
            "expiresIn", 900,
            "user", Map.of(
                "userId", 1,
                "username", "john.doe",
                "email", "john.doe@example.com",
                "roles", java.util.List.of("USER"),
                "permissions", java.util.List.of("READ_PROFILE"),
                "department", "Engineering",
                "organizationId", "tenant-123",
                "customClaims", Map.of()
            )
        ));

    components.addExamples("LoginSuccess", loginSuccessExample);

    // Registration success example
    var registrationSuccessExample = new Example()
        .summary("Successful Registration")
        .description("Example of successful user registration")
        .value(Map.of(
            "userId", 1,
            "username", "john.doe",
            "email", "john.doe@example.com",
            "firstName", "John",
            "lastName", "Doe",
            "emailVerified", false,
            "createdAt", "2024-01-15T10:30:00Z",
            "message", "User registered successfully. Please verify your email."
        ));

    components.addExamples("RegistrationSuccess", registrationSuccessExample);
  }

  private void addUserManagementExamples(OpenAPI openApi) {
    var components = openApi.getComponents();

    // User list example
    var userListExample = new Example()
        .summary("User List")
        .description("Paginated list of users")
        .value(Map.of(
            "content", java.util.List.of(
                Map.of(
                    "id", 1,
                    "username", "john.doe",
                    "email", "john.doe@example.com",
                    "firstName", "John",
                    "lastName", "Doe",
                    "enabled", true,
                    "roles", java.util.List.of("USER"),
                    "createdAt", "2024-01-15T10:30:00Z"
                )
            ),
            "pageable", Map.of(
                "pageNumber", 0,
                "pageSize", 20,
                "sort", Map.of("sorted", false)
            ),
            "totalElements", 1,
            "totalPages", 1,
            "first", true,
            "last", true
        ));

    components.addExamples("UserList", userListExample);
  }

  private void addAllExamples(OpenAPI openApi) {
    addAuthenticationExamples(openApi);
    addUserManagementExamples(openApi);
  }
}