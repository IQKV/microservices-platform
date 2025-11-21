package org.gripday.bookstore.shared;

import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration for Bookstore Service (Downstream Microservice).
 *
 * <p>This configuration implements the Gripday platform's authentication architecture
 * for downstream microservices. It validates JWT tokens issued by the User Service
 * using RSA256 asymmetric encryption via JWK endpoint.
 *
 * <p><b>Architecture Overview:</b>
 * <pre>
 * Client → Gateway Service → Bookstore Service
 *            (validates JWT)    (validates JWT)
 *                 ↓                    ↓
 *          User Service JWK      User Service JWK
 *          (public keys)         (public keys)
 * </pre>
 *
 * <p><b>Authentication Flow:</b>
 * <ol>
 *   <li>Client sends request with JWT token in Authorization header</li>
 *   <li>Gateway validates token using RSA256 public key from JWK endpoint</li>
 *   <li>Gateway propagates user context via headers (X-User-ID, X-Username, etc.)</li>
 *   <li>Bookstore validates token again using same JWK endpoint</li>
 *   <li>Bookstore extracts user context from JWT claims</li>
 *   <li>Business logic executes with user context</li>
 * </ol>
 *
 * <p><b>Security Features:</b>
 * <ul>
 *   <li>Stateless authentication (no sessions)</li>
 *   <li>RSA256 token validation via JWK endpoint</li>
 *   <li>Automatic key rotation support (90-day rotation, 7-day grace period)</li>
 *   <li>Method-level security with @PreAuthorize</li>
 *   <li>Role-based access control (ADMIN, SUPERADMIN)</li>
 *   <li>Public endpoints for browsing (no auth required)</li>
 *   <li>CORS configuration for frontend apps</li>
 *   <li>Security headers (HSTS, frame options, etc.)</li>
 * </ul>
 *
 * <p><b>Endpoint Security Levels:</b>
 * <ul>
 *   <li><b>Public:</b> Book browsing, search, health checks (no auth)</li>
 *   <li><b>Authenticated:</b> Book details, inventory checks (valid JWT)</li>
 *   <li><b>Admin:</b> Create/update/delete books, inventory management (ADMIN/SUPERADMIN)</li>
 * </ul>
 *
 * @see JwtConfiguration
 * @see UserContextExtractor
 * @see JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CorsConfiguration corsConfiguration;

  public SecurityConfiguration(final JwtAuthenticationFilter jwtAuthenticationFilter,
                               final CorsConfiguration corsConfiguration) {
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

            // Admin-only endpoints - require ADMIN or SUPERADMIN authority
            .requestMatchers(HttpMethod.POST, "/api/v1/bookstore/books").hasAnyAuthority("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/bookstore/books/**").hasAnyAuthority("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/bookstore/books/**").hasAnyAuthority("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/bookstore/inventory/**").hasAnyAuthority("ADMIN", "SUPERADMIN")
            .requestMatchers(HttpMethod.POST, "/api/v1/bookstore/inventory/**").hasAnyAuthority("ADMIN", "SUPERADMIN")

            // All other requests require authentication
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        )
        .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Configures JWT authentication converter with custom authorities extraction.
   *
   * <p>This converter extracts user authorities from JWT claims and converts them
   * to Spring Security's GrantedAuthority format for use in authorization decisions.
   *
   * @return JwtAuthenticationConverter configured with custom authorities converter
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
    return converter;
  }

  /**
   * Extracts granted authorities from JWT claims.
   *
   * <p>This converter supports multiple JWT claim formats for maximum compatibility:
   * <ul>
   *   <li><b>roles</b> - Standard claim used by Gripday User Service</li>
   *   <li><b>authorities</b> - Alternative claim name</li>
   *   <li><b>realm_access.roles</b> - Keycloak compatibility</li>
   * </ul>
   *
   * <p><b>Important:</b> Authorities are used directly without "ROLE_" prefix.
   * Use {@code hasAnyAuthority("ADMIN", "SUPERADMIN")} instead of
   * {@code hasAnyRole("ADMIN", "SUPERADMIN")}.
   *
   * <p><b>Example JWT Claims:</b>
   * <pre>
   * {
   *   "userId": 1,
   *   "username": "johndoe",
   *   "roles": ["USER", "ADMIN"],
   *   "permissions": ["read:profile", "update:profile"]
   * }
   * </pre>
   *
   * @return Converter that extracts authorities from JWT claims
   */
  @Bean
  public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
    return jwt -> {
      // Extract roles from multiple possible claim locations
      var roles = Stream.<String>empty();

      // Try "roles" claim (Gripday User Service standard)
      var rolesClaim = jwt.getClaimAsStringList("roles");
      if (rolesClaim != null) {
        roles = Stream.concat(roles, rolesClaim.stream());
      }

      // Try "authorities" claim (alternative format)
      var authoritiesClaim = jwt.getClaimAsStringList("authorities");
      if (authoritiesClaim != null) {
        roles = Stream.concat(roles, authoritiesClaim.stream());
      }

      // Try "realm_access.roles" for Keycloak compatibility
      var realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess != null && realmAccess.containsKey("roles")) {
        @SuppressWarnings("unchecked")
        var realmRoles = (Collection<String>) realmAccess.get("roles");
        if (realmRoles != null) {
          roles = Stream.concat(roles, realmRoles.stream());
        }
      }

      // Convert to GrantedAuthority without adding ROLE_ prefix
      // This allows using hasAnyAuthority("ADMIN") instead of hasAnyRole("ADMIN")
      return roles
          .distinct()
          .map(SimpleGrantedAuthority::new)
          .map(authority -> (GrantedAuthority) authority)
          .toList();
    };
  }
}
