package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.gripday.bookstore.shared.web.ApiInfoResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiInfoResource.class)
@Import(TestSecurityConfig.class)
@DisplayName("ApiInfoResource Tests")
class ApiInfoResourceTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Should return API version information")
  void shouldReturnApiVersionInformation() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/bookstore/version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentVersion").value("1"))
        .andExpect(jsonPath("$.supportedVersions").isArray())
        .andExpect(jsonPath("$.supportedVersions[0]").value("1"))
        .andExpect(jsonPath("$.deprecatedVersions").isArray())
        .andExpect(jsonPath("$.capabilities").exists())
        .andExpect(jsonPath("$.capabilities.bookManagement").value(true))
        .andExpect(jsonPath("$.capabilities.inventoryManagement").value(true))
        .andExpect(jsonPath("$.capabilities.searchAndFilter").value(true))
        .andExpect(jsonPath("$.capabilities.bulkOperations").value(true))
        .andExpect(jsonPath("$.capabilities.adminOperations").value(true))
        .andExpect(jsonPath("$.capabilities.publicCatalogAccess").value(true))
        .andExpect(jsonPath("$.versioningStrategies").isArray())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("Should return all versioning strategies")
  void shouldReturnAllVersioningStrategies() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/bookstore/version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versioningStrategies").isArray())
        .andExpect(jsonPath("$.versioningStrategies.length()").value(4))
        .andExpect(jsonPath("$.versioningStrategies[0]").value("URL Path (/api/v1/bookstore/...)"))
        .andExpect(jsonPath("$.versioningStrategies[1]").value("Header (API-Version: 1)"))
        .andExpect(jsonPath("$.versioningStrategies[2]").value("Content Negotiation (Accept: application/vnd.gripday.bookstore.v1+json)"))
        .andExpect(jsonPath("$.versioningStrategies[3]").value("Query Parameter (?version=1)"));
  }

  @Test
  @DisplayName("Should return empty deprecated versions list")
  void shouldReturnEmptyDeprecatedVersionsList() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/bookstore/version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deprecatedVersions").isArray())
        .andExpect(jsonPath("$.deprecatedVersions").isEmpty());
  }

  @Test
  @DisplayName("Should return timestamp in response")
  void shouldReturnTimestampInResponse() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/v1/bookstore/version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  @DisplayName("Should be accessible without authentication")
  void shouldBeAccessibleWithoutAuthentication() throws Exception {
    // Act & Assert - No authentication headers needed
    mockMvc.perform(get("/api/v1/bookstore/version"))
        .andExpect(status().isOk());
  }
}
