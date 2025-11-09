package org.gripday.bookstore.infrastructure.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${spring.application.name:bookstore-service}")
  private String applicationName;

  @Value("${server.port:8082}")
  private String serverPort;

  @Bean
  public OpenAPI bookstoreOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Bookstore Service API")
            .description("""
                Book catalog and inventory management microservice for the Gripday platform.
                
                This service provides book management capabilities including:
                - Book catalog browsing and search functionality
                - Inventory management and stock tracking
                - Administrative operations for book and inventory management
                
                **Authentication**: All endpoints require JWT authentication except for public catalog browsing.
                **Authorization**: Administrative operations require ADMIN or SUPERADMIN roles.
                
                **Integration**: This service is accessed through the Gateway Service (BFF pattern) 
                at `/api/v1/bookstore/*` endpoints. Direct access is not permitted in production.
                
                **API Versioning**: This API supports multiple versioning strategies:
                - URL Path: `/api/v1/bookstore/books` or `/api/v2/bookstore/books`
                - Header-based: `API-Version: 1` or `API-Version: 2`
                - Content negotiation: `Accept: application/vnd.gripday.bookstore.v1+json`
                - Query parameter: `?version=1` or `?version=2`
                
                Current version: v1 (default)
                Supported versions: v1
                """)
            .version("v1.0")
            .contact(new Contact()
                .name("Gripday Development Team")
                .email("dev@gripday.com")
                .url("https://gripday.com"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")))
        .servers(List.of(
            new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server"),
            new Server()
                .url("https://api.gripday.com")
                .description("Production Gateway (BFF)"),
            new Server()
                .url("https://api.gripday.website")
                .description("Staging Gateway (BFF)")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token obtained from Auth Service via Gateway Service")));
  }
}