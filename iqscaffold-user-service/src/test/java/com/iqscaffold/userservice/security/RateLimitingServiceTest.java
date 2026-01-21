package com.iqscaffold.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ZSetOperations<String, String> zsetOperations;

  @InjectMocks
  private RateLimitingService rateLimitingService;

  @BeforeEach
  void setUp() {
    // Setup is done per test as needed
  }

  @Test
  @DisplayName("isWithinRateLimit returns true when no previous requests exist")
  void withinRateLimitNoRequests() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.1";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(0L);
    when(zsetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

    var result = rateLimitingService.isWithinRateLimit(ipAddress);

    assertThat(result).isTrue();
    verify(zsetOperations).removeRangeByScore(eq("rate_limit:" + ipAddress), anyDouble(), anyDouble());
    verify(zsetOperations).add(eq("rate_limit:" + ipAddress), anyString(), anyDouble());
    verify(redisTemplate).expire(eq("rate_limit:" + ipAddress), anyLong(), eq(TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("isWithinRateLimit returns true when under the limit")
  void withinRateLimitUnderLimit() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.2";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(3L);
    when(zsetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

    var result = rateLimitingService.isWithinRateLimit(ipAddress);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("isWithinRateLimit returns false when limit is exceeded")
  void exceedsRateLimit() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.3";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(5L);

    var result = rateLimitingService.isWithinRateLimit(ipAddress);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isWithinRateLimit returns true when Redis returns null count")
  void withinRateLimitNullCount() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.4";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(null);

    var result = rateLimitingService.isWithinRateLimit(ipAddress);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("isWithinRateLimit returns true on Redis exception (fail open)")
  void withinRateLimitRedisException() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.5";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble()))
        .thenThrow(new RuntimeException("Redis connection failed"));

    var result = rateLimitingService.isWithinRateLimit(ipAddress);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("getRemainingAttempts returns correct count")
  void remainingAttempts() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.6";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(2L);

    var remaining = rateLimitingService.getRemainingAttempts(ipAddress);

    assertThat(remaining).isEqualTo(3);
  }

  @Test
  @DisplayName("getRemainingAttempts returns max when count is null")
  void remainingAttemptsNullCount() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.7";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(null);

    var remaining = rateLimitingService.getRemainingAttempts(ipAddress);

    assertThat(remaining).isEqualTo(5);
  }

  @Test
  @DisplayName("getRemainingAttempts returns zero when limit exceeded")
  void remainingAttemptsExceeded() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.8";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(7L);

    var remaining = rateLimitingService.getRemainingAttempts(ipAddress);

    assertThat(remaining).isZero();
  }

  @Test
  @DisplayName("getRemainingAttempts returns max on exception")
  void remainingAttemptsException() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.9";
    when(zsetOperations.count(anyString(), anyDouble(), anyDouble()))
        .thenThrow(new RuntimeException("Redis error"));

    var remaining = rateLimitingService.getRemainingAttempts(ipAddress);

    assertThat(remaining).isEqualTo(5);
  }

  @Test
  @DisplayName("getTimeUntilReset returns correct duration")
  void timeUntilReset() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.10";
    var oldestRequestTime = String.valueOf(System.currentTimeMillis() - 30000);
    when(zsetOperations.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
        .thenReturn(Set.of(oldestRequestTime));

    var duration = rateLimitingService.getTimeUntilReset(ipAddress);

    assertThat(duration).isNotNull();
    assertThat(duration.toMillis()).isGreaterThan(0);
  }

  @Test
  @DisplayName("getTimeUntilReset returns zero when no requests")
  void timeUntilResetNoRequests() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.11";
    when(zsetOperations.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
        .thenReturn(Set.of());

    var duration = rateLimitingService.getTimeUntilReset(ipAddress);

    assertThat(duration).isEqualTo(Duration.ZERO);
  }

  @Test
  @DisplayName("getTimeUntilReset returns zero when rangeByScore returns null")
  void timeUntilResetNull() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.12";
    when(zsetOperations.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
        .thenReturn(null);

    var duration = rateLimitingService.getTimeUntilReset(ipAddress);

    assertThat(duration).isEqualTo(Duration.ZERO);
  }

  @Test
  @DisplayName("getTimeUntilReset returns zero on exception")
  void timeUntilResetException() {
    when(redisTemplate.opsForZSet()).thenReturn(zsetOperations);
    var ipAddress = "192.168.1.13";
    when(zsetOperations.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
        .thenThrow(new RuntimeException("Redis error"));

    var duration = rateLimitingService.getTimeUntilReset(ipAddress);

    assertThat(duration).isEqualTo(Duration.ZERO);
  }

  @Test
  @DisplayName("clearRateLimit deletes the key")
  void clearRateLimit() {
    var ipAddress = "192.168.1.14";

    rateLimitingService.clearRateLimit(ipAddress);

    verify(redisTemplate).delete("rate_limit:" + ipAddress);
  }
}
