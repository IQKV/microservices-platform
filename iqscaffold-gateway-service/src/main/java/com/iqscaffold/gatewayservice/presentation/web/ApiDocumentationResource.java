package com.iqscaffold.gatewayservice.presentation.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resource for API documentation discovery and navigation.
 * Dynamically generates documentation endpoints based on configured services.
 */
@RestController
@RequestMapping("/api/v1/docs")
@Tag(name = "API Documentation", description = "Endpoints for discovering available API documentation")
public class ApiDocumentationResource {

  @Value("${server.port:8080}")
  private int serverPort;

  private final IqScaffoldProperties properties;

  public ApiDocumentationResource(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Operation(
      summary = "Get available API documentation",
      description = "Returns a list of all available API documentation endpoints for the gateway and connected microservices"
  )
  @ApiResponse(
      responseCode = "200",
      description = "Successfully retrieved API documentation endpoints",
      content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
              value = """
                  {
                    "gateway": {
                      "swaggerUi": "http://localhost:8080/swagger-ui.html",
                      "apiDocs": "http://localhost:8080/api-docs"
                    },
                    "services": [
                      {
                        "name": "user-service",
                        "displayName": "User Service APIs",
                        "description": "Authentication and user management APIs",
                        "swaggerUi": "http://localhost:8080/user-service/swagger-ui.html",
                        "apiDocs": "http://localhost:8080/user-service/api-docs",
                        "directUri": "http://localhost:8080",
                        "enabled": true
                      },
                      {
                        "name": "billing-service",
                        "displayName": "Billing Service APIs",
                        "description": "Payments, subscriptions and invoicing",
                        "swaggerUi": "http://localhost:8080/billing-service/swagger-ui.html",
                        "apiDocs": "http://localhost:8080/billing-service/api-docs",
                        "directUri": "http://localhost:8082",
                        "enabled": true,
                        "specializedGroups": [
                          {
                            "name": "billing-payments",
                            "displayName": "💳 Payment APIs",
                            "description": "Payment processing, refunds, and payment intent management",
                            "swaggerUi": "http://localhost:8080/billing-service/swagger-ui.html?urls.primaryName=💳 Payment APIs",
                            "apiDocs": "http://localhost:8080/billing-service/api-docs/billing-payments"
                          },
                          {
                            "name": "billing-webhooks",
                            "displayName": "🔗 Webhook APIs", 
                            "description": "Stripe webhook handlers for payment events",
                            "swaggerUi": "http://localhost:8080/billing-service/swagger-ui.html?urls.primaryName=🔗 Webhook APIs",
                            "apiDocs": "http://localhost:8080/billing-service/api-docs/billing-webhooks"
                          },
                          {
                            "name": "billing-admin",
                            "displayName": "🛠️ Billing Admin APIs",
                            "description": "Merchant onboarding and administrative operations", 
                            "swaggerUi": "http://localhost:8080/billing-service/swagger-ui.html?urls.primaryName=🛠️ Billing Admin APIs",
                            "apiDocs": "http://localhost:8080/billing-service/api-docs/billing-admin"
                          }
                        ]
                      }
                    ],
                    "totalServices": 2,
                    "message": "Access Swagger UI through the gateway for aggregated API documentation"
                  }
                  """
          )
      )
  )
  @GetMapping
  public ResponseEntity<Map<String, Object>> getApiDocumentation() {
    var baseUrl = "http://localhost:" + serverPort;

    var gateway = Map.of(
        "swaggerUi", baseUrl + "/swagger-ui.html",
        "apiDocs", baseUrl + "/api-docs"
    );

    var services = new ArrayList<Map<String, Object>>();
    var configuredServices = properties.gateway().routing().services();

    if (configuredServices != null) {
      configuredServices.forEach((serviceName, serviceConfig) -> {
        if (!serviceConfig.enabled()) {
          return;
        }

        var openApiConfig = serviceConfig.openapi();
        if (openApiConfig == null || !openApiConfig.enabled()) {
          return;
        }

        var contextPath = openApiConfig.contextPath();
        if (contextPath.isBlank()) {
          contextPath = serviceName;
        }

        // Remove leading slash if present
        if (contextPath.startsWith("/")) {
          contextPath = contextPath.substring(1);
        }

        var serviceInfo = new HashMap<String, Object>();
        serviceInfo.put("name", serviceName);
        serviceInfo.put("displayName", openApiConfig.displayName());
        serviceInfo.put("description", openApiConfig.description());
        serviceInfo.put("swaggerUi", baseUrl + "/" + contextPath + "/swagger-ui.html");
        serviceInfo.put("apiDocs", baseUrl + "/" + contextPath + "/api-docs");
        serviceInfo.put("directUri", serviceConfig.uri());
        serviceInfo.put("enabled", true);

        // Add specialized groups for billing service
        if ("billing-service".equals(serviceName)) {
          serviceInfo.put("specializedGroups", java.util.List.of(
              java.util.Map.of(
                  "name", "billing-payments",
                  "displayName", "💳 Payment APIs",
                  "description", "Payment processing, refunds, and payment intent management",
                  "swaggerUi", baseUrl + "/" + contextPath + "/swagger-ui.html?urls.primaryName=💳 Payment APIs",
                  "apiDocs", baseUrl + "/" + contextPath + "/api-docs/billing-payments"
              ),
              java.util.Map.of(
                  "name", "billing-webhooks", 
                  "displayName", "🔗 Webhook APIs",
                  "description", "Stripe webhook handlers for payment events",
                  "swaggerUi", baseUrl + "/" + contextPath + "/swagger-ui.html?urls.primaryName=🔗 Webhook APIs",
                  "apiDocs", baseUrl + "/" + contextPath + "/api-docs/billing-webhooks"
              ),
              java.util.Map.of(
                  "name", "billing-admin",
                  "displayName", "🛠️ Billing Admin APIs", 
                  "description", "Merchant onboarding and administrative operations",
                  "swaggerUi", baseUrl + "/" + contextPath + "/swagger-ui.html?urls.primaryName=🛠️ Billing Admin APIs",
                  "apiDocs", baseUrl + "/" + contextPath + "/api-docs/billing-admin"
              )
          ));
        }

        services.add(serviceInfo);
      });
    }

    var response = Map.of(
        "gateway", gateway,
        "services", services,
        "totalServices", services.size(),
        "message", "Access Swagger UI through the gateway for aggregated API documentation"
    );

    return ResponseEntity.ok(response);
  }
}
