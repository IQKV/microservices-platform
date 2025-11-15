package org.gripday.gatewayservice.presentation.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gripday.gatewayservice.config.GripdayProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private final GripdayProperties properties;

  public ApiDocumentationResource(GripdayProperties properties) {
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
                      }
                    ],
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
