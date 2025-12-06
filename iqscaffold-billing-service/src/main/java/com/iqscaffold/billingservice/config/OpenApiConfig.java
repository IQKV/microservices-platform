package com.iqscaffold.billingservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the Billing Service.
 * 
 * <p>Configures API documentation with SpringDoc, including:
 * <ul>
 *   <li>API metadata (title, version, description)</li>
 *   <li>Security schemes (JWT Bearer authentication)</li>
 *   <li>Common error responses for reuse across endpoints</li>
 *   <li>Server configurations for different environments</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI billingServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("IQ Scaffold Billing Service API")
                .version("1.0.0")
                .description("""
                    Billing & Subscription Management Service API
                    
                    This service provides comprehensive subscription lifecycle management,
                    payment processing, invoicing, and usage-based billing capabilities.
                    
                    Key Features:
                    - Subscription management (create, upgrade, downgrade, cancel)
                    - Multi-provider payment processing (Stripe, PayPal, manual)
                    - Automated invoice generation and delivery
                    - Usage-based billing and quota enforcement
                    - Customer self-service portal
                    - Webhook integration for payment events
                    """)
                .contact(new Contact()
                    .name("IQ Scaffold Team")
                    .email("support@iqscaffold.com")
                    .url("https://iqscaffold.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8082")
                    .description("Local development"),
                new Server()
                    .url("https://api-staging.iqscaffold.com")
                    .description("Staging environment"),
                new Server()
                    .url("https://api.iqscaffold.com")
                    .description("Production environment")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT authentication token obtained from User Service /api/v1/auth/login endpoint"))
                .addResponses("BadRequest", new ApiResponse()
                    .description("Bad Request - Invalid input data")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("Unauthorized", new ApiResponse()
                    .description("Unauthorized - Missing or invalid authentication token")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("Forbidden", new ApiResponse()
                    .description("Forbidden - Insufficient permissions")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("NotFound", new ApiResponse()
                    .description("Not Found - Resource does not exist")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("Conflict", new ApiResponse()
                    .description("Conflict - Resource already exists or state conflict")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("TooManyRequests", new ApiResponse()
                    .description("Too Many Requests - Rate limit exceeded")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("InternalServerError", new ApiResponse()
                    .description("Internal Server Error - Unexpected server error")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))))));
    }
}
