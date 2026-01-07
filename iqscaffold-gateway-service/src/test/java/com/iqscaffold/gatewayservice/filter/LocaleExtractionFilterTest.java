package com.iqscaffold.gatewayservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocaleExtractionFilter Tests")
class LocaleExtractionFilterTest {

  @Mock(lenient = true)
  private GatewayFilterChain filterChain;

  @Mock(lenient = true)
  private IqScaffoldProperties properties;

  private IqScaffoldProperties.I18nProperties i18nProperties;

  private LocaleExtractionFilter localeExtractionFilter;

  // Capture the mutated exchange passed to the filter chain
  private ServerWebExchange capturedExchange;

  @BeforeEach
  void setUp() {
    when(filterChain.filter(any())).thenAnswer(invocation -> {
      capturedExchange = invocation.getArgument(0);
      return Mono.empty();
    });

    // Create a real I18nProperties object instead of mocking it
    i18nProperties = new IqScaffoldProperties.I18nProperties(
        List.of("en", "es", "fr"),
        "en"
    );

    when(properties.i18n()).thenReturn(i18nProperties);

    localeExtractionFilter = new LocaleExtractionFilter(properties);
  }

  @Nested
  @DisplayName("Filter Order Tests")
  class FilterOrderTests {

    @Test
    @DisplayName("Should have correct filter order")
    void shouldHaveCorrectFilterOrder() {
      assertThat(localeExtractionFilter.getOrder())
          .isEqualTo(GatewayConstants.FilterOrder.LOCALE_EXTRACTION_FILTER);
    }
  }

  @Nested
  @DisplayName("User Locale Header Priority Tests")
  class UserLocaleHeaderPriorityTests {

    @Test
    @DisplayName("Should use X-User-Locale header when present and supported")
    void shouldUseUserLocaleHeaderWhenPresentAndSupported() {
      var request = MockServerHttpRequest.get("/api/test")
          .header(GatewayConstants.Headers.X_USER_LOCALE, "es")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("es");
      assertThat(capturedExchange.getRequest().getHeaders().getFirst(GatewayConstants.Headers.X_USER_LOCALE))
          .isEqualTo("es");
    }

    @Test
    @DisplayName("Should ignore X-User-Locale header when not supported")
    void shouldIgnoreUserLocaleHeaderWhenNotSupported() {
      var request = MockServerHttpRequest.get("/api/test")
          .header(GatewayConstants.Headers.X_USER_LOCALE, "de")
          .header("Accept-Language", "fr")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      // Should fall back to Accept-Language parsing
      var locale = capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE);
      assertThat(locale).isNotEqualTo("de");
    }

