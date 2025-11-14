package org.gripday.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the Gateway Service. Configures CORS and authorization policies.
 * JWT authentication uses RSA256 validation via JWK endpoint from User Service.
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
            // All other requests require authentication
            .anyExchange()
            .authenticated()
        )
        // Use OAuth2 Resource Server with JWK Set
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
        )
        .build();
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    var jwkSetUri = gripdayProperties.gateway().security().jwt().jwkSetUri();
    return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
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