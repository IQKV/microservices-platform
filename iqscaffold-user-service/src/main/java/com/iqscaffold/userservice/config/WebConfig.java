package com.iqscaffold.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for HTTP standards, content negotiation, and CORS. Implements proper Content-Type and Accept header handling.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer
        // Default to JSON
        .defaultContentType(MediaType.APPLICATION_JSON)
        // Support path extension negotiation
        .favorPathExtension(false)
        // Support parameter-based negotiation
        .favorParameter(true)
        .parameterName("format")
        // Support Accept header negotiation
        .ignoreAcceptHeader(false)
        // Media type mappings
        .mediaType("json", MediaType.APPLICATION_JSON)
        .mediaType("xml", MediaType.APPLICATION_XML)
        .mediaType("html", MediaType.TEXT_HTML);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .exposedHeaders(
            "X-Correlation-ID",
            "X-Request-ID",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset",
            "Location"
        )
        .maxAge(3600);
  }
}