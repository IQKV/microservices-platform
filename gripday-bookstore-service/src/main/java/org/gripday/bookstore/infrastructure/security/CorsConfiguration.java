package org.gripday.bookstore.infrastructure.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfiguration {

  @Value("${gripday.bookstore.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
  private String[] allowedOrigins;

  @Value("${gripday.bookstore.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
  private String[] allowedMethods;

  @Value("${gripday.bookstore.cors.allowed-headers:*}")
  private String[] allowedHeaders;

  @Value("${gripday.bookstore.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Value("${gripday.bookstore.cors.max-age:3600}")
  private long maxAge;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var configuration = new org.springframework.web.cors.CorsConfiguration();

    // Set allowed origins
    configuration.setAllowedOriginPatterns(List.of(allowedOrigins));

    // Set allowed methods
    configuration.setAllowedMethods(Arrays.asList(allowedMethods));

    // Set allowed headers
    if (allowedHeaders.length == 1 && "*".equals(allowedHeaders[0])) {
      configuration.addAllowedHeader("*");
    } else {
      configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
    }

    // Set credentials
    configuration.setAllowCredentials(allowCredentials);

    // Set max age
    configuration.setMaxAge(maxAge);

    // Expose headers that clients might need
    configuration.setExposedHeaders(List.of(
        "X-Correlation-ID",
        "X-Total-Count",
        "X-Page-Number",
        "X-Page-Size",
        "Authorization"
    ));

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}