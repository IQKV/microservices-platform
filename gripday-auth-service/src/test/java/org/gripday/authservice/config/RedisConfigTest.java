package org.gripday.authservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.gripday.authservice.config.RedisConfig.TenantAwareRedisService;
import org.gripday.authservice.config.RedisConfig.TenantAwareSessionService;
import org.gripday.authservice.domain.service.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test class for Redis configuration with tenant-aware caching and session management. Tests basic Redis operations, tenant isolation, and session management functionality.
 * 
 * Note: This test requires Redis to be running. Use docker-compose to start Redis:
 * docker-compose up -d redis
 */
@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Redis to be running - enable when Redis is available")
class RedisConfigTest {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private TenantAwareRedisService tenantAwareRedisService;

  @Autowired
  private TenantAwareSessionService tenantAwareSessionService;

  @Autowired
  private CacheManager tenantAwareCacheManager;

  private static final String TEST_TENANT_ID = "test-tenant";
  private static final String TEST_KEY = "test-key";
  private static final String TEST_VALUE = "test-value";

  @BeforeEach
  void setUp() {
    // Set up tenant context for testing
    TenantContext.setCurrentTenantId(TEST_TENANT_ID);
  }

  @AfterEach
  void tearDown() {
    // Clean up tenant context and test data
    TenantContext.clear();

    // Clean up test keys
    try {
      redisTemplate.delete(TEST_TENANT_ID + ":" + TEST_KEY);
      redisTemplate.delete("session:" + TEST_KEY);
      redisTemplate.delete("user-sessions:123");
    } catch (Exception e) {
      // Log cleanup errors for debugging
      System.err.println("Error during test cleanup: " + e.getMessage());
    }
  }

  @Test
  void testRedisTemplateConfiguration() {
    // Test basic Redis template functionality
    redisTemplate.opsForValue().set("basic-test", "basic-value");
    var result = redisTemplate.opsForValue().get("basic-test");

    assertThat(result).isEqualTo("basic-value");

    // Cleanup
    redisTemplate.delete("basic-test");
  }

  @Test
  void testTenantAwareRedisService() {
    // Test tenant-aware Redis operations
    tenantAwareRedisService.set(TEST_KEY, TEST_VALUE);

    var result = tenantAwareRedisService.get(TEST_KEY);
    assertThat(result).isEqualTo(TEST_VALUE);

    // Test key existence
    var exists = tenantAwareRedisService.hasKey(TEST_KEY);
    assertThat(exists).isTrue();

    // Test deletion
    var deleted = tenantAwareRedisService.delete(TEST_KEY);
    assertThat(deleted).isTrue();

    // Verify deletion
    var existsAfterDelete = tenantAwareRedisService.hasKey(TEST_KEY);
    assertThat(existsAfterDelete).isFalse();
  }

  @Test
  void testTenantAwareRedisServiceWithTTL() {
    // Test Redis operations with TTL
    var shortTtl = Duration.ofSeconds(1);
    tenantAwareRedisService.set(TEST_KEY, TEST_VALUE, shortTtl);

    var result = tenantAwareRedisService.get(TEST_KEY);
    assertThat(result).isEqualTo(TEST_VALUE);

    // Test expiration setting
    var expireSet = tenantAwareRedisService.expire(TEST_KEY, Duration.ofMinutes(5));
    assertThat(expireSet).isTrue();
  }

  @Test
  void testTenantAwareRedisServiceSetOperations() {
    // Test set operations with tenant isolation
    var setKey = "test-set";

    var added = tenantAwareRedisService.addToSet(setKey, "value1", "value2");
    assertThat(added).isEqualTo(2);

    var isMember = tenantAwareRedisService.isSetMember(setKey, "value1");
    assertThat(isMember).isTrue();

    var removed = tenantAwareRedisService.removeFromSet(setKey, "value1");
    assertThat(removed).isEqualTo(1);

    var isMemberAfterRemove = tenantAwareRedisService.isSetMember(setKey, "value1");
    assertThat(isMemberAfterRemove).isFalse();

    // Cleanup
    tenantAwareRedisService.delete(setKey);
  }

