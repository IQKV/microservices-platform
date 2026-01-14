package com.iqscaffold.billingservice.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;

import org.hibernate.Cache;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing and monitoring Hibernate Second Level Cache.
 */
@Service
public class CacheManagementService {

  private static final Logger log = LoggerFactory.getLogger(CacheManagementService.class);

  private final EntityManagerFactory entityManagerFactory;

  public CacheManagementService(final EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * Evict all cached entities from the second level cache.
   */
  public void evictAllCaches() {
    log.info("Evicting all second level caches");
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Cache cache = sessionFactory.getCache();
    cache.evictAllRegions();
  }

  /**
   * Evict a specific entity class from the cache.
   */
  public void evictEntityCache(Class<?> entityClass) {
    log.info("Evicting cache for entity: {}", entityClass.getName());
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Cache cache = sessionFactory.getCache();
    cache.evictEntityData(entityClass);
  }

  /**
   * Evict a specific entity instance from the cache.
   */
  public void evictEntity(Class<?> entityClass, Object id) {
    log.info("Evicting cache for entity: {} with id: {}", entityClass.getName(), id);
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Cache cache = sessionFactory.getCache();
    cache.evictEntityData(entityClass, id);
  }

  /**
   * Evict all query caches.
   */
  public void evictQueryCaches() {
    log.info("Evicting all query caches");
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Cache cache = sessionFactory.getCache();
    cache.evictQueryRegions();
  }

  /**
   * Get cache statistics for monitoring.
   */
  public Map<String, Object> getCacheStatistics() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();

    return Map.of(
        "statisticsEnabled", statistics.isStatisticsEnabled(),
        "secondLevelCacheHitCount", statistics.getSecondLevelCacheHitCount(),
        "secondLevelCacheMissCount", statistics.getSecondLevelCacheMissCount(),
        "secondLevelCachePutCount", statistics.getSecondLevelCachePutCount(),
        "queryCacheHitCount", statistics.getQueryCacheHitCount(),
        "queryCacheMissCount", statistics.getQueryCacheMissCount(),
        "queryCachePutCount", statistics.getQueryCachePutCount()
    );
  }

  /**
   * Get statistics for a specific cache region.
   */
  public CacheRegionStatistics getRegionStatistics(String regionName) {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    return statistics.getCacheRegionStatistics(regionName);
  }

  /**
   * Enable or disable statistics collection.
   */
  public void setStatisticsEnabled(boolean enabled) {
    log.info("Setting cache statistics enabled: {}", enabled);
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(enabled);
  }

  /**
   * Clear all statistics.
   */
  public void clearStatistics() {
    log.info("Clearing cache statistics");
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();
  }
}
