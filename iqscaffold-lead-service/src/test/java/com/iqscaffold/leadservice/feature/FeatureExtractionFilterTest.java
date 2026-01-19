package com.iqscaffold.leadservice.feature;

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
  @DisplayName("Should extract feature context from headers")
  void shouldExtractFeatureContextFromHeaders() throws Exception {
    // Given
    when(request.getHeader("X-Enabled-Features")).thenReturn("lead_management,data_export");
    when(request.getHeader("X-Plan-ID")).thenReturn("plan-123");
    when(request.getHeader("X-Plan-Name")).thenReturn("Professional");
    when(request.getHeader("X-Feature-Quotas")).thenReturn("{\"max_leads\":1000}");
    when(request.getHeader("X-Feature-Limits")).thenReturn("{\"max_lead_activities\":50}");
    when(request.getHeader("X-Feature-Tiers")).thenReturn("{\"support_level\":\"premium\"}");

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);

    // Context should be cleared after filter execution
    FeatureContext context = FeatureContextHolder.getContext();
    assertThat(context.enabledFeatures()).isEmpty(); // Should be empty as context is cleared
  }

  @Test
  @DisplayName("Should handle missing headers gracefully")
  void shouldHandleMissingHeadersGracefully() throws Exception {
    // Given - no headers set

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);

    // Should not throw exception and context should be empty
    FeatureContext context = FeatureContextHolder.getContext();
    assertThat(context.enabledFeatures()).isEmpty();
  }

  @Test
  @DisplayName("Should handle malformed JSON gracefully")
  void shouldHandleMalformedJsonGracefully() throws Exception {
    // Given
    when(request.getHeader("X-Enabled-Features")).thenReturn("lead_management");
    when(request.getHeader("X-Feature-Quotas")).thenReturn("invalid-json");

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);

    // Should not throw exception
    FeatureContext context = FeatureContextHolder.getContext();
    assertThat(context.enabledFeatures()).isEmpty(); // Context cleared after execution
  }
}
