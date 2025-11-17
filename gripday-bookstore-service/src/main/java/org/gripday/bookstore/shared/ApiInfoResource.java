package org.gripday.bookstore.shared.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookstore")
@Tag(name = "API Information", description = "API version and capability information")
public class ApiInfoResource {

  @Operation(
      summary = "Get API version information",
      description = "Retrieve information about supported API versions, capabilities, and deprecation notices"
  )
  @ApiResponse(
      responseCode = "200",
      description = "API version information retrieved successfully",
      content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
              name = "API version info",
              value = """
                  {
                    "currentVersion": "1",
                    "supportedVersions": ["1"],
                    "deprecatedVersions": [],
                    "capabilities": {
                      "bookManagement": true,
                      "inventoryManagement": true,
                      "searchAndFilter": true,
                      "bulkOperations": true
                    },
                    "versioningStrategies": [
                      "URL Path (/api/v1/bookstore/...)",
                      "Header (API-Version: 1)",
                      "Content Negotiation (Accept: application/vnd.gripday.bookstore.v1+json)",
                      "Query Parameter (?version=1)"
                    ],
                    "timestamp": "2024-01-15T10:30:00Z"
                  }
                  """
          )
      )
  )
  @GetMapping("/version")
  public ResponseEntity<Map<String, Object>> getVersionInfo() {
    var versionInfo = Map.of(
        "currentVersion", "1",
        "supportedVersions", List.of("1"),
        "deprecatedVersions", List.<String>of(),
        "capabilities", Map.of(
            "bookManagement", true,
            "inventoryManagement", true,
            "searchAndFilter", true,
            "bulkOperations", true,
            "adminOperations", true,
            "publicCatalogAccess", true
        ),
        "versioningStrategies", List.of(
            "URL Path (/api/v1/bookstore/...)",
            "Header (API-Version: 1)",
            "Content Negotiation (Accept: application/vnd.gripday.bookstore.v1+json)",
            "Query Parameter (?version=1)"
        ),
        "timestamp", Instant.now()
    );

    return ResponseEntity.ok(versionInfo);
  }
}