package com.iqscaffold.billingservice.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.iqscaffold.billingservice.shared.BillingConstants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for caching and distributed data storage.
 *
 * <p>Configures cache-specific TTL settings:
 * <ul>
 *   <li>Subscriptions: 5 minutes (frequently changing)</li>
 *   <li>Plans: 1 hour (relatively stable)</li>
 *   <li>Usage: 1 minute (real-time data)</li>
 *   <li>Quotas: 1 minute (real-time enforcement)</li>
 *   <li>Payment Methods: 10 minutes (moderate stability)</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisConfig {

  /**
   * Configure cache manager with specific TTL settings for each cache.
   *
   * @param connectionFactory the Redis connection factory
   * @return configured cache manager
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // Default cache configuration
    var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))
        .disableCachingNullValues()
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
        )
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()
            )
        );

    // Cache-specific configurations with different TTLs
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // Subscriptions: 5 minutes (frequently changing)
    cacheConfigurations.put(
        BillingConstants.CacheNames.SUBSCRIPTIONS,
        defaultConfig.entryTtl(Duration.ofMinutes(5))
    );

    // Plans: 1 hour (relatively stable)
    cacheConfigurations.put(
        BillingConstants.CacheNames.PLANS,
        defaultConfig.entryTtl(Duration.ofHours(1))
    );

    // Usage: 1 minute (real-time data)
    cacheConfigurations.put(
        BillingConstants.CacheNames.USAGE,
        defaultConfig.entryTtl(Duration.ofMinutes(1))
    );

    // Quotas: 1 minute (real-time enforcement)
    cacheConfigurations.put(
        BillingConstants.CacheNames.QUOTAS,
        defaultConfig.entryTtl(Duration.ofMinutes(1))
    );

    // Payment Methods: 10 minutes (moderate stability)
    cacheConfigurations.put(
        BillingConstants.CacheNames.PAYMENT_METHODS,
        defaultConfig.entryTtl(Duration.ofMinutes(10))
    );

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware()
        .build();
  }

  /**
   * Configure Redis template for general-purpose Redis operations.
   *
   * @param connectionFactory the Redis connection factory
   * @return configured Redis template
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    var template = new RedisTemplate<String, Object>();
    template.setConnectionFactory(connectionFactory);

    // Use String serialization for keys
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    // Use JSON serialization for values
    var jsonSerializer = new GenericJackson2JsonRedisSerializer();
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.setEnableTransactionSupport(true);
    template.afterPropertiesSet();

    return template;
  }
}
