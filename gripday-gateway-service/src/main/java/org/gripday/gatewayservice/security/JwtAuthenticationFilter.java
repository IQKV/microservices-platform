package org.gripday.gatewayservice.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.gripday.gatewayservice.common.GatewayConstants;
import org.gripday.gatewayservice.config.GripdayProperties;
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

  private final GripdayProperties gripdayProperties;

  public JwtAuthenticationFilter(final GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    var path = request.getPath().value();

    // Generate correlation ID if not present
    var correlationId = getOrGenerateCorrelationId(request);
    MDC.put(GatewayConstants.MdcKeys.CORRELATION_ID, correlationId);

    // Skip authentication for public paths
    if (isPublicPath(path)) {
      logger.debug("Skipping authentication for public path: {}", path);
      return addCorrelationIdAndContinue(exchange, chain, correlationId);
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
            var tenantContext = extractTenantContext(request, jwt);

            // Add tenant context to MDC for logging
            if (StringUtils.hasText(tenantContext.tenantId())) {
              MDC.put(GatewayConstants.MdcKeys.TENANT_ID, tenantContext.tenantId());
            }

            logger.debug("Authenticated user: {} for tenant: {}", userContext.username(), tenantContext.tenantId());

            // Propagate user and tenant context to downstream services
            var modifiedRequest = propagateContextHeaders(request, userContext, tenantContext, correlationId);
            var modifiedExchange = exchange.mutate().request(modifiedRequest).build();

            return chain.filter(modifiedExchange);
          } finally {
            // Clean up MDC
            MDC.remove(GatewayConstants.MdcKeys.CORRELATION_ID);
            MDC.remove(GatewayConstants.MdcKeys.TENANT_ID);
          }
        })
        .switchIfEmpty(addCorrelationIdAndContinue(exchange, chain, correlationId));
  }

  private String getOrGenerateCorrelationId(ServerHttpRequest request) {
    var existingCorrelationId = request.getHeaders().getFirst(GatewayConstants.Headers.X_CORRELATION_ID);
    return StringUtils.hasText(existingCorrelationId) ? existingCorrelationId : UUID.randomUUID().toString();
  }

  private boolean isPublicPath(String path) {
    return gripdayProperties.gateway().security().publicPaths().stream()
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
    var roles = extractStringList(claims.get(JwtClaimNames.ROLES));
    var permissions = extractStringList(claims.get(JwtClaimNames.PERMISSIONS));
    var department = extractString(claims.get(JwtClaimNames.DEPARTMENT));
    var organizationId = extractString(claims.get(JwtClaimNames.ORGANIZATION_ID));

    return new UserContext(
        userId,
        username,
        email,
        roles,
        permissions,
        department,
        organizationId
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
    if (gripdayProperties.gateway().security().authentication().enableUserContextPropagation()) {
      if (userContext.userId() != null) {
        builder.header(GatewayConstants.Headers.X_USER_ID, userContext.userId().toString());
      }
      if (StringUtils.hasText(userContext.username())) {
        builder.header(GatewayConstants.Headers.X_USERNAME, userContext.username());
      }
      if (!userContext.roles().isEmpty()) {
        builder.header(GatewayConstants.Headers.X_USER_ROLES, String.join(",", userContext.roles()));
      }

      logger.debug("User context propagated for user: {}", userContext.username());
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
      String department,
      String organizationId
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
