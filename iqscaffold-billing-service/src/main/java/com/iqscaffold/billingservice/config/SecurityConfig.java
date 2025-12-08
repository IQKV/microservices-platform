package com.iqscaffold.billingservice.config;

import com.iqscaffold.billingservice.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Billing Service.
 * 
 * <p>Configures JWT-based authentication using OAuth2 Resource Server,
 * following the same security patterns as the User Service.
 * 
 * <p>Security Features:
 * <ul>
 *   <li>JWT token validation using JWK endpoint from User Service</li>
 *   <li>Stateless session management</li>
 *   <li>Method-level security with @PreAuthorize annotations</li>
 *   <li>Public endpoints for health checks and API documentation</li>
 *   <li>Authenticated endpoints for customer portal and integration APIs</li>
 *   <li>Admin-only endpoints for administrative operations</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final BillingProperties billingProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(
      BillingProperties billingProperties,
      JwtAuthenticationFilter jwtAuthenticationFilter
  ) {
    this.billingProperties = billingProperties;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  /**
   * Configure the security filter chain.
   * 
   * @param http the HttpSecurity to configure
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        // Disable CSRF for API endpoints (using JWT tokens)
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**", "/actuator/**")
        )
        
        // Stateless session management (no server-side sessions)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        
        // Configure authorization rules
        .authorizeHttpRequests(auth -> auth
            // Public endpoints - no authentication required
            .requestMatchers("/api/v1/billing/plans/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/error").permitAll()
            
            // Integration APIs - require authentication
            .requestMatchers("/api/v1/billing/subscriptions/*/status").authenticated()
            .requestMatchers("/api/v1/billing/subscriptions/*/check-feature").authenticated()
            .requestMatchers("/api/v1/billing/usage/**").authenticated()
            
            // Customer portal - require authentication
            .requestMatchers("/api/v1/billing/portal/**").authenticated()
            
            // Admin endpoints - require ADMIN or SUPER_ADMIN authority
            .requestMatchers("/api/v1/admin/billing/**").hasAnyAuthority("ADMIN", "SUPER_ADMIN")
            
            // All other requests require authentication
            .anyRequest().authenticated()
        )
        
        // Configure OAuth2 Resource Server with JWT
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.decoder(jwtDecoder()))
        )
        
        // Add JWT authentication filter after Spring Security's authentication
        .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        
        // Configure security headers
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.deny())
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
            )
        )
        
        .build();
  }

  /**
   * Configure JWT decoder to validate tokens using JWK endpoint from User Service.
   * 
   * @return the configured JwtDecoder
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    String jwkSetUri = billingProperties.security().jwt().jwkSetUri();
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }
}
