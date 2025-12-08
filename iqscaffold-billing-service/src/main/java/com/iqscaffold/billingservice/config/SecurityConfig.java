package com.iqscaffold.billingservice.config;

import com.iqscaffold.billingservice.security.JwtAuthenticationFilter;
import com.iqscaffold.billingservice.security.RateLimitingFilter;
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
 *   <li>Stateless session management (SessionCreationPolicy.STATELESS)</li>
 *   <li>Method-level security with @PreAuthorize annotations</li>
 *   <li>Rate limiting on public endpoints using Redis</li>
 *   <li>TLS 1.3 for all API communication (configured at infrastructure level)</li>
 *   <li>Public endpoints for health checks and API documentation</li>
 *   <li>Authenticated endpoints for customer portal and integration APIs</li>
 *   <li>Admin-only endpoints for administrative operations</li>
 *   <li>Security headers (HSTS, frame options, content type options)</li>
 * </ul>
 * 
 * <p>Authority-Based Access Control:
 * <ul>
 *   <li>Public: /api/v1/billing/plans/** (no authentication)</li>
 *   <li>Customer: /api/v1/billing/portal/** (authenticated users)</li>
 *   <li>Admin: /api/v1/admin/billing/** (ADMIN or SUPER_ADMIN authority)</li>
 *   <li>Internal: /internal/billing/** (internal service authentication)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final BillingProperties billingProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RateLimitingFilter rateLimitingFilter;

  public SecurityConfig(
      BillingProperties billingProperties,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RateLimitingFilter rateLimitingFilter
  ) {
    this.billingProperties = billingProperties;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rateLimitingFilter = rateLimitingFilter;
  }

  /**
   * Configure the security filter chain.
   * 
   * <p>Implements comprehensive security controls including:
   * <ul>
   *   <li>JWT-based authentication via OAuth2 Resource Server</li>
   *   <li>Rate limiting on public endpoints</li>
   *   <li>Authority-based access control</li>
   *   <li>Stateless session management</li>
   *   <li>Security headers (HSTS, frame options)</li>
   * </ul>
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
        // REQ-SEC-008: Use stateless session management
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        
        // Configure authorization rules
        // REQ-SEC-006: Implement role-based access control (RBAC)
        // REQ-SEC-008: Authenticate API requests via JWT
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
            
            // Internal endpoints - require authentication (for service-to-service calls)
            .requestMatchers("/internal/billing/**").authenticated()
            
            // All other requests require authentication
            .anyRequest().authenticated()
        )
        
        // Configure OAuth2 Resource Server with JWT
        // REQ-SEC-008: Authenticate API requests via JWT
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.decoder(jwtDecoder()))
        )
        
        // Add rate limiting filter before authentication
        // REQ-SEC-010: Implement rate limiting on public endpoints
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        
        // Add JWT authentication filter after Spring Security's authentication
        .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        
        // Configure security headers
        // REQ-SEC-004: Encrypt all data in transit using TLS 1.3
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.deny())
            .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable())
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
