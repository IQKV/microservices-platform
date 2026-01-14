package com.iqscaffold.contactservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for Contact Service API documentation.
 * Provides interactive Swagger UI with security schemes and endpoint grouping.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI contactServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("IQ Scaffold Contact Service API")
            .version("1.0.0")
            .description("""
                CRM contact and company management service for IQ Scaffold platform.
                
                ## Features
                - Contact CRUD operations
                - Company management
                - Lead conversion tracking
                - Activity management
                - Multi-tenant data isolation
                
                ## Authentication
                All endpoints require JWT authentication. Include the Bearer token in the Authorization header:
                ```
                Authorization: Bearer <your-jwt-token>
                ```
                
                ## Error Handling
                All errors follow RFC 7807 Problem Details format with correlation IDs for tracing.
                """)
            .contact(new Contact()
                .name("IQ Scaffold Platform Team")
                .email("api-support@iqscaffold.com")
                .url("https://docs.iqscaffold.com"))
            .license(new License()
                .name("MIT")
                .url("https://opensource.org/licenses/MIT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("""
                        JWT Bearer token authentication.
                        
                        Obtain a token from the User Service authentication endpoint.
                        The token should be included in the Authorization header for all protected endpoints.
                        
                        Example: `Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
                        """))
            .addResponses("BadRequest", createErrorResponse(
                "400", "Bad Request", "Invalid request data or validation errors"))
            .addResponses("Unauthorized", createErrorResponse(
                "401", "Unauthorized", "Authentication required or invalid token"))
            .addResponses("Forbidden", createErrorResponse(
                "403", "Forbidden", "Insufficient permissions for this operation"))
            .addResponses("NotFound", createErrorResponse(
                "404", "Not Found", "Requested resource not found"))
            .addResponses("Conflict", createErrorResponse(
                "409", "Conflict", "Resource conflict (e.g., duplicate email)"))
            .addResponses("InternalServerError", createErrorResponse(
                "500", "Internal Server Error", "Unexpected server error")));
  }

  @Bean
  public GroupedOpenApi contactManagementApi() {
    return GroupedOpenApi.builder()
        .group("contact-management")
        .displayName("Contact Management")
        .pathsToMatch("/api/v1/contacts/**")
        .build();
  }

  @Bean
  public GroupedOpenApi companyManagementApi() {
    return GroupedOpenApi.builder()
        .group("company-management")
        .displayName("Company Management")
        .pathsToMatch("/api/v1/companies/**")
        .build();
  }

  @Bean
  public GroupedOpenApi allContactApis() {
    return GroupedOpenApi.builder()
        .group("all")
        .displayName("All Contact APIs")
        .pathsToMatch("/api/**")
        .build();
  }

  private ApiResponse createErrorResponse(String code, String description, String exampleMessage) {
    var errorExample = new io.swagger.v3.oas.models.examples.Example()
        .summary("Error Response")
        .value(java.util.Map.of(
            "type", "https://api.iqscaffold.com/errors/" + code.toLowerCase().replace(" ", "-"),
            "title", description,
            "status", Integer.valueOf(code),
            "detail", exampleMessage,
            "instance", "/api/v1/contacts",
            "timestamp", "2026-01-14T10:30:00Z",
            "correlationId", "abc-123-def-456"
        ));

    var mediaType = new MediaType()
        .addExamples("error", errorExample);

    return new ApiResponse()
        .description(description)
        .content(new Content().addMediaType("application/problem+json", mediaType));
  }
}