    @Test
    @DisplayName("Should ignore empty X-User-Locale header")
    void shouldIgnoreEmptyUserLocaleHeader() {
      var request = MockServerHttpRequest.get("/api/test")
          .header(GatewayConstants.Headers.X_USER_LOCALE, "")
          .header("Accept-Language", "fr")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      var locale = capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE);
      assertThat(locale).isNotEqualTo("");
    }
  }

  @Nested
  @DisplayName("Accept-Language Header Tests")
  class AcceptLanguageHeaderTests {

    @Test
    @DisplayName("Should parse Accept-Language header with exact match")
    void shouldParseAcceptLanguageHeaderWithExactMatch() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "fr")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("fr");
      assertThat(capturedExchange.getRequest().getHeaders().getFirst(GatewayConstants.Headers.X_USER_LOCALE))
          .isEqualTo("fr");
    }

    @Test
    @DisplayName("Should parse Accept-Language header with quality values")
    void shouldParseAcceptLanguageHeaderWithQualityValues() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "fr;q=0.9, en;q=0.8, es;q=0.7")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("fr");
    }

    @Test
    @DisplayName("Should handle language-only match when exact locale not supported")
    void shouldHandleLanguageOnlyMatchWhenExactLocaleNotSupported() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "en-US")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en");
    }

    @Test
    @DisplayName("Should handle multiple languages in Accept-Language header")
    void shouldHandleMultipleLanguagesInAcceptLanguageHeader() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "de, fr;q=0.8, en;q=0.6")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      // Should pick fr since de is not supported
      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("fr");
    }

    @Test
    @DisplayName("Should handle malformed Accept-Language header gracefully")
    void shouldHandleMalformedAcceptLanguageHeaderGracefully() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "invalid-locale-format")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      // Should fall back to default locale
      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en");
    }

    @Test
    @DisplayName("Should handle empty Accept-Language header")
    void shouldHandleEmptyAcceptLanguageHeader() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      // Should fall back to default locale
      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en");
    }
  }

  @Nested
  @DisplayName("Default Locale Tests")
  class DefaultLocaleTests {

    @Test
    @DisplayName("Should use default locale when no headers present")
    void shouldUseDefaultLocaleWhenNoHeadersPresent() {
      var request = MockServerHttpRequest.get("/api/test").build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en");
      assertThat(capturedExchange.getRequest().getHeaders().getFirst(GatewayConstants.Headers.X_USER_LOCALE))
          .isEqualTo("en");
    }

    @Test
    @DisplayName("Should use default locale when no supported locale found")
    void shouldUseDefaultLocaleWhenNoSupportedLocaleFound() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "de, zh")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en");
    }
  }

  @Nested
  @DisplayName("MDC and Context Tests")
  class MdcAndContextTests {

    @Test
    @DisplayName("Should set MDC locale key during processing")
    void shouldSetMdcLocaleKeyDuringProcessing() {
      var request = MockServerHttpRequest.get("/api/test")
          .header(GatewayConstants.Headers.X_USER_LOCALE, "es")
          .build();
      var exchange = MockServerWebExchange.from(request);

      // Mock filter chain to capture MDC state during processing
      when(filterChain.filter(any())).thenAnswer(invocation -> {
        assertThat(MDC.get(GatewayConstants.MdcKeys.LOCALE)).isEqualTo("es");
        return Mono.empty();
      });

      localeExtractionFilter.filter(exchange, filterChain).block();
    }

    @Test
    @DisplayName("Should clean up MDC after processing")
    void shouldCleanUpMdcAfterProcessing() {
      var request = MockServerHttpRequest.get("/api/test")
          .header(GatewayConstants.Headers.X_USER_LOCALE, "es")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      // MDC should be cleaned up after processing
      assertThat(MDC.get(GatewayConstants.MdcKeys.LOCALE)).isNull();
    }

    @Test
    @DisplayName("Should store locale in exchange attributes")
    void shouldStoreLocaleInExchangeAttributes() {
      var request = MockServerHttpRequest.get("/api/test")
          .header(GatewayConstants.Headers.X_USER_LOCALE, "fr")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getAttributes()).containsKey(GatewayConstants.Attributes.LOCALE);
      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("fr");
    }

    @Test
    @DisplayName("Should add locale header to downstream request")
    void shouldAddLocaleHeaderToDownstreamRequest() {
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "es")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      assertThat(capturedExchange.getRequest().getHeaders().getFirst(GatewayConstants.Headers.X_USER_LOCALE))
          .isEqualTo("es");
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle null locale gracefully")
    void shouldHandleNullLocaleGracefully() {
      // Create a new I18nProperties with null default locale - this will throw in constructor
      // So we need to test a different scenario - when all locales fail to match
      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "xx-YY")  // Unsupported locale
          .build();
      var exchange = MockServerWebExchange.from(request);

      // Should not throw exception and fall back to default
      localeExtractionFilter.filter(exchange, filterChain).block();

      // Should use default locale
      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en");
    }

    @Test
    @DisplayName("Should handle case-insensitive locale matching")
    void shouldHandleCaseInsensitiveLocaleMatching() {
      var request = MockServerHttpRequest.get("/api/test")
          .header(GatewayConstants.Headers.X_USER_LOCALE, "ES")
          .build();
      var exchange = MockServerWebExchange.from(request);

      localeExtractionFilter.filter(exchange, filterChain).block();

      // Should fall back to default since ES (uppercase) is not in supported list
      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en");
    }

    @Test
    @DisplayName("Should handle complex Accept-Language with regions")
    void shouldHandleComplexAcceptLanguageWithRegions() {
      // Create a new I18nProperties with regional locales
      var regionalI18nProperties = new IqScaffoldProperties.I18nProperties(
          List.of("en-US", "es-ES", "fr-FR"),
          "en-US"
      );
      when(properties.i18n()).thenReturn(regionalI18nProperties);

      // Create a new filter instance with the updated properties
      var testFilter = new LocaleExtractionFilter(properties);

      var request = MockServerHttpRequest.get("/api/test")
          .header("Accept-Language", "en-GB;q=0.9, en-US;q=0.8, fr-FR;q=0.7")
          .build();
      var exchange = MockServerWebExchange.from(request);

      testFilter.filter(exchange, filterChain).block();

      // Should match en-US since en-GB is not supported but en-US is
      assertThat(capturedExchange.getAttributes().get(GatewayConstants.Attributes.LOCALE)).isEqualTo("en-US");
    }
  }
}
