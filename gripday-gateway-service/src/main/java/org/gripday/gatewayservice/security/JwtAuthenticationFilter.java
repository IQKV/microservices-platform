package org.gripday.gatewayservice.security;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.gripday.gatewayservice.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive JWT authentication filter for token validation with tenant extraction. Handles JWT token validation, user context extraction, and tenant context establishment.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String X_TENANT_ID_HEADER = "X-Tenant-ID";
  private static final String X_USER_ID_HEADER = "X-User-ID";
  private static final String X_USERNAME_HEADER = "X-Username";
  private static final String X_USER_ROLES_HEADER = "X-User-Roles";
  private static final String X_CORRELATION_ID_HEADER = "X-Correlation-ID";

  private final GatewayProperties gatewayProperties;
  private final SecretKey jwtSecretKey;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(GatewayProperties gatewayProperties, ObjectMapper objectMapper) {
    this.gatewayProperties = gatewayProperties;
    this.objectMapper = objectMapper;
    var secretKeyBytes = gatewayProperties.security().jwt().secretKey().getBytes(StandardCharsets.UTF_8);
    this.jwtSecretKey = Keys.hmacShaKeyFor(secretKeyBytes);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    var path = request.getPath().value();

    // Generate correlation ID if not present
    var correlationId = getOrGenerateCorrelationId(request);
    MDC.put("correlationId", correlationId);

    // Skip authentication for public paths
    if (isPublicPath(path)) {
      logger.debug("Skipping authentication for public path: {}", path);
      return addCorrelationIdAndContinue(exchange, chain, correlationId);
    }

    // Extract JWT token
    var token = extractToken(request);
    if (!StringUtils.hasText(token)) {
      logger.warn("Missing JWT token for protected path: {}", path);
      return handleAuthenticationError(exchange, "Missing authentication token");
    }

    try {
      // Validate and parse JWT token
      var claims = validateAndParseToken(token);

      // Extract user context from JWT claims
      var userContext = extractUserContext(claims);

      // Extract tenant context
      var tenantContext = extractTenantContext(request, claims);

      // Add tenant context to MDC for logging
      if (StringUtils.hasText(tenantContext.tenantId())) {
        MDC.put("tenantId", tenantContext.tenantId());
      }

      logger.debug("Authenticated user: {} for tenant: {}", userContext.username(), tenantContext.tenantId());

      // Propagate user and tenant context to downstream services
      var modifiedRequest = propagateContextHeaders(request, userContext, tenantContext, correlationId);
      var modifiedExchange = exchange.mutate().request(modifiedRequest).build();

      return chain.filter(modifiedExchange);

    } catch (Exception e) {
      logger.error("JWT authentication failed for path: {}", path, e);
      return handleAuthenticationError(exchange, "Invalid authentication token");
    } finally {
      // Clean up MDC
      MDC.remove("correlationId");
      MDC.remove("tenantId");
    }
  }

  private String getOrGenerateCorrelationId(ServerHttpRequest request) {
    var existingCorrelationId = request.getHeaders().getFirst(X_CORRELATION_ID_HEADER);
    return StringUtils.hasText(existingCorrelationId) ? existingCorrelationId : UUID.randomUUID().toString();
  }

  private boolean isPublicPath(String path) {
    return gatewayProperties.security().publicPaths().stream()
        .anyMatch(publicPath -> {
          if (publicPath.endsWith("/**")) {
            var prefix = publicPath.substring(0, publicPath.length() - 3);
            return path.startsWith(prefix);
          }
          return path.equals(publicPath);
        });
  }

  private String extractToken(ServerHttpRequest request) {
    var authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
      return authHeader.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  private Claims validateAndParseToken(String token) {
    return Jwts.parser()
        .verifyWith(jwtSecretKey)
        .requireIssuer(gatewayProperties.security().jwt().issuer())
        .requireAudience(gatewayProperties.security().jwt().audience())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private UserContext extractUserContext(Claims claims) {
    var userId = claims.get("userId", Long.class);
    var username = claims.getSubject();
    var email = claims.get("email", String.class);
    var roles = claims.get("roles", List.class);
    var permissions = claims.get("permissions", List.class);
    var department = claims.get("department", String.class);
    var organizationId = claims.get("organizationId", String.class);

    return new UserContext(
        userId,
        username,
        email,
        roles != null ? roles : List.of(),
        permissions != null ? permissions : List.of(),
        department,
        organizationId
    );
  }

  private TenantContext extractTenantContext(ServerHttpRequest request, Claims claims) {
    // Priority: 1. X-Tenant-ID header, 2. JWT claims, 3. Subdomain extraction
    var tenantId = request.getHeaders().getFirst(X_TENANT_ID_HEADER);

    if (!StringUtils.hasText(tenantId)) {
      tenantId = claims.get("tenantId", String.class);
    }

    if (!StringUtils.hasText(tenantId)) {
      tenantId = extractTenantFromSubdomain(request);
    }

    return new TenantContext(tenantId);
  }

  private String extractTenantFromSubdomain(ServerHttpRequest request) {
    var host = request.getHeaders().getFirst(HttpHeaders.HOST);
    if (StringUtils.hasText(host)) {
      var parts = host.split("\\.");
      if (parts.length > 2) {
        // Extract subdomain as tenant ID (e.g., tenant1.api.gripday.com -> tenant1)
        return parts[0];
      }
    }
    return null;
  }

  private ServerHttpRequest propagateContextHeaders(
      ServerHttpRequest request,
      UserContext userContext,
      TenantContext tenantContext,
      String correlationId) {

    var builder = request.mutate();

    // Add correlation ID
    builder.header(X_CORRELATION_ID_HEADER, correlationId);

    // Add user context headers
    if (userContext.userId() != null) {
      builder.header(X_USER_ID_HEADER, userContext.userId().toString());
    }
    if (StringUtils.hasText(userContext.username())) {
      builder.header(X_USERNAME_HEADER, userContext.username());
    }
    if (!userContext.roles().isEmpty()) {
      builder.header(X_USER_ROLES_HEADER, String.join(",", userContext.roles()));
    }

    // Add tenant context headers
    if (StringUtils.hasText(tenantContext.tenantId())) {
      builder.header(X_TENANT_ID_HEADER, tenantContext.tenantId());
    }

    return builder.build();
  }

  private Mono<Void> addCorrelationIdAndContinue(ServerWebExchange exchange, GatewayFilterChain chain, String correlationId) {
    var modifiedRequest = exchange.getRequest().mutate()
        .header(X_CORRELATION_ID_HEADER, correlationId)
        .build();
    var modifiedExchange = exchange.mutate().request(modifiedRequest).build();
    return chain.filter(modifiedExchange);
  }

  private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, String message) {
    var response = exchange.getResponse();
    var request = exchange.getRequest();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    var correlationId = MDC.get("correlationId");

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
    pd.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
    pd.setType(URI.create("/problems/auth_token_invalid"));
    pd.setInstance(URI.create(request.getPath().value()));
    pd.setProperty("code", "AUTH_TOKEN_INVALID");
    pd.setProperty("timestamp", Instant.now().toString());
    if (correlationId != null) {
      pd.setProperty("correlationId", correlationId);
    }

    try {
      byte[] body = objectMapper.writeValueAsBytes(pd);
      var buffer = response.bufferFactory().wrap(body);
      return response.writeWith(Mono.just(buffer));
    } catch (Exception e) {
      var fallback = ("{\n  \"type\": \"" + pd.getType() + "\",\n" +
          "  \"title\": \"" + pd.getTitle() + "\",\n" +
          "  \"status\": " + pd.getStatus() + ",\n" +
          "  \"detail\": \"" + message + "\",\n" +
          "  \"instance\": \"" + request.getPath().value() + "\"\n}")
          .getBytes(StandardCharsets.UTF_8);
      var buffer = response.bufferFactory().wrap(fallback);
      return response.writeWith(Mono.just(buffer));
    }
  }

  @Override
  public int getOrder() {
    return -100; // Execute before other filters
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
   * Tenant context extracted from headers, JWT claims, or subdomain.
   */
  public record TenantContext(
      String tenantId
  ) {

  }
}