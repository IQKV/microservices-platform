package com.iqscaffold.contactservice.security;

import com.iqscaffold.contactservice.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT Authentication Filter that extracts user context from JWT claims and sets tenant context.
 * 
 * <p>This filter runs after Spring Security's OAuth2 Resource Server has validated the JWT token.
 * It extracts user information from JWT claims and establishes the tenant context for the request.
 * 
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Extract user context from JWT claims</li>
 *   <li>Set tenant context from JWT or X-Tenant-ID header</li>
 *   <li>Add correlation ID to MDC for distributed tracing</li>
 *   <li>Clear context in finally block to prevent memory leaks</li>
 * </ul>
 * 
 * <h3>Tenant Context Priority:</h3>
 * <ol>
 *   <li>X-Tenant-ID header (set by Gateway Service)</li>
 *   <li>JWT tenant_id claim</li>
 * </ol>
 * 
 * @see UserContext
 * @see TenantContext
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String TENANT_ID_HEADER = "X-Tenant-ID";
  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    // Add correlation ID to MDC for distributed tracing
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId != null) {
      org.slf4j.MDC.put("correlationId", correlationId);
    }

    try {
      // Extract user context from JWT if available
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
        Jwt jwt = jwtAuthToken.getToken();
        UserContext userContext = extractUserContext(jwt);

        // Set tenant context with priority: X-Tenant-ID header > JWT claim
        String tenantId = extractTenantId(request, userContext);
        if (tenantId != null) {
          TenantContext.setCurrentTenantId(tenantId);
        }

        // Add user context to MDC for structured logging
        if (userContext.userId() != null) {
          org.slf4j.MDC.put("userId", userContext.userId().toString());
        }
        if (userContext.username() != null) {
          org.slf4j.MDC.put("username", userContext.username());
        }

        // Store user context in request attribute for service layer access
        request.setAttribute("userContext", userContext);
      }

      filterChain.doFilter(request, response);
    } finally {
      // Clear context to prevent memory leaks in thread pool
      TenantContext.clear();
      org.slf4j.MDC.clear();
    }
  }

  /**
   * Extract tenant ID with priority: X-Tenant-ID header > JWT claim.
   *
   * @param request the HTTP request
   * @param userContext the user context extracted from JWT
   * @return the tenant ID or null if not found
   */
  private String extractTenantId(HttpServletRequest request, UserContext userContext) {
    // Priority 1: X-Tenant-ID header (from Gateway)
    String tenantId = request.getHeader(TENANT_ID_HEADER);
    if (StringUtils.hasText(tenantId)) {
      return tenantId.trim();
    }

    // Priority 2: JWT tenant_id claim
    if (userContext.tenantId() != null) {
      return userContext.tenantId();
    }

    return null;
  }

  /**
   * Extract user context from JWT claims.
   *
   * @param jwt the JWT token
   * @return the user context
   */
  private UserContext extractUserContext(Jwt jwt) {
    Long userId = extractLong(jwt.getClaim(JwtClaimNames.SUBJECT));
    String username = jwt.getClaim(JwtClaimNames.USERNAME);
    String email = jwt.getClaim(JwtClaimNames.EMAIL);
    Set<String> authorities = extractAuthorities(jwt.getClaim(JwtClaimNames.AUTHORITIES));
    String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);
    Long organizationId = extractLong(jwt.getClaim(JwtClaimNames.ORGANIZATION_ID));
    String firstName = jwt.getClaim(JwtClaimNames.FIRST_NAME);
    String lastName = jwt.getClaim(JwtClaimNames.LAST_NAME);

    return new UserContext(
        userId,
        username,
        email,
        authorities,
        tenantId,
        organizationId,
        firstName,
        lastName
    );
  }

  /**
   * Extract Long value from JWT claim, handling various numeric types.
   *
   * @param value the claim value
   * @return the Long value or null
   */
  private Long extractLong(Object value) {
    return switch (value) {
      case Long l -> l;
      case Integer i -> i.longValue();
      case String s -> {
        try {
          yield Long.parseLong(s);
        } catch (final NumberFormatException e) {
          yield null;
        }
      }
      case null, default -> null;
    };
  }

  /**
   * Extract authorities from JWT claim, handling List or Set types.
   *
   * @param value the claim value
   * @return the set of authorities
   */
  @SuppressWarnings("unchecked")
  private Set<String> extractAuthorities(Object value) {
    return switch (value) {
      case List<?> list -> new HashSet<>(list.stream().map(Object::toString).toList());
      case Set<?> set -> new HashSet<>(set.stream().map(Object::toString).toList());
      case null, default -> Collections.emptySet();
    };
  }
}
