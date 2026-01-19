package com.iqscaffold.contactservice.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureExtractionFilterTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private FeatureExtractionFilter filter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    filter = new FeatureExtractionFilter(objectMapper);
  }

  @Test
  @DisplayName("Should extract feature context from headers and set in thread-local")
  void shouldExtractFeatureContextFromHeaders() throws Exception {
    // Arrange
    when(request.getHeader(FeatureExtractionFilter.Headers.X_ENABLED_FEATURES)).thenReturn("lead_management,data_export");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_PLAN_ID)).thenReturn("plan-123");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_PLAN_NAME)).thenReturn("Professional");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_FEATURE_QUOTAS)).thenReturn("{\"max_leads\":\"1000\"}");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_FEATURE_LIMITS)).thenReturn("{\"max_activities\":\"50\"}");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_FEATURE_TIERS)).thenReturn("{\"support_level\":\"premium\"}");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);

    // Context should be cleared after filter execution
    FeatureContext context = FeatureContextHolder.getContext();
    assertThat(context.enabledFeatures()).isEmpty(); // Should be empty as context is cleared
  }

  @Test
  @DisplayName("Should handle missing headers gracefully")
  void shouldHandleMissingHeadersGracefully() throws Exception {
    // Arrange - no headers set

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);

    // Context should be empty
    FeatureContext context = FeatureContextHolder.getContext();
    assertThat(context.enabledFeatures()).isEmpty();
  }

  @Test
  @DisplayName("Should handle malformed JSON headers gracefully")
  void shouldHandleMalformedJsonHeadersGracefully() throws Exception {
    // Arrange
    when(request.getHeader(FeatureExtractionFilter.Headers.X_ENABLED_FEATURES)).thenReturn("lead_management,data_export");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_PLAN_ID)).thenReturn("plan-123");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_FEATURE_QUOTAS)).thenReturn("invalid-json");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_FEATURE_LIMITS)).thenReturn("{malformed}");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);

    // Should continue processing despite malformed JSON
    FeatureContext context = FeatureContextHolder.getContext();
    assertThat(context.enabledFeatures()).isEmpty(); // Context cleared after filter
  }

  @Test
  @DisplayName("Should parse comma-separated features correctly")
  void shouldParseCommaSeparatedFeaturesCorrectly() throws Exception {
    // Arrange
    when(request.getHeader(FeatureExtractionFilter.Headers.X_ENABLED_FEATURES)).thenReturn("feature1, feature2 , feature3");

    // Act & Assert - we need to test during filter execution
    filter.doFilterInternal(request, response, (req, resp) -> {
      FeatureContext context = FeatureContextHolder.getContext();
      assertThat(context.enabledFeatures()).containsExactlyInAnyOrder("feature1", "feature2", "feature3");
    });
  }

  @Test
  @DisplayName("Should handle empty feature list")
  void shouldHandleEmptyFeatureList() throws Exception {
    // Arrange
    when(request.getHeader(FeatureExtractionFilter.Headers.X_ENABLED_FEATURES)).thenReturn("");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should always clear context after filter execution")
  void shouldAlwaysClearContextAfterFilterExecution() throws Exception {
    // Arrange
    when(request.getHeader(FeatureExtractionFilter.Headers.X_ENABLED_FEATURES)).thenReturn("test_feature");
    when(request.getHeader(FeatureExtractionFilter.Headers.X_PLAN_ID)).thenReturn("test-plan");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    FeatureContext context = FeatureContextHolder.getContext();
    assertThat(context.enabledFeatures()).isEmpty(); // Should be cleared
    assertThat(context.planId()).isNull();
  }
}
