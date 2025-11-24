package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiVersionInterceptor Tests")
class ApiVersionInterceptorTest {

  private ApiVersionInterceptor interceptor;

  @Mock
  private ApiDeprecationNotice deprecationNotice;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @BeforeEach
  void setUp() {
    interceptor = new ApiVersionInterceptor(deprecationNotice);
  }

  @Test
  @DisplayName("Should extract version from API-Version header")
  void shouldExtractVersionFromApiVersionHeader() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn("2");
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "2");
    verify(response).setHeader("API-Version", "2");
    verify(response).setHeader("Supported-Versions", "1");
  }

  @Test
  @DisplayName("Should extract version from Accept header with v1")
  void shouldExtractVersionFromAcceptHeaderWithV1() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn(null);
    when(request.getHeader("Accept")).thenReturn("application/vnd.gripday.bookstore.v1+json");
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "1");
    verify(response).setHeader("API-Version", "1");
  }

  @Test
  @DisplayName("Should extract version from Accept header with v2")
  void shouldExtractVersionFromAcceptHeaderWithV2() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn(null);
    when(request.getHeader("Accept")).thenReturn("application/vnd.gripday.bookstore.v2+json");
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "2");
  }

  @Test
  @DisplayName("Should extract version from URL path with v1")
  void shouldExtractVersionFromUrlPathWithV1() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn(null);
    when(request.getHeader("Accept")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api/v1/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "1");
  }

  @Test
  @DisplayName("Should extract version from URL path with v2")
  void shouldExtractVersionFromUrlPathWithV2() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn(null);
    when(request.getHeader("Accept")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api/v2/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "2");
  }

  @Test
  @DisplayName("Should extract version from query parameter")
  void shouldExtractVersionFromQueryParameter() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn(null);
    when(request.getHeader("Accept")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(request.getParameter("version")).thenReturn("2");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "2");
  }

  @Test
  @DisplayName("Should default to version 1 when no version specified")
  void shouldDefaultToVersion1WhenNoVersionSpecified() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn(null);
    when(request.getHeader("Accept")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(request.getParameter("version")).thenReturn(null);
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "1");
  }

  @Test
  @DisplayName("Should normalize version with v prefix")
  void shouldNormalizeVersionWithVPrefix() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn("v2");
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "2");
  }

  @Test
  @DisplayName("Should normalize version with uppercase V prefix")
  void shouldNormalizeVersionWithUppercaseVPrefix() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn("V2");
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "2");
  }

  @Test
  @DisplayName("Should default to version 1 for invalid version format")
  void shouldDefaultToVersion1ForInvalidVersionFormat() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn("invalid");
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(request).setAttribute("apiVersion", "1");
  }

  @Test
  @DisplayName("Should handle deprecated version")
  void shouldHandleDeprecatedVersion() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn("1");
    when(request.getRequestURI()).thenReturn("/api/v1/bookstore/books");
    when(deprecationNotice.isVersionDeprecated("1")).thenReturn(true);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
    verify(deprecationNotice).isVersionDeprecated("1");
  }

  @Test
  @DisplayName("Should always return true to continue filter chain")
  void shouldAlwaysReturnTrueToContinueFilterChain() {
    // Arrange
    when(request.getHeader("API-Version")).thenReturn("1");
    when(request.getRequestURI()).thenReturn("/api/bookstore/books");
    when(deprecationNotice.isVersionDeprecated(anyString())).thenReturn(false);

    // Act
    var result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
  }
}
