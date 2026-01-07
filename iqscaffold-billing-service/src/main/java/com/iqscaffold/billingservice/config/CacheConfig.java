package com.iqscaffold.billingservice.config;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Cache;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Hibernate Second Level Cache with Ehcache 3.
 * Provides cache monitoring and management capabilities.
 */
@Configuration
public class CacheConfig {

  private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

  /**
   * Health indicator for cache monitoring.
   */
  @Bean
  public HealthIndicator cacheHealthIndicator(EntityManagerFactory entityManagerFactory) {
    return () -> {
      try {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Cache cache = sessionFactory.getCache();
        
        if (cache == null) {
          return Health.down().withDetail("reason", "Cache not available").build();
        }

        // Get cache statistics
        org.hibernate.stat.Statistics statistics = sessionFactory.getStatistics();
        
        return Health.up()
            .withDetail("secondLevelCacheEnabled", true)
            .withDetail("queryCacheEnabled", statistics.isStatisticsEnabled())
            .withDetail("cacheRegions", cache.getCacheRegionNames())
            .build();
      } catch (final Exception e) {
        log.error("Error checking cache health", e);
        return Health.down()
            .withDetail("error", e.getMessage())
            .build();
      }
    };
  }
}
