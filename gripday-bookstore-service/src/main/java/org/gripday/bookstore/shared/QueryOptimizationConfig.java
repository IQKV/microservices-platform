package org.gripday.bookstore.shared;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class QueryOptimizationConfig {

  @Bean
  @Profile("!test")
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
    return hibernateProperties -> {
      // Enable query plan caching
      hibernateProperties.put(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, 2048);
      hibernateProperties.put(AvailableSettings.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE, 128);

      // Enable second-level cache for entities
      hibernateProperties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, true);
      hibernateProperties.put(AvailableSettings.USE_QUERY_CACHE, true);
      hibernateProperties.put(AvailableSettings.CACHE_REGION_FACTORY,
          "org.hibernate.cache.jcache.JCacheRegionFactory");

      // Optimize batch processing
      hibernateProperties.put(AvailableSettings.STATEMENT_BATCH_SIZE, 25);
      hibernateProperties.put(AvailableSettings.ORDER_INSERTS, true);
      hibernateProperties.put(AvailableSettings.ORDER_UPDATES, true);
      hibernateProperties.put(AvailableSettings.BATCH_VERSIONED_DATA, true);

      // Connection pool optimization
      hibernateProperties.put(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, true);

      // Query optimization
      hibernateProperties.put(AvailableSettings.USE_GET_GENERATED_KEYS, true);
      hibernateProperties.put(AvailableSettings.STATEMENT_FETCH_SIZE, 50);

      // Statistics for monitoring (only in non-production)
      hibernateProperties.put(AvailableSettings.GENERATE_STATISTICS, true);
      hibernateProperties.put(AvailableSettings.LOG_SLOW_QUERY, 1000); // Log queries slower than 1 second
    };
  }

  @Bean
  @Profile("production")
  public HibernatePropertiesCustomizer productionHibernatePropertiesCustomizer() {
    return hibernateProperties -> {
      // Production-specific optimizations
      hibernateProperties.put(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, 4096);
      hibernateProperties.put(AvailableSettings.STATEMENT_BATCH_SIZE, 50);

      // Disable statistics in production for performance
      hibernateProperties.put(AvailableSettings.GENERATE_STATISTICS, false);
      hibernateProperties.put(AvailableSettings.LOG_SLOW_QUERY, 2000); // Log only very slow queries

      // Enable connection pooling optimizations
      hibernateProperties.put("hibernate.hikari.maximumPoolSize", 20);
      hibernateProperties.put("hibernate.hikari.minimumIdle", 5);
      hibernateProperties.put("hibernate.hikari.connectionTimeout", 30000);
      hibernateProperties.put("hibernate.hikari.idleTimeout", 600000);
      hibernateProperties.put("hibernate.hikari.maxLifetime", 1800000);
    };
  }
}