package com.iqscaffold.contactservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI contactServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("IQ Scaffold Contact Service API")
            .description("CRM contact management service for IQ Scaffold platform")
            .version("1.0.0")
            .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
        )
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes("Bearer Authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )
        );
  }
}