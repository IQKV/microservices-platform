package org.gripday.bookstore.domain.service;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Service
public class QueryPerformanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryPerformanceService.class);
    
    private final EntityManagerFactory entityManagerFactory;
    private volatile Map<String, Object> lastPerformanceMetrics = new HashMap<>();
    
    public QueryPerformanceService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    @Async("performanceMonitoringExecutor")
    public void collectPerformanceMetrics() {
        try {
            var sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            var statistics = sessionFactory.getStatistics();
            
            if (statistics.isStatisticsEnabled()) {
                var metrics = buildPerformanceMetrics(statistics);
                lastPerformanceMetrics = metrics;
                
                // Log performance warnings
                checkPerformanceThresholds(metrics);
            }
        } catch (Exception e) {
            logger.warn("Failed to collect performance metrics", e);
        }
    }
    
    private Map<String, Object> buildPerformanceMetrics(Statistics statistics) {
        var metrics = new HashMap<String, Object>();
        
        // Query statistics
        metrics.put("queryExecutionCount", statistics.getQueryExecutionCount());
        metrics.put("queryExecutionMaxTime", statistics.getQueryExecutionMaxTime());
        metrics.put("queryExecutionMaxTimeQueryString", statistics.getQueryExecutionMaxTimeQueryString());
        metrics.put("queryCacheHitCount", statistics.getQueryCacheHitCount());
        metrics.put("queryCacheMissCount", statistics.getQueryCacheMissCount());
        
        // Second level cache statistics
        metrics.put("secondLevelCacheHitCount", statistics.getSecondLevelCacheHitCount());
        metrics.put("secondLevelCacheMissCount", statistics.getSecondLevelCacheMissCount());
        
        // Connection statistics
        metrics.put("connectCount", statistics.getConnectCount());
        metrics.put("flushCount", statistics.getFlushCount());
        
        // Transaction statistics
        metrics.put("transactionCount", statistics.getTransactionCount());
        metrics.put("successfulTransactionCount", statistics.getSuccessfulTransactionCount());
        
        // Calculate hit ratios
        var queryCacheHitRatio = calculateHitRatio(
            statistics.getQueryCacheHitCount(), 
            statistics.getQueryCacheMissCount()
        );
        metrics.put("queryCacheHitRatio", queryCacheHitRatio);
        
        var secondLevelCacheHitRatio = calculateHitRatio(
            statistics.getSecondLevelCacheHitCount(), 
            statistics.getSecondLevelCacheMissCount()
        );
        metrics.put("secondLevelCacheHitRatio", secondLevelCacheHitRatio);
        
        return metrics;
    }
    
    private double calculateHitRatio(long hits, long misses) {
        var total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    private void checkPerformanceThresholds(Map<String, Object> metrics) {
        // Check for slow queries
        var maxQueryTime = (Long) metrics.get("queryExecutionMaxTime");
        if (maxQueryTime != null && maxQueryTime > 1000) { // 1 second threshold
            var slowQuery = (String) metrics.get("queryExecutionMaxTimeQueryString");
            logger.warn("Slow query detected: {}ms - {}", maxQueryTime, slowQuery);
        }
        
        // Check cache hit ratios
        var queryCacheHitRatio = (Double) metrics.get("queryCacheHitRatio");
        if (queryCacheHitRatio != null && queryCacheHitRatio < 0.8) { // 80% threshold
            logger.warn("Low query cache hit ratio: {:.2f}%", queryCacheHitRatio * 100);
        }
        
        var secondLevelCacheHitRatio = (Double) metrics.get("secondLevelCacheHitRatio");
        if (secondLevelCacheHitRatio != null && secondLevelCacheHitRatio < 0.7) { // 70% threshold
            logger.warn("Low second level cache hit ratio: {:.2f}%", secondLevelCacheHitRatio * 100);
        }
    }
    
    public Map<String, Object> getPerformanceMetrics() {
        return new HashMap<>(lastPerformanceMetrics);
    }
    
    public void resetStatistics() {
        try {
            var sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            var statistics = sessionFactory.getStatistics();
            statistics.clear();
            logger.info("Hibernate statistics reset");
        } catch (Exception e) {
            logger.error("Failed to reset statistics", e);
        }
    }
    
    public Map<String, String> getHealthStatus() {
        var status = new HashMap<String, String>();
        
        try {
            var metrics = getPerformanceMetrics();
            
            if (!metrics.isEmpty()) {
                var queryCacheHitRatio = (Double) metrics.get("queryCacheHitRatio");
                var maxQueryTime = (Long) metrics.get("queryExecutionMaxTime");
                
                status.put("queryCacheHitRatio", 
                    queryCacheHitRatio != null ? String.format("%.2f%%", queryCacheHitRatio * 100) : "N/A");
                status.put("maxQueryTime", 
                    maxQueryTime != null ? maxQueryTime + "ms" : "N/A");
                
                // Determine overall status
                if ((queryCacheHitRatio != null && queryCacheHitRatio < 0.5) || 
                    (maxQueryTime != null && maxQueryTime > 5000)) {
                    status.put("status", "DEGRADED");
                    status.put("reason", "Performance degradation detected");
                } else {
                    status.put("status", "HEALTHY");
                }
            } else {
                status.put("status", "UNKNOWN");
                status.put("reason", "No metrics available");
            }
        } catch (Exception e) {
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
}