package com.iqscaffold.billingservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.iqscaffold.billingservice.shared.BillingConstants;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter for extracting user context from validated JWT tokens.
 *
 * <p>This filter runs after Spring Security's OAuth2 Resource Server has validated
 * the JWT token. It extracts user information from JWT claims and:
 * <ul>
 *   <li>Creates a UserContext from JWT claims</li>
 *   <li>Sets UserContext in SecurityContextHolder</li>
 *   <li>Sets tenant ID in TenantContext for multi-tenancy</li>
 *   <li>Adds user ID and tenant ID to MDC for structured logging</li>
 * </ul>
 *
 * <p>The filter follows the gateway-service + user-service JWT propagation pattern.
 */
@Component
@Order(2) // Execute after TenantExtractionFilter (order 1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    try {
      // Get authentication from SecurityContext (set by Spring Security OAuth2 Resource Server)
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
        Jwt jwt = jwtAuthToken.getToken();

        // Extract user context from JWT
        UserContext userContext = extractUserContext(jwt);

        // Set tenant context if not already set by TenantExtractionFilter
        if (!TenantContext.hasTenantContext() && userContext.tenantId() != null) {
          TenantContext.setCurrentTenantId(userContext.tenantId());
          logger.debug("Tenant context set from JWT: {}", userContext.tenantId());
        }

        // Add user ID to MDC for structured logging
        if (userContext.userId() != null) {
          MDC.put(BillingConstants.MDC.USER_ID, userContext.userId().toString());
        }

        // Store UserContext in request attribute for access in controllers
        request.setAttribute("userContext", userContext);

        logger.debug("User context established: username={}, tenantId={}, authorities={}",
            userContext.username(), userContext.tenantId(), userContext.authorities());
      }

      filterChain.doFilter(request, response);

    } finally {
      // Clean up MDC
      MDC.remove(BillingConstants.MDC.USER_ID);
    }
  }

  /**
   * Extract UserContext from JWT token claims.
   *
   * @param jwt the JWT token
   * @return the extracted UserContext
   */
  private UserContext extractUserContext(Jwt jwt) {
    // Extract user ID from 'sub' claim
    Long userId = extractLong(jwt.getClaim(JwtClaimNames.SUBJECT));

    // Extract username
    String username = jwt.getClaim(JwtClaimNames.USERNAME);

    // Extract email
    String email = jwt.getClaim(JwtClaimNames.EMAIL);

    // Extract authorities from 'roles' claim (despite the name, it contains authorities)
    Set<String> authorities = extractAuthorities(jwt.getClaim(JwtClaimNames.ROLES));

    // Extract tenant ID
    String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);

    // Extract first and last name
    String firstName = jwt.getClaim(JwtClaimNames.FIRST_NAME);
    String lastName = jwt.getClaim(JwtClaimNames.LAST_NAME);

    return new UserContext(
        userId,
        username,
        email,
        authorities,
        tenantId,
        firstName,
        lastName
    );
  }

  /**
   * Extract Long value from JWT claim.
   * Handles different numeric types that might be in the JWT.
   *
   * @param value the claim value
   * @return the Long value, or null if not a valid number
   */
  private Long extractLong(Object value) {
    return switch (value) {
      case Long l -> l;
      case Integer i -> i.longValue();
      case String s -> {
        try {
          yield Long.parseLong(s);
        } catch (final NumberFormatException e) {
          logger.warn("Failed to parse user ID from string: {}", s);
          yield null;
        }
      }
      case null, default -> null;
    };
  }

  /**
   * Extract authorities from JWT claim.
   * Handles both List and Set types.
   *
   * @param value the claim value
   * @return the set of authorities
   */
  @SuppressWarnings("unchecked")
  private Set<String> extractAuthorities(Object value) {
    return switch (value) {
      case List<?> list -> new HashSet<>(list.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .toList());
      case Set<?> set -> new HashSet<>(set.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .toList());
      case null, default -> Collections.emptySet();
    };
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String requestURI = request.getRequestURI();

    // Skip JWT processing for public paths
    return requestURI.startsWith("/actuator/")
           || requestURI.startsWith("/swagger-ui/")
           || requestURI.startsWith("/v3/api-docs/")
           || requestURI.equals("/favicon.ico")
           || requestURI.equals("/error");
  }
}
