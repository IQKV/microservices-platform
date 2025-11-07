package org.gripday.bookstore.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CorsConfiguration corsConfiguration;

  public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
      CorsConfiguration corsConfiguration) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.corsConfiguration = corsConfiguration;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfiguration.corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> headers
            .frameOptions().deny()
            .contentTypeOptions().and()
            .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
            )
            .and()
        )
        .authorizeHttpRequests(authz -> authz
            // Public endpoints - no authentication required
            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/info").permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
            .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()

            // Public book browsing endpoints - no authentication required
            .requestMatchers(HttpMethod.GET, "/api/v1/bookstore/books").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/bookstore/books/available").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/bookstore/books/search/**").permitAll()

            // Authenticated endpoints - require valid JWT
            .requestMatchers(HttpMethod.GET, "/api/v1/bookstore/books/*").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/v1/bookstore/books/isbn/*").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/v1/bookstore/books/in-stock").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/v1/bookstore/inventory/**").authenticated()

            // Admin-only endpoints - require ADMIN or SUPERADMIN role
            .requestMatchers(HttpMethod.POST, "/api/v1/bookstore/books").hasAnyRole("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/bookstore/books/**").hasAnyRole("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/bookstore/books/**").hasAnyRole("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/bookstore/inventory/**").hasAnyRole("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.POST, "/api/v1/bookstore/inventory/**").hasAnyRole("ADMIN", "SUPERADMIN")

            // All other requests require authentication
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> {
            })
        )
        .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}