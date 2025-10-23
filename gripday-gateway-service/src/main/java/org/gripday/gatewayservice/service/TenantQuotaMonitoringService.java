package org.gripday.gatewayservice.service;

import org.gripday.gatewayservice.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for monitoring tenant quota usage and alerting.
 * Tracks tenant usage patterns and provides quota monitoring capabilities.
 */
@Service
public class TenantQuotaMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(TenantQuotaMonitoringService.class);
    
    private final GatewayProperties gatewayProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final Map<String, TenantUsageStats> tenantUsageCache = new ConcurrentHashMap<>();

    public TenantQuotaMonitoringService(GatewayProperties gatewayProperties, ReactiveStringRedisTemplate redisTemplate) {
        this.gatewayProperties = gatewayProperties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Records a request for tenant quota monitoring.
     */
    public Mono<Void> recordTenantRequest(String tenantId, String endpoint, boolean rateLimited) {
        if (!gatewayProperties.rateLimiting().tenantQuotas().enabled()) {
            return Mono.empty();
        }
        
        return Mono.fromRunnable(() -> {
            var stats = tenantUsageCache.computeIfAbsent(tenantId, k -> new TenantUsageStats(tenantId));
            stats.recordRequest(endpoint, rateLimited);
            
            // Log usage patterns for monitoring
            if (stats.getTotalRequests() % 100 == 0) { // Log every 100 requests
                logger.info("Tenant usage - ID: {}, Total: {}, Rate Limited: {}, Endpoints: {}", 
                    tenantId, stats.getTotalRequests(), stats.getRateLimitedRequests(), stats.getEndpointCounts().size());
            }
            
            // Alert on high rate limiting
            if (stats.getRateLimitedRequests() > 0 && 
                (double) stats.getRateLimitedRequests() / stats.getTotalRequests() > 0.1) { // 10% rate limited
                logger.warn("High rate limiting detected for tenant: {} - {}% of requests rate limited", 
                    tenantId, (double) stats.getRateLimitedRequests() / stats.getTotalRequests() * 100);
            }
        });
    }

    /**
     * Gets current usage statistics for a tenant.
     */
    public Mono<TenantUsageStats> getTenantUsageStats(String tenantId) {
        return Mono.fromSupplier(() -> tenantUsageCache.get(tenantId))
            .switchIfEmpty(Mono.just(new TenantUsageStats(tenantId)));
    }

    /**
     * Gets usage statistics for all tenants.
     */
    public Mono<Map<String, TenantUsageStats>> getAllTenantUsageStats() {
        return Mono.just(Map.copyOf(tenantUsageCache));
    }

    /**
     * Resets usage statistics for a tenant.
     */
    public Mono<Void> resetTenantUsageStats(String tenantId) {
        return Mono.fromRunnable(() -> {
            tenantUsageCache.remove(tenantId);
            logger.info("Reset usage statistics for tenant: {}", tenantId);
        });
    }

    /**
     * Checks if a tenant is approaching their quota limit.
     */
    public Mono<Boolean> isTenantApproachingQuota(String tenantId) {
        var quota = getTenantQuota(tenantId);
        
        return getCurrentTenantUsage(tenantId)
            .map(currentUsage -> {
                var usagePercentage = (double) currentUsage / quota;
                return usagePercentage > 0.8; // 80% threshold
            })
            .defaultIfEmpty(false);
    }

    private Mono<Long> getCurrentTenantUsage(String tenantId) {
        var keyPrefix = gatewayProperties.rateLimiting().redis().keyPrefix();
        var tenantKey = String.format("%s:tenant:%s:*", keyPrefix, tenantId);
        
        // Get current usage from Redis sliding window
        var now = Instant.now();
        var windowStart = now.minusSeconds(60).getEpochSecond();
        
        return redisTemplate.keys(tenantKey)
            .flatMap(key -> redisTemplate.opsForZSet().count(key, org.springframework.data.domain.Range.closed((double) windowStart, (double) now.getEpochSecond())))
            .reduce(0L, (a, b) -> a + b);
    }

    private int getTenantQuota(String tenantId) {
        var tenantQuotas = gatewayProperties.rateLimiting().tenantQuotas();
        return tenantQuotas.tenantSpecificQuotas().getOrDefault(tenantId, tenantQuotas.defaultTenantRequestsPerMinute());
    }

    /**
     * Tenant usage statistics tracking.
     */
    public static class TenantUsageStats {
        private final String tenantId;
        private final Instant startTime;
        private long totalRequests;
        private long rateLimitedRequests;
        private final Map<String, Long> endpointCounts = new ConcurrentHashMap<>();

        public TenantUsageStats(String tenantId) {
            this.tenantId = tenantId;
            this.startTime = Instant.now();
        }

        public void recordRequest(String endpoint, boolean rateLimited) {
            totalRequests++;
            if (rateLimited) {
                rateLimitedRequests++;
            }
            endpointCounts.merge(endpoint, 1L, Long::sum);
        }

        // Getters
        public String getTenantId() { return tenantId; }
        public Instant getStartTime() { return startTime; }
        public long getTotalRequests() { return totalRequests; }
        public long getRateLimitedRequests() { return rateLimitedRequests; }
        public Map<String, Long> getEndpointCounts() { return Map.copyOf(endpointCounts); }
        
        public double getRateLimitedPercentage() {
            return totalRequests > 0 ? (double) rateLimitedRequests / totalRequests * 100 : 0.0;
        }
        
        public Duration getUptime() {
            return Duration.between(startTime, Instant.now());
        }
    }
}