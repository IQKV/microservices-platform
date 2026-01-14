package com.iqscaffold.pipelineservice.config;

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
 * OpenAPI configuration for Pipeline Service API documentation.
 * Provides interactive Swagger UI with security schemes and endpoint grouping.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI pipelineServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("IQ Scaffold Pipeline Service API")
            .version("1.0.0")
            .description("""
                CRM pipeline management service for lead tracking and follow-ups.
                
                ## Features
                - Pipeline stage management
                - Pipeline item tracking
                - Lead stage transitions
                - Follow-up scheduling and tracking
                - Dashboard statistics and metrics
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
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")
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
                "409", "Conflict", "Resource conflict (e.g., cannot delete stage with leads)"))
            .addResponses("InternalServerError", createErrorResponse(
                "500", "Internal Server Error", "Unexpected server error")));
  }

  @Bean
  public GroupedOpenApi pipelineManagementApi() {
    return GroupedOpenApi.builder()
        .group("pipeline-management")
        .displayName("Pipeline Management")
        .pathsToMatch("/api/v1/pipeline/**")
        .build();
  }

  @Bean
  public GroupedOpenApi followUpManagementApi() {
    return GroupedOpenApi.builder()
        .group("follow-up-management")
        .displayName("Follow-Up Management")
        .pathsToMatch("/api/v1/follow-ups/**")
        .build();
  }

  @Bean
  public GroupedOpenApi dashboardApi() {
    return GroupedOpenApi.builder()
        .group("dashboard")
        .displayName("Dashboard & Analytics")
        .pathsToMatch("/api/v1/dashboard/**")
        .build();
  }

  @Bean
  public GroupedOpenApi allPipelineApis() {
    return GroupedOpenApi.builder()
        .group("all")
        .displayName("All Pipeline APIs")
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
            "instance", "/api/v1/pipeline",
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
