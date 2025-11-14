package org.gripday.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Enhanced security configuration for the user service. Configures JWT-based authentication, CSRF protection, and rate limiting.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final JwtDecoder jwtDecoder;
  private final RateLimitingFilter rateLimitingFilter;

  public SecurityConfig(final JwtDecoder jwtDecoder, final RateLimitingFilter rateLimitingFilter) {
    this.jwtDecoder = jwtDecoder;
    this.rateLimitingFilter = rateLimitingFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    // Use BCrypt with strength 12 for enhanced security
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        // Configure CSRF protection for state-changing operations
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            // Disable CSRF for API endpoints (using JWT tokens)
            .ignoringRequestMatchers("/api/**")
            // Enable CSRF for web endpoints if any
            .ignoringRequestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/password/forgot", "/api/v1/password/reset").permitAll()
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            // JWK endpoint must be public for downstream services
            .requestMatchers("/.well-known/jwks.json").permitAll()
            // Protected endpoints
            .requestMatchers("/api/v1/auth/refresh", "/api/v1/auth/logout").authenticated()
            .requestMatchers("/api/v1/admin/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
            .requestMatchers("/api/v1/tenants/**").hasRole("SUPER_ADMIN")
            // All other requests require authentication
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.decoder(jwtDecoder))
        )
        // Add rate limiting filter before authentication
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        // Security headers
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.deny())
            .contentTypeOptions(contentTypeOptions -> {
            })
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
            )
        )
        .build();
  }
}
