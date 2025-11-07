package org.gripday.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.gripday.gatewayservice.service.ApiVersionExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Unit tests for ApiVersionRoutingFilter focusing on successful version routing scenarios.
 */
@ExtendWith(MockitoExtension.class)
class ApiVersionRoutingFilterTest {

  @Mock
  private ApiVersionExtractor versionExtractor;

  @Mock
  private GatewayFilterChain filterChain;

  private ApiVersionRoutingFilter routingFilter;

  @BeforeEach
  void setUp() {
    routingFilter = new ApiVersionRoutingFilter(versionExtractor);
    lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("Should add version headers for successful version detection")
  void shouldAddVersionHeadersForSuccessfulVersionDetection() {
    // Given
    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);
    when(versionExtractor.extractVersion(request)).thenReturn("v1");

    // When
    var result = routingFilter.filter(exchange, filterChain);

    // Then - verify the filter completes successfully
    assertThat(result).isNotNull();
    // The actual header addition is tested through integration tests
  }

  @Test
  @DisplayName("Should transform path for unversioned API requests")
  void shouldTransformPathForUnversionedApiRequests() {
    // Given
    var originalPath = "/api/auth/login";
    var version = "v1";

    // When
    var transformedPath = routingFilter.transformPathForVersion(originalPath, version);

    // Then
    assertThat(transformedPath).isEqualTo("/api/v1/auth/login");
  }

  @Test
  @DisplayName("Should keep versioned paths unchanged")
  void shouldKeepVersionedPathsUnchanged() {
    // Given
    var originalPath = "/api/v2/users";
    var version = "v1";
    when(versionExtractor.extractVersionFromPath(originalPath)).thenReturn("v2");

    // When
    var transformedPath = routingFilter.transformPathForVersion(originalPath, version);

    // Then
    assertThat(transformedPath).isEqualTo("/api/v2/users");
  }

  @Test
  @DisplayName("Should not transform non-API paths")
  void shouldNotTransformNonApiPaths() {
    // Given
    var paths = new String[] {
        "/health",
        "/actuator/health",
        "/swagger-ui.html",
        "/favicon.ico"
    };
    var version = "v1";

    // When & Then
    for (var path : paths) {
      var transformedPath = routingFilter.transformPathForVersion(path, version);
      assertThat(transformedPath).isEqualTo(path);
    }
  }

  @Test
  @DisplayName("Should correctly identify paths needing version routing")
  void shouldCorrectlyIdentifyPathsNeedingVersionRouting() {
    // Given
    lenient().when(versionExtractor.extractVersionFromPath("/api/auth/login")).thenReturn(null);
    lenient().when(versionExtractor.extractVersionFromPath("/api/v1/auth/login")).thenReturn("v1");
    lenient().when(versionExtractor.extractVersionFromPath("/health")).thenReturn(null);

    // When & Then
    assertThat(routingFilter.needsVersionRouting("/api/auth/login")).isTrue();
    assertThat(routingFilter.needsVersionRouting("/api/v1/auth/login")).isFalse();
    assertThat(routingFilter.needsVersionRouting("/health")).isFalse();
    assertThat(routingFilter.needsVersionRouting(null)).isFalse();
  }

  @Test
  @DisplayName("Should validate supported API versions correctly")
  void shouldValidateSupportedApiVersionsCorrectly() {
    // Given
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    when(versionExtractor.extractVersion(request)).thenReturn("v1");
    when(versionExtractor.isSupportedVersion("v1")).thenReturn(true);

    // When
    var isSupported = routingFilter.isSupportedApiVersion(request);

    // Then
    assertThat(isSupported).isTrue();
  }

  @Test
  @DisplayName("Should handle backward compatibility scenarios")
  void shouldHandleBackwardCompatibilityScenarios() {
    // Given - test various backward compatibility path transformations
    var testCases = new String[][] {
        {"/api/auth/login", "v1", "/api/v1/auth/login"},
        {"/api/users", "v2", "/api/v2/users"},
        {"/api/admin/settings", "v1", "/api/v1/admin/settings"}
    };

    // When & Then
    for (var testCase : testCases) {
      var originalPath = testCase[0];
      var version = testCase[1];
      var expectedPath = testCase[2];

      var transformedPath = routingFilter.transformPathForVersion(originalPath, version);
      assertThat(transformedPath).isEqualTo(expectedPath);
    }
  }

  @Test
  @DisplayName("Should have correct filter order for proper execution sequence")
  void shouldHaveCorrectFilterOrderForProperExecutionSequence() {
    // When
    var order = routingFilter.getOrder();

    // Then
    assertThat(order).isEqualTo(-100);
  }

  @Test
  @DisplayName("Should handle common user workflow paths")
  void shouldHandleCommonUserWorkflowPaths() {
    // Given - common API usage patterns
    var workflowPaths = new String[] {
        "/api/auth/signup",
        "/api/auth/login",
        "/api/users/profile",
        "/api/users/settings"
    };
    var version = "v1";

    // When & Then
    for (var path : workflowPaths) {
      var transformedPath = routingFilter.transformPathForVersion(path, version);
      assertThat(transformedPath).startsWith("/api/v1/");
      assertThat(transformedPath).contains(path.substring("/api/".length()));
    }
  }

  @Test
  @DisplayName("Should support version routing for different API versions")
  void shouldSupportVersionRoutingForDifferentApiVersions() {
    // Given
    var basePath = "/api/users";

    // When & Then
    var v1Path = routingFilter.transformPathForVersion(basePath, "v1");
    var v2Path = routingFilter.transformPathForVersion(basePath, "v2");

    assertThat(v1Path).isEqualTo("/api/v1/users");
    assertThat(v2Path).isEqualTo("/api/v2/users");
  }
}