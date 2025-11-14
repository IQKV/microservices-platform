package org.gripday.authservice.config;

import java.time.Duration;

import org.gripday.authservice.domain.service.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * Redis configuration for User Service with tenant-aware caching and session management. Configures Redis connection, serialization, tenant-isolated caching, and session storage.
 */
@Configuration
@EnableCaching
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes session timeout
@EnableConfigurationProperties(GripdayProperties.class)
public class RedisConfig extends AbstractHttpSessionApplicationInitializer {

  private final GripdayProperties gripdayProperties;

  public RedisConfig(final GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
  }

  /**
   * Configure RedisTemplate with proper serializers for key-value operations. Uses String serializer for keys and Jackson JSON serializer for values.
   */
  @Bean
  @Primary
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
   * Configure tenant-aware Redis template with automatic key prefixing. All keys are automatically prefixed with tenant ID for isolation.
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
   * Configure tenant-aware cache manager with namespace isolation. Each tenant gets its own cache namespace to prevent data leakage.
   */
  @Bean
  @Primary
  public CacheManager tenantAwareCacheManager(RedisConnectionFactory connectionFactory) {
    var cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30)) // Default TTL of 30 minutes
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new TenantAwareStringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues(); // Don't cache null values

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(cacheConfiguration)
        // Configure specific cache configurations
        .withCacheConfiguration("users",
            cacheConfiguration.entryTtl(Duration.ofMinutes(15)))
        .withCacheConfiguration("authorities",
            cacheConfiguration.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("tenants",
            cacheConfiguration.entryTtl(Duration.ofHours(2)))
        .withCacheConfiguration("jwt-blacklist",
            cacheConfiguration.entryTtl(Duration.ofHours(24)))
        .withCacheConfiguration("rate-limits",
            cacheConfiguration.entryTtl(Duration.ofMinutes(1)))
        .withCacheConfiguration("account-lockouts",
            cacheConfiguration.entryTtl(Duration.ofMinutes(30)))
        .build();
  }

  /**
   * Configure session-specific cache manager for session storage with tenant isolation.
   */
  @Bean("sessionCacheManager")
  public CacheManager sessionCacheManager(RedisConnectionFactory connectionFactory) {
    var sessionCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30)) // Session TTL of 30 minutes
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new TenantAwareSessionKeySerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(sessionCacheConfiguration)
        .withCacheConfiguration("sessions", sessionCacheConfiguration)
        .build();
  }

  /**
   * Tenant-aware string Redis serializer that automatically prefixes keys with tenant ID. Provides automatic tenant isolation for all Redis operations.
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
   * Tenant-aware session key serializer for session management with tenant isolation. Ensures session keys are properly namespaced by tenant ID.
   */
  public static class TenantAwareSessionKeySerializer extends StringRedisSerializer {

    private static final String SESSION_PREFIX = "spring:session:";

    @Override
    public byte[] serialize(String string) {
      if (string == null) {
        return super.serialize(null);
      }

      // For session keys, add tenant namespace after the session prefix
      if (string.startsWith(SESSION_PREFIX)) {
        var sessionKey = string.substring(SESSION_PREFIX.length());
        var tenantAwareKey = SESSION_PREFIX + TenantContext.createTenantAwareCacheKey(sessionKey);
        return super.serialize(tenantAwareKey);
      } else {
        // For non-session keys, use standard tenant-aware key
        var tenantAwareKey = TenantContext.createTenantAwareCacheKey(string);
        return super.serialize(tenantAwareKey);
      }
    }

    @Override
    public String deserialize(byte[] bytes) {
      var key = super.deserialize(bytes);
      if (key == null) {
        return null;
      }

      // Extract original key by removing tenant prefix
      var tenantId = TenantContext.getCurrentTenantIdOrDefault();
      var tenantPrefix = tenantId + ":";

      if (key.startsWith(SESSION_PREFIX)) {
        var sessionPart = key.substring(SESSION_PREFIX.length());
        if (sessionPart.startsWith(tenantPrefix)) {
          return SESSION_PREFIX + sessionPart.substring(tenantPrefix.length());
        }
      } else if (key.startsWith(tenantPrefix)) {
        return key.substring(tenantPrefix.length());
      }

      return key;
    }
  }

  /**
   * Service for tenant-aware Redis operations. Provides high-level methods for tenant-isolated caching and session management.
   */
  @Bean
  public TenantAwareRedisService tenantAwareRedisService(
      @Qualifier("tenantAwareRedisTemplate") RedisTemplate<String, Object> tenantAwareRedisTemplate,
      @Qualifier("sessionCacheManager") CacheManager sessionCacheManager) {
    return new TenantAwareRedisService(tenantAwareRedisTemplate, sessionCacheManager);
  }

  /**
   * Service for tenant-aware session management. Provides session operations with automatic tenant isolation.
   */
  @Bean
  public TenantAwareSessionService tenantAwareSessionService(
      @Qualifier("tenantAwareRedisTemplate") RedisTemplate<String, Object> tenantAwareRedisTemplate) {
    return new TenantAwareSessionService(tenantAwareRedisTemplate);
  }

  /**
   * Service class for tenant-aware Redis operations with caching and session support.
   */
  public static class TenantAwareRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager sessionCacheManager;

    public TenantAwareRedisService(final RedisTemplate<String, Object> redisTemplate, final CacheManager sessionCacheManager) {
      this.redisTemplate = redisTemplate;
      this.sessionCacheManager = sessionCacheManager;
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

    /**
     * Increment counter with tenant isolation.
     */
    public Long increment(String key) {
      return redisTemplate.opsForValue().increment(key);
    }

    /**
     * Increment counter by delta with tenant isolation.
     */
    public Long increment(String key, long delta) {
      return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * Store hash field with tenant isolation.
     */
    public void putHash(String key, String hashKey, Object value) {
      redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * Get hash field with tenant isolation.
     */
    public Object getHash(String key, String hashKey) {
      return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * Delete hash field with tenant isolation.
     */
    public Long deleteHash(String key, String... hashKeys) {
      return redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    /**
     * Get all hash entries with tenant isolation.
     */
    public java.util.Map<Object, Object> getAllHash(String key) {
      return redisTemplate.opsForHash().entries(key);
    }
  }

  /**
   * Service class for tenant-aware session management operations.
   */
  public static class TenantAwareSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user-sessions:";

    public TenantAwareSessionService(final RedisTemplate<String, Object> redisTemplate) {
      this.redisTemplate = redisTemplate;
    }

    /**
     * Store session data with tenant isolation.
     */
    public void storeSession(String sessionId, Object sessionData, Duration timeout) {
      var sessionKey = SESSION_KEY_PREFIX + sessionId;
      redisTemplate.opsForValue().set(sessionKey, sessionData, timeout);
    }

    /**
     * Retrieve session data with tenant isolation.
     */
    public Object getSession(String sessionId) {
      var sessionKey = SESSION_KEY_PREFIX + sessionId;
      return redisTemplate.opsForValue().get(sessionKey);
    }

    /**
     * Delete session with tenant isolation.
     */
    public Boolean deleteSession(String sessionId) {
      var sessionKey = SESSION_KEY_PREFIX + sessionId;
      return redisTemplate.delete(sessionKey);
    }

    /**
     * Check if session exists with tenant isolation.
     */
    public Boolean sessionExists(String sessionId) {
      var sessionKey = SESSION_KEY_PREFIX + sessionId;
      return redisTemplate.hasKey(sessionKey);
    }

    /**
     * Extend session timeout with tenant isolation.
     */
    public Boolean extendSession(String sessionId, Duration timeout) {
      var sessionKey = SESSION_KEY_PREFIX + sessionId;
      return redisTemplate.expire(sessionKey, timeout);
    }

    /**
     * Store user session mapping with tenant isolation. Allows tracking all sessions for a specific user.
     */
    public void addUserSession(String userId, String sessionId) {
      var userSessionKey = USER_SESSION_PREFIX + userId;
      redisTemplate.opsForSet().add(userSessionKey, sessionId);
      // Set expiration for user session tracking (longer than individual sessions)
      redisTemplate.expire(userSessionKey, Duration.ofHours(24));
    }

    /**
     * Remove user session mapping with tenant isolation.
     */
    public void removeUserSession(String userId, String sessionId) {
      var userSessionKey = USER_SESSION_PREFIX + userId;
      redisTemplate.opsForSet().remove(userSessionKey, sessionId);
    }

    /**
     * Get all sessions for a user with tenant isolation.
     */
    public java.util.Set<Object> getUserSessions(String userId) {
      var userSessionKey = USER_SESSION_PREFIX + userId;
      return redisTemplate.opsForSet().members(userSessionKey);
    }

    /**
     * Invalidate all sessions for a user with tenant isolation.
     */
    public void invalidateAllUserSessions(String userId) {
      var userSessionKey = USER_SESSION_PREFIX + userId;
      var sessions = redisTemplate.opsForSet().members(userSessionKey);

      if (sessions != null && !sessions.isEmpty()) {
        // Delete all individual sessions
        for (final var session : sessions) {
          var sessionKey = SESSION_KEY_PREFIX + session;
          redisTemplate.delete(sessionKey);
        }

        // Clear user session tracking
        redisTemplate.delete(userSessionKey);
      }
    }
  }
}