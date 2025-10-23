package org.gripday.authservice.config;

import org.gripday.authservice.domain.service.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
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

import java.time.Duration;

/**
 * Redis configuration for Auth Service with tenant-aware caching support.
 * Configures Redis connection, serialization, and tenant-isolated caching.
 */
@Configuration
@EnableCaching
public class RedisConfiguration {

    /**
     * Configure RedisTemplate with proper serializers for key-value operations.
     * Uses String serializer for keys and Jackson JSON serializer for values.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure tenant-aware Redis template with automatic key prefixing.
     * All keys are automatically prefixed with tenant ID for isolation.
     */
    @Bean
    public RedisTemplate<String, Object> tenantAwareRedisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);
        
        // Use tenant-aware key serializer
        template.setKeySerializer(new TenantAwareStringRedisSerializer());
        template.setHashKeySerializer(new TenantAwareStringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure tenant-aware cache manager with namespace isolation.
     * Each tenant gets its own cache namespace to prevent data leakage.
     */
    @Bean
    public CacheManager tenantAwareCacheManager(RedisConnectionFactory connectionFactory) {
        var cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)) // Default TTL of 30 minutes
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new TenantAwareStringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfiguration)
            .build();
    }

    /**
     * Tenant-aware string Redis serializer that automatically prefixes keys with tenant ID.
     * Provides automatic tenant isolation for all Redis operations.
     */
    public static class TenantAwareStringRedisSerializer extends StringRedisSerializer {
        
        @Override
        public byte[] serialize(String string) {
            if (string == null) {
                return super.serialize(null);
            }
            
            // Create tenant-aware key
            var tenantAwareKey = TenantContext.createTenantAwareCacheKey(string);
            return super.serialize(tenantAwareKey);
        }
        
        @Override
        public String deserialize(byte[] bytes) {
            var key = super.deserialize(bytes);
            if (key == null) {
                return null;
            }
            
            // Extract original key by removing tenant prefix
            var tenantId = TenantContext.getCurrentTenantIdOrDefault();
            var prefix = tenantId + ":";
            
            if (key.startsWith(prefix)) {
                return key.substring(prefix.length());
            }
            
            return key;
        }
    }

    /**
     * Service for tenant-aware Redis operations.
     * Provides high-level methods for tenant-isolated caching.
     */
    @Bean
    public TenantAwareRedisService tenantAwareRedisService(@Qualifier("tenantAwareRedisTemplate") RedisTemplate<String, Object> tenantAwareRedisTemplate) {
        return new TenantAwareRedisService(tenantAwareRedisTemplate);
    }

    /**
     * Service class for tenant-aware Redis operations.
     */
    public static class TenantAwareRedisService {
        
        private final RedisTemplate<String, Object> redisTemplate;
        
        public TenantAwareRedisService(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }
        
        /**
         * Store value with tenant isolation.
         */
        public void set(String key, Object value) {
            redisTemplate.opsForValue().set(key, value);
        }
        
        /**
         * Store value with TTL and tenant isolation.
         */
        public void set(String key, Object value, Duration timeout) {
            redisTemplate.opsForValue().set(key, value, timeout);
        }
        
        /**
         * Get value with tenant isolation.
         */
        public Object get(String key) {
            return redisTemplate.opsForValue().get(key);
        }
        
        /**
         * Delete key with tenant isolation.
         */
        public Boolean delete(String key) {
            return redisTemplate.delete(key);
        }
        
        /**
         * Check if key exists with tenant isolation.
         */
        public Boolean hasKey(String key) {
            return redisTemplate.hasKey(key);
        }
        
        /**
         * Set expiration for key with tenant isolation.
         */
        public Boolean expire(String key, Duration timeout) {
            return redisTemplate.expire(key, timeout);
        }
        
        /**
         * Add to set with tenant isolation.
         */
        public Long addToSet(String key, Object... values) {
            return redisTemplate.opsForSet().add(key, values);
        }
        
        /**
         * Remove from set with tenant isolation.
         */
        public Long removeFromSet(String key, Object... values) {
            return redisTemplate.opsForSet().remove(key, values);
        }
        
        /**
         * Check if set contains value with tenant isolation.
         */
        public Boolean isSetMember(String key, Object value) {
            return redisTemplate.opsForSet().isMember(key, value);
        }
    }
}