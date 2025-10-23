package org.gripday.gatewayservice.filter;

import org.gripday.gatewayservice.config.GatewayProperties;
import org.gripday.gatewayservice.service.TenantQuotaMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Tenant-aware rate limiting filter with Redis-backed storage.
 * Implements tenant-specific rate limiting policies and resource quotas per endpoint.
 */
@Component
public class TenantRateLimitingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(TenantRateLimitingFilter.class);
    
    private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";
    
    private final GatewayProperties gatewayProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final TenantQuotaMonitoringService quotaMonitoringService;

    public TenantRateLimitingFilter(GatewayProperties gatewayProperties, 
                                   ReactiveStringRedisTemplate redisTemplate,
                                   TenantQuotaMonitoringService quotaMonitoringService) {
        this.gatewayProperties = gatewayProperties;
        this.redisTemplate = redisTemplate;
        this.quotaMonitoringService = quotaMonitoringService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayProperties.rateLimiting().enabled()) {
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
                        .then(handleRateLimitExceeded(exchange, "Global rate limit exceeded", tenantId));
                }
                
                // Check tenant-specific rate limit if enabled
                if (rateLimitPolicy.enableTenantQuotas() && gatewayProperties.rateLimiting().tenantQuotas().enabled()) {
                    var tenantQuota = getTenantQuota(tenantId);
                    return checkRateLimit(tenantKey, tenantQuota, tenantQuota * 2) // Allow burst of 2x quota
                        .flatMap(tenantAllowed -> {
                            if (!tenantAllowed) {
                                logger.warn("Tenant rate limit exceeded for tenant: {}, Path: {}", tenantId, path);
                                return quotaMonitoringService.recordTenantRequest(tenantId, path, true)
                                    .then(handleRateLimitExceeded(exchange, "Tenant rate limit exceeded", tenantId));
                            }
                            return quotaMonitoringService.recordTenantRequest(tenantId, path, false)
                                .then(chain.filter(exchange));
                        });
                }
                
                return quotaMonitoringService.recordTenantRequest(tenantId, path, false)
                    .then(chain.filter(exchange));
            })
            .onErrorResume(error -> {
                logger.error("Rate limiting error for path: {}, tenant: {}", path, tenantId, error);
                // Continue processing on Redis errors to avoid blocking requests
                return chain.filter(exchange);
            });
    }

    private String getClientIp(org.springframework.http.server.reactive.ServerHttpRequest request) {
        // Check X-Forwarded-For header first (for load balancers/proxies)
        var xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        var xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        // Fall back to remote address
        var remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    private GatewayProperties.RateLimiting.EndpointPolicy determineRateLimitPolicy(String path) {
        var policies = gatewayProperties.rateLimiting().policies();
        
        // Check for exact path match first
        var exactMatch = policies.endpoints().get(path);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Check for pattern matches
        for (var entry : policies.endpoints().entrySet()) {
            var pattern = entry.getKey();
            if (pathMatches(path, pattern)) {
                return entry.getValue();
            }
        }
        
        // Return default policy
        return new GatewayProperties.RateLimiting.EndpointPolicy(
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
        var keyPrefix = gatewayProperties.rateLimiting().redis().keyPrefix();
        return String.format("%s:global:%s:%s", keyPrefix, clientIp, sanitizePath(path));
    }

    private String createTenantRateLimitKey(String tenantId, String path) {
        var keyPrefix = gatewayProperties.rateLimiting().redis().keyPrefix();
        return String.format("%s:tenant:%s:%s", keyPrefix, tenantId, sanitizePath(path));
    }

    private String sanitizePath(String path) {
        return path.replaceAll("[^a-zA-Z0-9/_-]", "_");
    }

    private int getTenantQuota(String tenantId) {
        var tenantQuotas = gatewayProperties.rateLimiting().tenantQuotas();
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

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, String message, String tenantId) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        // Add rate limit headers
        response.getHeaders().add("X-RateLimit-Limit", "60");
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()));
        
        var correlationId = MDC.get("correlationId");
        var errorResponse = createRateLimitErrorResponse(message, exchange.getRequest().getPath().value(), 
            correlationId, tenantId);
        
        var buffer = response.bufferFactory().wrap(errorResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String createRateLimitErrorResponse(String message, String path, String correlationId, String tenantId) {
        var errorResponseBuilder = new StringBuilder();
        errorResponseBuilder.append("{\n");
        errorResponseBuilder.append("  \"error\": {\n");
        errorResponseBuilder.append("    \"code\": \"RATE_LIMIT_EXCEEDED\",\n");
        errorResponseBuilder.append("    \"message\": \"").append(message).append("\",\n");
        errorResponseBuilder.append("    \"details\": \"Request rate limit exceeded. Please try again later.\",\n");
        errorResponseBuilder.append("    \"timestamp\": \"").append(Instant.now()).append("\",\n");
        errorResponseBuilder.append("    \"path\": \"").append(path).append("\"");
        
        if (correlationId != null) {
            errorResponseBuilder.append(",\n    \"correlationId\": \"").append(correlationId).append("\"");
        }
        
        if (tenantId != null) {
            errorResponseBuilder.append(",\n    \"tenantId\": \"").append(tenantId).append("\"");
        }
        
        errorResponseBuilder.append(",\n    \"retryAfter\": 60");
        errorResponseBuilder.append("\n  }\n}");
        
        return errorResponseBuilder.toString();
    }

    @Override
    public int getOrder() {
        return -50; // Execute after authentication but before routing
    }
}