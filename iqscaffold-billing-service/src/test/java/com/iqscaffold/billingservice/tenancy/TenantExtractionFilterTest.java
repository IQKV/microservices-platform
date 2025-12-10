package com.iqscaffold.billingservice.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantExtractionFilterTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @Mock
  private TenantExtractionService tenantExtractionService;

  private TenantExtractionFilter tenantExtractionFilter;

  @BeforeEach
  void setUp() {
    tenantExtractionFilter = new TenantExtractionFilter(tenantExtractionService);
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  @DisplayName("Should extract and set tenant context from request")
  void shouldExtractAndSetTenantContext() throws ServletException, IOException {
    // Arrange
    when(request.getRequestURI()).thenReturn("/api/v1/subscriptions");

    // Act
    tenantExtractionFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(tenantExtractionService).extractAndSetTenantContext(request);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should clear tenant context after request processing")
  void shouldClearTenantContextAfterRequest() throws ServletException, IOException {
    // Arrange
    when(request.getRequestURI()).thenReturn("/api/v1/subscriptions");

    // Act
    tenantExtractionFilter.doFilterInternal(request, response, filterChain);

    // Assert
    assertThat(TenantContext.getCurrentTenantId()).isNull();
  }

  @Test
  @DisplayName("Should continue filter chain when no tenant context")
  void shouldContinueFilterChainWhenNoTenantContext() throws ServletException, IOException {
    // Arrange
    when(request.getRequestURI()).thenReturn("/api/v1/subscriptions");
    when(request.getMethod()).thenReturn("GET");
    when(tenantExtractionService.extractAndSetTenantContext(request)).thenReturn(false);

    // Act
    tenantExtractionFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should skip filter for actuator endpoints")
  void shouldSkipFilterForActuatorEndpoints() throws ServletException {
    // Arrange
    when(request.getRequestURI()).thenReturn("/actuator/health");

    // Act
    var shouldNotFilter = tenantExtractionFilter.shouldNotFilter(request);

    // Assert
    assertThat(shouldNotFilter).isTrue();
  }

  @Test
  @DisplayName("Should skip filter for swagger-ui endpoints")
  void shouldSkipFilterForSwaggerEndpoints() throws ServletException {
    // Arrange
    when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

    // Act
    var shouldNotFilter = tenantExtractionFilter.shouldNotFilter(request);

    // Assert
    assertThat(shouldNotFilter).isTrue();
  }

  @Test
  @DisplayName("Should skip filter for api-docs endpoints")
  void shouldSkipFilterForApiDocsEndpoints() throws ServletException {
    // Arrange
    when(request.getRequestURI()).thenReturn("/v3/api-docs/swagger-config");

    // Act
    var shouldNotFilter = tenantExtractionFilter.shouldNotFilter(request);

    // Assert
    assertThat(shouldNotFilter).isTrue();
  }

  @Test
  @DisplayName("Should not skip filter for regular API endpoints")
  void shouldNotSkipFilterForRegularEndpoints() throws ServletException {
    // Arrange
    when(request.getRequestURI()).thenReturn("/api/v1/subscriptions");

    // Act
    var shouldNotFilter = tenantExtractionFilter.shouldNotFilter(request);

    // Assert
    assertThat(shouldNotFilter).isFalse();
  }

  @Test
  @DisplayName("Should clear tenant context even when exception occurs")
  void shouldClearTenantContextOnException() throws Exception {
    // Arrange
    when(request.getRequestURI()).thenReturn("/api/v1/subscriptions");

    // Simulate exception during filter chain
    org.mockito.Mockito.doThrow(new ServletException("Test exception"))
        .when(filterChain).doFilter(any(), any());

    // Act & Assert
    try {
      tenantExtractionFilter.doFilterInternal(request, response, filterChain);
    } catch (final ServletException e) {
      // Expected exception
    }

    assertThat(TenantContext.getCurrentTenantId()).isNull();
  }
}
