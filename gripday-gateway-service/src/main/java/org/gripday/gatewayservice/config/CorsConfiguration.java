package org.gripday.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for the gateway service.
 * Provides environment-specific CORS policies for cross-origin requests.
 */
@Configuration
public class CorsConfiguration {

    private final GatewayProperties gatewayProperties;

    public CorsConfiguration(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * Configure CORS filter with environment-specific settings.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        var corsConfig = new org.springframework.web.cors.CorsConfiguration();
        var corsProperties = gatewayProperties.cors();

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
        corsConfig.setMaxAge(corsProperties.maxAge());

        // Expose headers that clients might need
        corsConfig.addExposedHeader("X-Correlation-ID");
        corsConfig.addExposedHeader("X-Request-ID");
        corsConfig.addExposedHeader("X-Total-Count");
        corsConfig.addExposedHeader("X-Page-Number");
        corsConfig.addExposedHeader("X-Page-Size");

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}