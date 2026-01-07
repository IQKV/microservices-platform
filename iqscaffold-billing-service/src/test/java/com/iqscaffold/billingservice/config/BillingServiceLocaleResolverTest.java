package com.iqscaffold.billingservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingServiceLocaleResolverTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private BillingServiceLocaleResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new BillingServiceLocaleResolver();
  }

  @Test
  void resolveLocale_shouldUseUserLocaleHeaderWhenPresent() {
    // Given
    when(request.getHeader("X-User-Locale")).thenReturn("fr-FR");
    resolver.setSupportedLocales(Arrays.asList(Locale.FRENCH, Locale.ENGLISH));

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertEquals("fr", result.getLanguage());
  }

  @Test
  void resolveLocale_shouldUseUserLocaleHeaderWithLanguageMatch() {
    // Given
    when(request.getHeader("X-User-Locale")).thenReturn("en-US");
    resolver.setSupportedLocales(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertEquals("en", result.getLanguage());
  }

  @Test
  void resolveLocale_shouldFallbackToAcceptLanguageWhenUserLocaleInvalid() {
    // Given
    when(request.getHeader("X-User-Locale")).thenReturn("invalid-locale");
    when(request.getLocale()).thenReturn(Locale.US);

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertNotNull(result);
    assertEquals(Locale.US, result);
  }

  @Test
  void resolveLocale_shouldFallbackToAcceptLanguageWhenUserLocaleNotSupported() {
    // Given
    when(request.getHeader("X-User-Locale")).thenReturn("de-DE");
    when(request.getLocale()).thenReturn(Locale.US);
    resolver.setSupportedLocales(Arrays.asList(Locale.ENGLISH, Locale.FRENCH));

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertNotNull(result);
    assertEquals(Locale.US, result);
  }

  @Test
  void resolveLocale_shouldFallbackToAcceptLanguageWhenUserLocaleHeaderEmpty() {
    // Given
    when(request.getHeader("X-User-Locale")).thenReturn("");
    when(request.getLocale()).thenReturn(Locale.US);

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertNotNull(result);
    assertEquals(Locale.US, result);
  }

  @Test
  void resolveLocale_shouldFallbackToAcceptLanguageWhenUserLocaleHeaderNull() {
    // Given
    when(request.getHeader("X-User-Locale")).thenReturn(null);
    when(request.getLocale()).thenReturn(Locale.US);

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertNotNull(result);
    assertEquals(Locale.US, result);
  }

  @Test
  void resolveLocale_shouldAcceptAnyLocaleWhenNoSupportedLocalesConfigured() {
    // Given
    when(request.getHeader("X-User-Locale")).thenReturn("ja-JP");
    resolver.setSupportedLocales(java.util.Collections.emptyList());

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertEquals("ja", result.getLanguage());
  }

  @Test
  void resolveLocale_shouldHandleExceptionGracefully() {
    // Given
    when(request.getHeader("X-User-Locale")).thenThrow(new RuntimeException("Header error"));

    // When
    Locale result = resolver.resolveLocale(request);

    // Then
    assertNotNull(result);
    // Should fall back to default locale when exception occurs
  }

  @Test
  void setLocale_shouldThrowUnsupportedOperationException() {
    // When & Then
    assertThrows(UnsupportedOperationException.class, () ->
        resolver.setLocale(request, response, Locale.FRENCH)
    );
  }

  @Test
  void setLocale_shouldThrowExceptionWithDescriptiveMessage() {
    // When & Then
    UnsupportedOperationException exception = assertThrows(
        UnsupportedOperationException.class,
        () -> resolver.setLocale(request, response, Locale.FRENCH)
    );

    assertTrue(exception.getMessage().contains("user preferences"));
  }
}
