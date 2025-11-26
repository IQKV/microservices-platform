package com.iqscaffold.gatewayservice.config;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for the gateway service. Provides environment-specific CORS policies for cross-origin requests.
 */
@Configuration
public class CorsConfiguration {

  private final IqScaffoldProperties systemProperties;

  public CorsConfiguration(final IqScaffoldProperties systemProperties) {
    this.systemProperties = systemProperties;
  }

  /**
   * Configure CORS filter with environment-specific settings.
   */
  @Bean
  public CorsWebFilter corsWebFilter() {
    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
    var corsProperties = systemProperties.gateway().cors();

    // Configure allowed origins
    if (corsProperties.allowedOrigins() != null && !corsProperties.allowedOrigins().isEmpty()) {
      corsConfig.setAllowedOriginPatterns(corsProperties.allowedOrigins());
    } else {
      // Default development origins
      corsConfig.addAllowedOriginPattern("http://localhost:*");
      corsConfig.addAllowedOriginPattern("https://localhost:*");
    }

    // Configure allowed methods
    if (corsProperties.allowedMethods() != null && !corsProperties.allowedMethods().isEmpty()) {
      corsConfig.setAllowedMethods(corsProperties.allowedMethods());
    } else {
      // Default allowed methods
      corsConfig.addAllowedMethod("GET");
      corsConfig.addAllowedMethod("POST");
      corsConfig.addAllowedMethod("PUT");
      corsConfig.addAllowedMethod("PATCH");
      corsConfig.addAllowedMethod("DELETE");
      corsConfig.addAllowedMethod("OPTIONS");
    }

    // Configure allowed headers
    if (corsProperties.allowedHeaders() != null && !corsProperties.allowedHeaders().isEmpty()) {
      corsConfig.setAllowedHeaders(corsProperties.allowedHeaders());
    } else {
      // Allow all headers by default
      corsConfig.addAllowedHeader("*");
    }

    // Configure credentials
    corsConfig.setAllowCredentials(corsProperties.allowCredentials());

    // Configure max age
    corsConfig.setMaxAge((long) corsProperties.maxAge());

    // Expose headers that clients might need
    corsConfig.addExposedHeader(GatewayConstants.Headers.X_CORRELATION_ID);
    corsConfig.addExposedHeader(GatewayConstants.Headers.X_REQUEST_ID);
    corsConfig.addExposedHeader(GatewayConstants.Headers.X_TOTAL_COUNT);
    corsConfig.addExposedHeader(GatewayConstants.Headers.X_PAGE_NUMBER);
    corsConfig.addExposedHeader(GatewayConstants.Headers.X_PAGE_SIZE);

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }
}
