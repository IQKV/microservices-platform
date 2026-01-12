package com.iqscaffold.billingservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";
    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
        )
        .info(new Info()
            .title("Billing Service API")
            .version("0.0.1")
            .description("API for Billing and Payment operations"))
        .addTagsItem(new Tag()
            .name("Subscriptions")
            .description("Tenant subscription lifecycle management. " +
                "Manage subscription creation, updates, cancellations, and state transitions. " +
                "Lifecycle states: ACTIVE → PAUSED → ACTIVE, ACTIVE → CANCELED → EXPIRED. " +
                "Supports immediate and end-of-period cancellations."))
        .addTagsItem(new Tag()
            .name("Subscription Plans")
            .description("Platform-wide subscription plan management (admin). " +
                "Create and manage subscription plans with pricing tiers, features, and billing intervals. " +
                "Plans are synchronized with Stripe for payment processing."))
        .addTagsItem(new Tag()
            .name("Invoices")
            .description("Subscription invoice management. " +
                "View and manage invoices generated for subscription billing cycles. " +
                "Track payment status, due dates, and access invoice documents."));
  }
}
