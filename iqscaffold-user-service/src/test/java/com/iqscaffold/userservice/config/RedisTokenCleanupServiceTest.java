package com.iqscaffold.userservice.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Unit tests for RedisTokenCleanupService.
 */
@ExtendWith(MockitoExtension.class)
class RedisTokenCleanupServiceTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private MeterRegistry meterRegistry;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @Mock
  private Counter counter;

  @Mock
  private Timer timer;

  @InjectMocks
  private RedisTokenCleanupService service;

  @BeforeEach
  void setUp() {
    // Setup is done in individual tests
  }

  @Test
  @DisplayName("Should cleanup expired refresh tokens")
  void shouldCleanupExpiredRefreshTokens() {
    // Arrange
    var refreshKeys = Set.of("refresh:token:1", "refresh:token:2");
    when(redisTemplate.keys("refresh:token:*")).thenReturn(refreshKeys);
    when(redisTemplate.keys("revoked:refresh:*")).thenReturn(Set.of());
    when(redisTemplate.keys("session:*")).thenReturn(Set.of());
    when(redisTemplate.keys("blacklist:token:*")).thenReturn(Set.of());
    when(redisTemplate.getExpire("refresh:token:1")).thenReturn(-1L);
    when(redisTemplate.getExpire("refresh:token:2")).thenReturn(3600L);
    when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    when(meterRegistry.timer(anyString())).thenReturn(timer);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(redisTemplate).delete("refresh:token:1");
    verify(meterRegistry).counter("token.cleanup.refresh_tokens", "status", "removed");
  }

  @Test
  @DisplayName("Should cleanup old revocation records")
  void shouldCleanupOldRevocationRecords() {
    // Arrange
    var revocationKeys = Set.of("revoked:refresh:1", "revoked:refresh:2");
    var oldTimestamp = String.valueOf(System.currentTimeMillis() / 1000 - (31 * 24 * 60 * 60));
    var recentTimestamp = String.valueOf(System.currentTimeMillis() / 1000);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(redisTemplate.keys("refresh:token:*")).thenReturn(Set.of());
    when(redisTemplate.keys("revoked:refresh:*")).thenReturn(revocationKeys);
    when(redisTemplate.keys("session:*")).thenReturn(Set.of());
    when(redisTemplate.keys("blacklist:token:*")).thenReturn(Set.of());
    when(valueOperations.get("revoked:refresh:1")).thenReturn(oldTimestamp);
    when(valueOperations.get("revoked:refresh:2")).thenReturn(recentTimestamp);
    when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    when(meterRegistry.timer(anyString())).thenReturn(timer);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(redisTemplate).delete("revoked:refresh:1");
    verify(meterRegistry).counter("token.cleanup.revocations", "status", "removed");
  }

  @Test
  @DisplayName("Should cleanup expired sessions")
  void shouldCleanupExpiredSessions() {
    // Arrange
    var sessionKeys = Set.of("session:1", "session:2");
    when(redisTemplate.keys("refresh:token:*")).thenReturn(Set.of());
    when(redisTemplate.keys("revoked:refresh:*")).thenReturn(Set.of());
    when(redisTemplate.keys("session:*")).thenReturn(sessionKeys);
    when(redisTemplate.keys("blacklist:token:*")).thenReturn(Set.of());
    when(redisTemplate.getExpire("session:1")).thenReturn(-1L);
    when(redisTemplate.getExpire("session:2")).thenReturn(1800L);
    when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    when(meterRegistry.timer(anyString())).thenReturn(timer);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(redisTemplate).delete("session:1");
    verify(meterRegistry).counter("token.cleanup.sessions", "status", "removed");
  }

  @Test
  @DisplayName("Should cleanup expired blacklist entries")
  void shouldCleanupExpiredBlacklistEntries() {
    // Arrange
    var blacklistKeys = Set.of("blacklist:token:1", "blacklist:token:2");
    when(redisTemplate.keys("refresh:token:*")).thenReturn(Set.of());
    when(redisTemplate.keys("revoked:refresh:*")).thenReturn(Set.of());
    when(redisTemplate.keys("session:*")).thenReturn(Set.of());
    when(redisTemplate.keys("blacklist:token:*")).thenReturn(blacklistKeys);
    when(redisTemplate.getExpire("blacklist:token:1")).thenReturn(-1L);
    when(redisTemplate.getExpire("blacklist:token:2")).thenReturn(900L);
    when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    when(meterRegistry.timer(anyString())).thenReturn(timer);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(redisTemplate).delete("blacklist:token:1");
    verify(meterRegistry).counter("token.cleanup.blacklist", "status", "removed");
  }

  @Test
  @DisplayName("Should handle empty key sets gracefully")
  void shouldHandleEmptyKeySetsGracefully() {
    // Arrange
    when(redisTemplate.keys(anyString())).thenReturn(Set.of());
    when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    when(meterRegistry.timer(anyString())).thenReturn(timer);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(meterRegistry).timer("token.cleanup.duration");
  }

  @Test
  @DisplayName("Should handle null key sets gracefully")
  void shouldHandleNullKeySetsGracefully() {
    // Arrange
    when(redisTemplate.keys(anyString())).thenReturn(null);
    when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    when(meterRegistry.timer(anyString())).thenReturn(timer);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(meterRegistry).timer("token.cleanup.duration");
  }

  @Test
  @DisplayName("Should record metrics on cleanup completion")
  void shouldRecordMetricsOnCleanupCompletion() {
    // Arrange
    when(redisTemplate.keys(anyString())).thenReturn(Set.of());
    when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    when(meterRegistry.timer(anyString())).thenReturn(timer);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(meterRegistry).counter("token.cleanup.refresh_tokens", "status", "removed");
    verify(meterRegistry).counter("token.cleanup.revocations", "status", "removed");
    verify(meterRegistry).counter("token.cleanup.sessions", "status", "removed");
    verify(meterRegistry).counter("token.cleanup.blacklist", "status", "removed");
    verify(meterRegistry).timer("token.cleanup.duration");
  }

  @Test
  @DisplayName("Should record error metric on exception")
  void shouldRecordErrorMetricOnException() {
    // Arrange
    when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis error"));
    when(meterRegistry.counter(anyString())).thenReturn(counter);

    // Act
    service.cleanupExpiredTokens();

    // Assert
    verify(meterRegistry).counter("token.cleanup.errors");
  }
}
