package com.iqscaffold.billingservice.feature;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for feature management system.
 *
 * <p>Configures caching, async processing, and feature-related properties.
 */
@Configuration
@EnableCaching
@EnableAsync
@ConfigurationProperties(prefix = "iqscaffold.features")
public class FeatureConfiguration {

  private Cache cache = new Cache();
  private UsageTracking usageTracking = new UsageTracking();

  /**
   * Cache manager for feature-related data.
   * Uses ConcurrentMapCacheManager for simple in-memory caching.
   */
  @Bean("featureCacheManager")
  public CacheManager featureCacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

    // Pre-create caches
    cacheManager.setCacheNames(java.util.List.of(
        "featureEnablement",
        "featureContext",
        "featureDefinitions"
    ));

    return cacheManager;
  }

  // Getters and setters for configuration properties
  public Cache getCache() {
    return cache;
  }

  public void setCache(Cache cache) {
    this.cache = cache;
  }

  public UsageTracking getUsageTracking() {
    return usageTracking;
  }

  public void setUsageTracking(UsageTracking usageTracking) {
    this.usageTracking = usageTracking;
  }

  /**
   * Cache configuration properties.
   */
  public static class Cache {
    private long maxSize = 10000;
    private Duration ttl = Duration.ofMinutes(15);

    public long getMaxSize() {
      return maxSize;
    }

    public void setMaxSize(long maxSize) {
      this.maxSize = maxSize;
    }

    public Duration getTtl() {
      return ttl;
    }

    public void setTtl(Duration ttl) {
      this.ttl = ttl;
    }
  }

  /**
   * Usage tracking configuration properties.
   */
  public static class UsageTracking {
    private boolean enabled = true;
    private int retentionDays = 90;
    private boolean asyncEnabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getRetentionDays() {
      return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
      this.retentionDays = retentionDays;
    }

    public boolean isAsyncEnabled() {
      return asyncEnabled;
    }

    public void setAsyncEnabled(boolean asyncEnabled) {
      this.asyncEnabled = asyncEnabled;
    }
  }
}
