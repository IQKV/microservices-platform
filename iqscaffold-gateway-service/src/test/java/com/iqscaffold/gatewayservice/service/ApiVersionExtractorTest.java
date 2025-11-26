package com.iqscaffold.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

/**
 * Unit tests for ApiVersionExtractor focusing on successful version detection scenarios.
 */
class ApiVersionExtractorTest {

  private ApiVersionExtractor versionExtractor;

  @BeforeEach
  void setUp() {
    versionExtractor = new ApiVersionExtractor();
  }

  @Test
  @DisplayName("Should extract version from URL path with valid v1 format")
  void shouldExtractVersionFromUrlPathV1() {
    // Given
    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();

    // When
    var version = versionExtractor.extractVersion(request);

    // Then
    assertThat(version).isEqualTo("v1");
  }

  @Test
  @DisplayName("Should extract version from URL path with valid v2 format")
  void shouldExtractVersionFromUrlPathV2() {
    // Given
    var request = MockServerHttpRequest.get("/api/v2/users").build();

    // When
    var version = versionExtractor.extractVersion(request);

    // Then
    assertThat(version).isEqualTo("v2");
  }

  @Test
  @DisplayName("Should extract version from API-Version header with semantic version format")
  void shouldExtractVersionFromApiVersionHeader() {
    // Given
    var request = MockServerHttpRequest.get("/api/auth/login")
        .header("API-Version", "2.0")
        .build();

    // When
    var version = versionExtractor.extractVersion(request);

    // Then
    assertThat(version).isEqualTo("v2");
  }

  @Test
  @DisplayName("Should extract version from API-Version header with v-prefix format")
  void shouldExtractVersionFromApiVersionHeaderWithVPrefix() {
    // Given
    var request = MockServerHttpRequest.get("/api/auth/login")
        .header("API-Version", "v1")
        .build();

    // When
    var version = versionExtractor.extractVersion(request);

    // Then
    assertThat(version).isEqualTo("v1");
  }

  @Test
  @DisplayName("Should extract version from Accept header with vendor media type")
  void shouldExtractVersionFromAcceptHeader() {
    // Given
    var request = MockServerHttpRequest.get("/api/users")
        .header(HttpHeaders.ACCEPT, "application/vnd.iqscaffold.v2+json")
        .build();

    // When
    var version = versionExtractor.extractVersion(request);

    // Then
    assertThat(version).isEqualTo("v2");
  }

  @Test
  @DisplayName("Should return default version when no version specified")
  void shouldReturnDefaultVersionWhenNoVersionSpecified() {
    // Given
    var request = MockServerHttpRequest.get("/api/users").build();

    // When
    var version = versionExtractor.extractVersion(request);

    // Then
    assertThat(version).isEqualTo("v1");
  }

  @Test
  @DisplayName("Should prioritize URL path version over header version")
  void shouldPrioritizeUrlPathVersionOverHeader() {
    // Given
    var request = MockServerHttpRequest.get("/api/v2/users")
        .header("API-Version", "1.0")
        .build();

    // When
    var version = versionExtractor.extractVersion(request);

    // Then
    assertThat(version).isEqualTo("v2");
  }

  @Test
  @DisplayName("Should extract version from path correctly")
  void shouldExtractVersionFromPath() {
    // When & Then
    assertThat(versionExtractor.extractVersionFromPath("/api/v1/auth/login")).isEqualTo("v1");
    assertThat(versionExtractor.extractVersionFromPath("/api/v2/users")).isEqualTo("v2");
    assertThat(versionExtractor.extractVersionFromPath("/api/auth/login")).isNull();
    assertThat(versionExtractor.extractVersionFromPath("/health")).isNull();
  }

  @Test
  @DisplayName("Should validate supported versions correctly")
  void shouldValidateSupportedVersions() {
    // When & Then
    assertThat(versionExtractor.isSupportedVersion("v1")).isTrue();
    assertThat(versionExtractor.isSupportedVersion("v2")).isTrue();
    assertThat(versionExtractor.isSupportedVersion("v3")).isFalse();
    assertThat(versionExtractor.isSupportedVersion("")).isFalse();
    assertThat(versionExtractor.isSupportedVersion(null)).isFalse();
  }

  @Test
  @DisplayName("Should return correct default version")
  void shouldReturnCorrectDefaultVersion() {
    // When
    var defaultVersion = versionExtractor.getDefaultVersion();

    // Then
    assertThat(defaultVersion).isEqualTo("v1");
  }

  @Test
  @DisplayName("Should handle complex API paths with version extraction")
  void shouldHandleComplexApiPathsWithVersionExtraction() {
    // Given
    var paths = new String[] {
        "/api/v1/auth/signup",
        "/api/v2/users/123/profile",
        "/api/v1/admin/users",
        "/api/v2/tenant/settings"
    };

    // When & Then
    assertThat(versionExtractor.extractVersionFromPath(paths[0])).isEqualTo("v1");
    assertThat(versionExtractor.extractVersionFromPath(paths[1])).isEqualTo("v2");
    assertThat(versionExtractor.extractVersionFromPath(paths[2])).isEqualTo("v1");
    assertThat(versionExtractor.extractVersionFromPath(paths[3])).isEqualTo("v2");
  }
}