  @Test
  void testTenantAwareRedisServiceHashOperations() {
    // Test hash operations with tenant isolation
    var hashKey = "test-hash";

    tenantAwareRedisService.putHash(hashKey, "field1", "value1");
    tenantAwareRedisService.putHash(hashKey, "field2", "value2");

    var fieldValue = tenantAwareRedisService.getHash(hashKey, "field1");
    assertThat(fieldValue).isEqualTo("value1");

    var allHash = tenantAwareRedisService.getAllHash(hashKey);
    assertThat(allHash).hasSize(2);
    assertThat(allHash).containsEntry("field1", "value1");
    assertThat(allHash).containsEntry("field2", "value2");

    var deletedFields = tenantAwareRedisService.deleteHash(hashKey, "field1");
    assertThat(deletedFields).isEqualTo(1);

    // Cleanup
    tenantAwareRedisService.delete(hashKey);
  }

  @Test
  void testTenantAwareSessionService() {
    // Test session management with tenant isolation
    var sessionId = "test-session-123";
    var sessionData = "session-data-value";
    var sessionTimeout = Duration.ofMinutes(30);

    // Store session
    tenantAwareSessionService.storeSession(sessionId, sessionData, sessionTimeout);

    // Retrieve session
    var retrievedData = tenantAwareSessionService.getSession(sessionId);
    assertThat(retrievedData).isEqualTo(sessionData);

    // Check session existence
    var exists = tenantAwareSessionService.sessionExists(sessionId);
    assertThat(exists).isTrue();

    // Extend session
    var extended = tenantAwareSessionService.extendSession(sessionId, Duration.ofHours(1));
    assertThat(extended).isTrue();

    // Delete session
    var deleted = tenantAwareSessionService.deleteSession(sessionId);
    assertThat(deleted).isTrue();

    // Verify deletion
    var existsAfterDelete = tenantAwareSessionService.sessionExists(sessionId);
    assertThat(existsAfterDelete).isFalse();
  }

  @Test
  void testTenantAwareSessionServiceUserSessions() {
    // Test user session tracking with tenant isolation
    var userId = "123";
    var sessionId1 = "session-1";
    var sessionId2 = "session-2";

    // Add user sessions
    tenantAwareSessionService.addUserSession(userId, sessionId1);
    tenantAwareSessionService.addUserSession(userId, sessionId2);

    // Get user sessions
    var userSessions = tenantAwareSessionService.getUserSessions(userId);
    assertThat(userSessions).hasSize(2);
    assertThat(userSessions).contains(sessionId1, sessionId2);

    // Remove one session
    tenantAwareSessionService.removeUserSession(userId, sessionId1);

    var userSessionsAfterRemove = tenantAwareSessionService.getUserSessions(userId);
    assertThat(userSessionsAfterRemove).hasSize(1);
    assertThat(userSessionsAfterRemove).contains(sessionId2);

    // Invalidate all user sessions
    tenantAwareSessionService.invalidateAllUserSessions(userId);

    var userSessionsAfterInvalidate = tenantAwareSessionService.getUserSessions(userId);
    assertThat(userSessionsAfterInvalidate).isEmpty();
  }

  @Test
  void testTenantIsolation() {
    // Test that different tenants have isolated data
    var tenant1 = "tenant-1";
    var tenant2 = "tenant-2";
    var key = "isolation-test";
    var value1 = "value-for-tenant-1";
    var value2 = "value-for-tenant-2";

    // Set value for tenant 1
    TenantContext.setCurrentTenantId(tenant1);
    tenantAwareRedisService.set(key, value1);

    // Set value for tenant 2
    TenantContext.setCurrentTenantId(tenant2);
    tenantAwareRedisService.set(key, value2);

    // Verify tenant 1 data
    TenantContext.setCurrentTenantId(tenant1);
    var result1 = tenantAwareRedisService.get(key);
    assertThat(result1).isEqualTo(value1);

    // Verify tenant 2 data
    TenantContext.setCurrentTenantId(tenant2);
    var result2 = tenantAwareRedisService.get(key);
    assertThat(result2).isEqualTo(value2);

    // Cleanup
    TenantContext.setCurrentTenantId(tenant1);
    tenantAwareRedisService.delete(key);
    TenantContext.setCurrentTenantId(tenant2);
    tenantAwareRedisService.delete(key);
  }

  @Test
  void testCacheManagerConfiguration() {
    // Test that cache manager is properly configured
    assertThat(tenantAwareCacheManager).isNotNull();

    var cacheNames = tenantAwareCacheManager.getCacheNames();
    assertThat(cacheNames).isNotEmpty();

    // Test getting a specific cache
    var usersCache = tenantAwareCacheManager.getCache("users");
    assertThat(usersCache).isNotNull();
  }
}