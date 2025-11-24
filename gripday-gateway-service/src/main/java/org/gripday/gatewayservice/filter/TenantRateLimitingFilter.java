package org.gripday.gatewayservice.filter;

import java.time.Duration;
import java.time.Instant;

import org.gripday.gatewayservice.config.GripdayProperties;
import org.gripday.gatewayservice.exception.RateLimitExceededException;
import org.gripday.gatewayservice.service.TenantQuotaMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tenant-aware rate limiting filter with Redis-backed storage. Implements tenant-specific rate limiting policies and resource quotas per endpoint.
 */
@Component
public class TenantRateLimitingFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(TenantRateLimitingFilter.class);

  private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";

  private final GripdayProperties gripdayProperties;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final TenantQuotaMonitoringService quotaMonitoringService;

  public TenantRateLimitingFilter(final GripdayProperties gripdayProperties,
                                  final ReactiveStringRedisTemplate redisTemplate,
                                  final TenantQuotaMonitoringService quotaMonitoringService) {
    this.gripdayProperties = gripdayProperties;
    this.redisTemplate = redisTemplate;
    this.quotaMonitoringService = quotaMonitoringService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!gripdayProperties.gateway().rateLimiting().enabled()) {
      return chain.filter(exchange);
    }

    var request = exchange.getRequest();
    var path = request.getPath().value();
    var clientIp = getClientIp(request);

    // Get tenant context from exchange attributes
    var tenantContext = (TenantExtractionFilter.TenantContext)
        exchange.getAttributes().get(TENANT_CONTEXT_ATTRIBUTE);

    var tenantId = tenantContext != null ? tenantContext.tenantId() : "default";

    // Determine rate limiting policy for the endpoint
    var rateLimitPolicy = determineRateLimitPolicy(path);

    // Create rate limit keys
    var globalKey = createGlobalRateLimitKey(clientIp, path);
    var tenantKey = createTenantRateLimitKey(tenantId, path);

    logger.debug("Checking rate limits - Global key: {}, Tenant key: {}, Policy: {}",
        globalKey, tenantKey, rateLimitPolicy);

    // Check global rate limit first
    return checkRateLimit(globalKey, rateLimitPolicy.requestsPerMinute(), rateLimitPolicy.burstCapacity())
        .flatMap(globalAllowed -> {
          if (!globalAllowed) {
            logger.warn("Global rate limit exceeded for IP: {}, Path: {}", clientIp, path);
            return quotaMonitoringService.recordTenantRequest(tenantId, path, true)
                .then(Mono.error(new RateLimitExceededException(tenantId, path,
                    RateLimitExceededException.RateLimitType.GLOBAL, 60)));
          }

          // Check tenant-specific rate limit if enabled
          if (rateLimitPolicy.enableTenantQuotas() && gripdayProperties.gateway().rateLimiting().tenantQuotas().enabled()) {
            var tenantQuota = getTenantQuota(tenantId);
            return checkRateLimit(tenantKey, tenantQuota, tenantQuota * 2) // Allow burst of 2x quota
                .flatMap(tenantAllowed -> {
                  if (!tenantAllowed) {
                    logger.warn("Tenant rate limit exceeded for tenant: {}, Path: {}", tenantId, path);
                    return quotaMonitoringService.recordTenantRequest(tenantId, path, true)
                        .then(Mono.error(new RateLimitExceededException(tenantId, path,
                            RateLimitExceededException.RateLimitType.TENANT, 60)));
                  }
                  return quotaMonitoringService.recordTenantRequest(tenantId, path, false)
                      .then(chain.filter(exchange));
                });
          }

          return quotaMonitoringService.recordTenantRequest(tenantId, path, false)
              .then(chain.filter(exchange));
        })
        .onErrorResume(error -> {
          // Propagate RateLimitExceededException
          if (error instanceof RateLimitExceededException) {
            return Mono.error(error);
          }
          logger.error("Rate limiting error for path: {}, tenant: {}", path, tenantId, error);
          // Continue processing on Redis errors to avoid blocking requests
          return chain.filter(exchange);
        });
  }

  private String getClientIp(org.springframework.http.server.reactive.ServerHttpRequest request) {
    // Check X-Forwarded-For header first (for load balancers/proxies)
    var headerxForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
    if (StringUtils.hasText(headerxForwardedFor)) {
      return headerxForwardedFor.split(",")[0].trim();
    }

    // Check X-Real-IP header
    var headerxRealIp = request.getHeaders().getFirst("X-Real-IP");
    if (StringUtils.hasText(headerxRealIp)) {
      return headerxRealIp;
    }

    // Fall back to remote address
    var remoteAddress = request.getRemoteAddress();
    return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
  }

  private GripdayProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties determineRateLimitPolicy(String path) {
    var policies = gripdayProperties.gateway().rateLimiting().policies();

    // Check for exact path match first
    var exactMatch = policies.endpoints().get(path);
    if (exactMatch != null) {
      return exactMatch;
    }

    // Check for pattern matches
    for (final var entry : policies.endpoints().entrySet()) {
      var pattern = entry.getKey();
      if (pathMatches(path, pattern)) {
        return entry.getValue();
      }
    }

    // Return default policy
    return new GripdayProperties.GatewayProperties.RateLimitingProperties.PoliciesProperties.EndpointPolicyProperties(
        policies.defaultRequestsPerMinute(),
        policies.defaultBurstCapacity(),
        false
    );
  }

  private boolean pathMatches(String path, String pattern) {
    if (pattern.endsWith("/**")) {
      var prefix = pattern.substring(0, pattern.length() - 3);
      return path.startsWith(prefix);
    }
    return path.equals(pattern);
  }

  private String createGlobalRateLimitKey(String clientIp, String path) {
    var keyPrefix = gripdayProperties.gateway().rateLimiting().redis().keyPrefix();
    return String.format("%s:global:%s:%s", keyPrefix, clientIp, sanitizePath(path));
  }

  private String createTenantRateLimitKey(String tenantId, String path) {
    var keyPrefix = gripdayProperties.gateway().rateLimiting().redis().keyPrefix();
    return String.format("%s:tenant:%s:%s", keyPrefix, tenantId, sanitizePath(path));
  }

  private String sanitizePath(String path) {
    return path.replaceAll("[^a-zA-Z0-9/_-]", "_");
  }

  private int getTenantQuota(String tenantId) {
    var tenantQuotas = gripdayProperties.gateway().rateLimiting().tenantQuotas();
    return tenantQuotas.tenantSpecificQuotas().getOrDefault(tenantId, tenantQuotas.defaultTenantRequestsPerMinute());
  }

  private Mono<Boolean> checkRateLimit(String key, int requestsPerMinute, int burstCapacity) {
    var now = Instant.now();
    var windowStart = now.minusSeconds(60).getEpochSecond();

    // Use Redis sliding window log algorithm
    return redisTemplate.opsForZSet()
        // Remove old entries outside the time window
        .removeRangeByScore(key, org.springframework.data.domain.Range.closed(0.0, (double) windowStart))
        .then(redisTemplate.opsForZSet().count(key, org.springframework.data.domain.Range.closed((double) windowStart, (double) now.getEpochSecond())))
        .flatMap(currentCount -> {
          if (currentCount < requestsPerMinute) {
            // Add current request to the window
            return redisTemplate.opsForZSet()
                .add(key, now.toString(), now.getEpochSecond())
                .then(redisTemplate.expire(key, Duration.ofMinutes(2))) // Cleanup after 2 minutes
                .thenReturn(true);
          } else if (currentCount < burstCapacity) {
            // Allow burst but don't add to sliding window
            logger.debug("Allowing burst request for key: {}, count: {}/{}", key, currentCount, burstCapacity);
            return Mono.just(true);
          } else {
            // Rate limit exceeded
            return Mono.just(false);
          }
        });
  }

  @Override
  public int getOrder() {
    return -50; // Execute after authentication but before routing
  }
}
