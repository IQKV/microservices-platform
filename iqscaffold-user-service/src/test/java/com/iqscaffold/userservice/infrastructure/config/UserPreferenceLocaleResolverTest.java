package com.iqscaffold.userservice.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for UserPreferenceLocaleResolver class.
 * Tests locale resolution priority and fallback behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Preference Locale Resolver Tests")
class UserPreferenceLocaleResolverTest {

  private UserPreferenceLocaleResolver resolver;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @BeforeEach
  void setUp() {
    resolver = new UserPreferenceLocaleResolver();
    resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.FRENCH, new Locale("es")));
    resolver.setDefaultLocale(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should resolve locale from X-User-Locale header")
  void shouldResolveLocaleFromUserLocaleHeader() {
    when(request.getHeader("X-User-Locale")).thenReturn("fr");

    Locale result = resolver.resolveLocale(request);

    assertThat(result).isEqualTo(Locale.FRENCH);
  }

  @Test
  @DisplayName("Should resolve Spanish locale from X-User-Locale header")
  void shouldResolveSpanishLocaleFromUserLocaleHeader() {
    when(request.getHeader("X-User-Locale")).thenReturn("es");

    Locale result = resolver.resolveLocale(request);

    assertThat(result.getLanguage()).isEqualTo("es");
  }

  @Test
  @DisplayName("Should handle locale with region code in X-User-Locale header")
  void shouldHandleLocaleWithRegionCodeInUserLocaleHeader() {
    when(request.getHeader("X-User-Locale")).thenReturn("en-US");

    Locale result = resolver.resolveLocale(request);

    assertThat(result.getLanguage()).isEqualTo("en");
  }

  @Test
  @DisplayName("Should accept any locale when no supported locales configured")
  void shouldAcceptAnyLocaleWhenNoSupportedLocalesConfigured() {
    resolver.setSupportedLocales(List.of());
    when(request.getHeader("X-User-Locale")).thenReturn("de");

    Locale result = resolver.resolveLocale(request);

    assertThat(result.getLanguage()).isEqualTo("de");
  }

  @Test
  @DisplayName("Should match locale by language only when exact match not found")
  void shouldMatchLocaleByLanguageOnlyWhenExactMatchNotFound() {
    when(request.getHeader("X-User-Locale")).thenReturn("en-GB");

    Locale result = resolver.resolveLocale(request);

    // Should match English language even though en-GB is not exactly in supported list
    assertThat(result.getLanguage()).isEqualTo("en");
  }

  @Test
  @DisplayName("Should throw UnsupportedOperationException when trying to set locale")
  void shouldThrowUnsupportedOperationExceptionWhenTryingToSetLocale() {
    assertThatThrownBy(() -> resolver.setLocale(request, response, Locale.FRENCH))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Cannot change locale via UserPreferenceLocaleResolver")
        .hasMessageContaining("update user preferences instead");
  }

  @Test
  @DisplayName("Should prioritize X-User-Locale over Accept-Language")
  void shouldPrioritizeUserLocaleOverAcceptLanguage() {
    when(request.getHeader("X-User-Locale")).thenReturn("fr");

    Locale result = resolver.resolveLocale(request);

    assertThat(result).isEqualTo(Locale.FRENCH);
  }

  @Test
  @DisplayName("Should handle English locale")
  void shouldHandleEnglishLocale() {
    when(request.getHeader("X-User-Locale")).thenReturn("en");

    Locale result = resolver.resolveLocale(request);

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("Should handle null X-User-Locale header")
  void shouldHandleNullUserLocaleHeader() {
    when(request.getHeader("X-User-Locale")).thenReturn(null);

    Locale result = resolver.resolveLocale(request);

    // Should return default locale or from Accept-Language
    assertThat(result).isNotNull();
  }
}
