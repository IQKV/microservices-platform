package com.iqscaffold.gatewayservice.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive JWT authentication filter for user context propagation.
 * Extracts user context from validated JWT and propagates via headers to downstream services.
 * JWT validation is handled by Spring Security OAuth2 Resource Server with RSA256.
 */
@Component
public final class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final IqScaffoldProperties properties;

  public JwtAuthenticationFilter(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var originalRequest = exchange.getRequest();
    var path = originalRequest.getPath().value();

    // Sanitize incoming headers to prevent spoofing
    var sanitizedRequest = sanitizeIncomingHeaders(originalRequest);

    // Generate correlation ID if not present
    var correlationId = getOrGenerateCorrelationId(sanitizedRequest);
    MDC.put(GatewayConstants.MdcKeys.CORRELATION_ID, correlationId);

    // Skip authentication for public paths
    if (isPublicPath(path)) {
      logger.debug("Skipping authentication for public path: {}", path);
      return addCorrelationIdAndContinue(exchange.mutate().request(sanitizedRequest).build(), chain, correlationId);
    }

    // Extract user context from authenticated JWT
    return ReactiveSecurityContextHolder.getContext()
        .map(securityContext -> securityContext.getAuthentication())
        .filter(auth -> auth instanceof JwtAuthenticationToken)
        .map(auth -> (JwtAuthenticationToken) auth)
        .map(jwtAuth -> jwtAuth.getToken())
        .flatMap(jwt -> {
          try {
            // Extract user context from JWT
            var userContext = extractUserContext(jwt);

            // Extract tenant context
            var tenantContext = extractTenantContext(sanitizedRequest, jwt);

            // Add tenant context to MDC for logging
            if (StringUtils.hasText(tenantContext.tenantId())) {
              MDC.put(GatewayConstants.MdcKeys.TENANT_ID, tenantContext.tenantId());
            }

            logger.debug("Authenticated user: {} for tenant: {}", userContext.username(), tenantContext.tenantId());

            // Propagate user and tenant context to downstream services
            var modifiedRequest = propagateContextHeaders(sanitizedRequest, userContext, tenantContext, correlationId);
            var modifiedExchange = exchange.mutate().request(modifiedRequest).build();

            return chain.filter(modifiedExchange);
          } finally {
            // Clean up MDC
            MDC.remove(GatewayConstants.MdcKeys.CORRELATION_ID);
            MDC.remove(GatewayConstants.MdcKeys.TENANT_ID);
          }
        })
        .switchIfEmpty(addCorrelationIdAndContinue(exchange.mutate().request(sanitizedRequest).build(), chain, correlationId));
  }

  private String getOrGenerateCorrelationId(ServerHttpRequest request) {
    var existingCorrelationId = request.getHeaders().getFirst(GatewayConstants.Headers.X_CORRELATION_ID);
    return StringUtils.hasText(existingCorrelationId) ? existingCorrelationId : UUID.randomUUID().toString();
  }

  /**
   * Sanitize incoming headers to prevent header spoofing attacks.
   * Removes any user/tenant context headers that may have been set by external clients.
   * Only the gateway should set these headers after JWT validation.
   */
  private ServerHttpRequest sanitizeIncomingHeaders(ServerHttpRequest request) {
    return request.mutate()
        .headers(headers -> {
          // Remove user context headers (will be set by gateway after JWT validation)
          headers.remove(GatewayConstants.Headers.X_USER_ID);
          headers.remove(GatewayConstants.Headers.X_USERNAME);
          headers.remove(GatewayConstants.Headers.X_USER_EMAIL);
          headers.remove(GatewayConstants.Headers.X_USER_AUTHORITIES);
          headers.remove(GatewayConstants.Headers.X_USER_PERMISSIONS);
          headers.remove(GatewayConstants.Headers.X_USER_ROLES);
          headers.remove(GatewayConstants.Headers.X_ORGANIZATION_ID);

          // Remove tenant context headers (will be set by gateway after validation)
          headers.remove(GatewayConstants.Headers.X_TENANT_ID);

          // Remove internal headers that should never come from external requests
          headers.remove(GatewayConstants.Headers.X_INTERNAL_SERVICE);
          headers.remove(GatewayConstants.Headers.X_INTERNAL_VERSION);
          headers.remove(GatewayConstants.Headers.X_INTERNAL_TOKEN);
          headers.remove(GatewayConstants.Headers.AUTHORIZATION_INTERNAL);

          logger.trace("Sanitized incoming request headers");
        })
        .build();
  }

  private boolean isPublicPath(String path) {
    return properties.gateway().security().publicPaths().stream()
        .anyMatch(publicPath -> {
          if (publicPath.endsWith("/**")) {
            var prefix = publicPath.substring(0, publicPath.length() - 3);
            return path.startsWith(prefix);
          }
          return path.equals(publicPath);
        });
  }

  private UserContext extractUserContext(Jwt jwt) {
    var claims = jwt.getClaims();

    var userId = extractLong(claims.get(JwtClaimNames.USER_ID));
    var username = jwt.getSubject();
    var email = extractString(claims.get(JwtClaimNames.EMAIL));
    var roles = extractStringList(claims.get(JwtClaimNames.AUTHORITIES));
    var permissions = extractStringList(claims.get(JwtClaimNames.PERMISSIONS));
    var organizationId = extractString(claims.get(JwtClaimNames.ORGANIZATION_ID));
    var preferredLocale = extractString(claims.get(JwtClaimNames.PREFERRED_LOCALE));

    return new UserContext(
        userId,
        username,
        email,
        roles,
        permissions,
        organizationId,
        preferredLocale
    );
  }

  private TenantContext extractTenantContext(ServerHttpRequest request, Jwt jwt) {
    // Priority: 1. JWT claims, 2. X-Tenant-ID header
    var tenantId = extractString(jwt.getClaims().get(JwtClaimNames.TENANT_ID));

    if (!StringUtils.hasText(tenantId)) {
      tenantId = request.getHeaders().getFirst(GatewayConstants.Headers.X_TENANT_ID);
    }

    return new TenantContext(tenantId);
  }

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

  private String extractString(Object value) {
    return value instanceof String s ? s : null;
  }

  @SuppressWarnings("unchecked")
  private List<String> extractStringList(Object value) {
    return switch (value) {
      case List<?> list -> list.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .collect(Collectors.toList());
      case Set<?> set -> set.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .collect(Collectors.toList());
      case null, default -> List.of();
    };
  }


  private ServerHttpRequest propagateContextHeaders(
      ServerHttpRequest request,
      UserContext userContext,
      TenantContext tenantContext,
      String correlationId) {

    var builder = request.mutate();

    // Add correlation ID
    builder.header(GatewayConstants.Headers.X_CORRELATION_ID, correlationId);

    // Add user context headers only if propagation is enabled
    if (properties.gateway().security().authentication().enableUserContextPropagation()) {
      // User ID
      if (userContext.userId() != null) {
        builder.header(GatewayConstants.Headers.X_USER_ID, userContext.userId().toString());
      }

      // Username
      if (StringUtils.hasText(userContext.username())) {
        builder.header(GatewayConstants.Headers.X_USERNAME, userContext.username());
      }

      // Email
      if (StringUtils.hasText(userContext.email())) {
        builder.header(GatewayConstants.Headers.X_USER_EMAIL, userContext.email());
      }

      // Authorities (for authorization checks - CRITICAL for @PreAuthorize)
      if (!userContext.roles().isEmpty()) {
        var authoritiesStr = String.join(",", userContext.roles());
        builder.header(GatewayConstants.Headers.X_USER_AUTHORITIES, authoritiesStr);
        // Also set X-User-Roles for backward compatibility
        builder.header(GatewayConstants.Headers.X_USER_ROLES, authoritiesStr);
      }

      // Permissions (for fine-grained access control)
      if (!userContext.permissions().isEmpty()) {
        builder.header(GatewayConstants.Headers.X_USER_PERMISSIONS, String.join(",", userContext.permissions()));
      }

      // Organization ID
      if (StringUtils.hasText(userContext.organizationId())) {
        builder.header(GatewayConstants.Headers.X_ORGANIZATION_ID, userContext.organizationId());
      }

      // Locale
      if (StringUtils.hasText(userContext.preferredLocale())) {
        builder.header(GatewayConstants.Headers.X_USER_LOCALE, userContext.preferredLocale());
      }

      logger.debug("User context propagated for user: {} with authorities: {} and locale: {}",
          userContext.username(), userContext.roles(), userContext.preferredLocale());
    } else {
      logger.debug("User context propagation is disabled");
    }

    // Add tenant context headers
    if (StringUtils.hasText(tenantContext.tenantId())) {
      builder.header(GatewayConstants.Headers.X_TENANT_ID, tenantContext.tenantId());
    }

    return builder.build();
  }

  private Mono<Void> addCorrelationIdAndContinue(ServerWebExchange exchange, GatewayFilterChain chain, String correlationId) {
    var modifiedRequest = exchange.getRequest().mutate()
        .header(GatewayConstants.Headers.X_CORRELATION_ID, correlationId)
        .build();
    var modifiedExchange = exchange.mutate().request(modifiedRequest).build();
    return chain.filter(modifiedExchange);
  }

  @Override
  public int getOrder() {
    return GatewayConstants.FilterOrder.JWT_AUTHENTICATION_FILTER;
  }

  /**
   * User context extracted from JWT token.
   */
  public record UserContext(
      Long userId,
      String username,
      String email,
      List<String> roles,
      List<String> permissions,
      String organizationId,
      String preferredLocale
  ) {

  }

  /**
   * Tenant context extracted from JWT claims or headers.
   */
  public record TenantContext(
      String tenantId
  ) {

  }
}
