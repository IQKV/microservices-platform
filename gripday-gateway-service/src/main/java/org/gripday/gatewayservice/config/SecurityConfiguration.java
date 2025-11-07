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
 * Security configuration for the Gateway Service. Configures CORS, authentication, and authorization policies.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  private final GatewayProperties gatewayProperties;

  public SecurityConfiguration(GatewayProperties gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(exchanges -> exchanges
            // Public paths - no authentication required
            .pathMatchers(gatewayProperties.security().publicPaths().toArray(new String[0]))
            .permitAll()
            // Health and actuator endpoints
            .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info")
            .permitAll()
            // All other requests require authentication
            .anyExchange()
            .authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwkSetUri("http://localhost:8081/.well-known/jwks.json"))
        )
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var corsConfig = gatewayProperties.cors();
    var configuration = new CorsConfiguration();

    if (corsConfig.enabled()) {
      configuration.setAllowedOriginPatterns(corsConfig.allowedOrigins());
      configuration.setAllowedMethods(corsConfig.allowedMethods());
      configuration.setAllowedHeaders(corsConfig.allowedHeaders());
      configuration.setAllowCredentials(corsConfig.allowCredentials());
      configuration.setMaxAge(corsConfig.maxAge());
    }

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}