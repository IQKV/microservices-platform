package org.gripday.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the Gateway Service. Configures CORS and authorization policies.
 * JWT authentication is handled by JwtAuthenticationFilter using HMAC-based validation.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  private final GripdayProperties gripdayProperties;

  public SecurityConfiguration(final GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(exchanges -> exchanges
            // Public paths - no authentication required
            .pathMatchers(gripdayProperties.gateway().security().publicPaths().toArray(new String[0]))
            .permitAll()
            // Health and actuator endpoints
            .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info")
            .permitAll()
            // All other requests require authentication (handled by JwtAuthenticationFilter)
            .anyExchange()
            .permitAll() // JwtAuthenticationFilter handles authentication
        )
        .build();
  }

  /**
   * CORS configuration source using GripdayProperties settings.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var corsConfig = gripdayProperties.gateway().cors();
    var configuration = new CorsConfiguration();

    if (corsConfig.enabled()) {
      configuration.setAllowedOriginPatterns(corsConfig.allowedOrigins());
      configuration.setAllowedMethods(corsConfig.allowedMethods());
      configuration.setAllowedHeaders(corsConfig.allowedHeaders());
      configuration.setAllowCredentials(corsConfig.allowCredentials());
      configuration.setMaxAge((long) corsConfig.maxAge());
    }

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}