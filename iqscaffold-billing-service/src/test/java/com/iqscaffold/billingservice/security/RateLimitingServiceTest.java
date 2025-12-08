package com.iqscaffold.billingservice.security;

import com.iqscaffold.billingservice.config.BillingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitingService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingService Tests")
class RateLimitingServiceTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ZSetOperations<String, String> zSetOperations;

  private RateLimitingService rateLimitingService;
  private BillingProperties billingProperties;

  @BeforeEach
  void setUp() {
    // Create test properties with rate limiting enabled
    billingProperties = new BillingProperties(
        new BillingProperties.Security(
            new BillingProperties.Security.Jwt(
                "http://localhost:8081/.well-known/jwks.json",
                "iqscaffold-user-service"
            ),
            new BillingProperties.Security.RateLimiting(
                true,
                100,
                200
            )
        ),
        new BillingProperties.Integration(
            new BillingProperties.Integration.UserService("http://localhost:8081", Duration.ofSeconds(5)),
            new BillingProperties.Integration.EmailService("http://localhost:8084", Duration.ofSeconds(10))
        ),
        new BillingProperties.Payment("stripe", null, null),
        new BillingProperties.Subscription("USD", 14, 3, false),
        new BillingProperties.Usage(true, 100, Duration.ofSeconds(30), 365),
        new BillingProperties.Invoice("INV-{YEAR}{MONTH}-{SEQUENCE}", 7, true, true),
        new BillingProperties.Portal(true, true, true),
        new BillingProperties.Features(true, true, true, true)
    );

    lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

    rateLimitingService = new RateLimitingService(redisTemplate, billingProperties);
  }

  @Test
  @DisplayName("Should allow request when within rate limit")
  void shouldAllowRequestWhenWithinRateLimit() {
    // Given
    String ipAddress = "192.168.1.1";
    when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(50L);

    // When
    boolean result = rateLimitingService.isWithinRateLimit(ipAddress);

    // Then
    assertThat(result).isTrue();
    verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
    verify(zSetOperations).count(anyString(), anyDouble(), anyDouble());
    verify(zSetOperations).add(anyString(), anyString(), anyDouble());
    verify(redisTemplate).expire(anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("Should deny request when rate limit exceeded")
  void shouldDenyRequestWhenRateLimitExceeded() {
    // Given
    String ipAddress = "192.168.1.1";
    when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(100L);

    // When
    boolean result = rateLimitingService.isWithinRateLimit(ipAddress);

    // Then
    assertThat(result).isFalse();
    verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
    verify(zSetOperations).count(anyString(), anyDouble(), anyDouble());
    verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
  }

  @Test
  @DisplayName("Should allow request when rate limiting is disabled")
  void shouldAllowRequestWhenRateLimitingDisabled() {
    // Given
    BillingProperties disabledProperties = new BillingProperties(
        new BillingProperties.Security(
            new BillingProperties.Security.Jwt(
                "http://localhost:8081/.well-known/jwks.json",
                "iqscaffold-user-service"
            ),
            new BillingProperties.Security.RateLimiting(
                false,  // disabled
                100,
                200
            )
        ),
        new BillingProperties.Integration(
            new BillingProperties.Integration.UserService("http://localhost:8081", Duration.ofSeconds(5)),
            new BillingProperties.Integration.EmailService("http://localhost:8084", Duration.ofSeconds(10))
        ),
        new BillingProperties.Payment("stripe", null, null),
        new BillingProperties.Subscription("USD", 14, 3, false),
        new BillingProperties.Usage(true, 100, Duration.ofSeconds(30), 365),
        new BillingProperties.Invoice("INV-{YEAR}{MONTH}-{SEQUENCE}", 7, true, true),
        new BillingProperties.Portal(true, true, true),
        new BillingProperties.Features(true, true, true, true)
    );

    RateLimitingService service = new RateLimitingService(redisTemplate, disabledProperties);
    String ipAddress = "192.168.1.1";

    // When
    boolean result = service.isWithinRateLimit(ipAddress);

    // Then
    assertThat(result).isTrue();
    verifyNoInteractions(zSetOperations);
  }

  @Test
  @DisplayName("Should fail open when Redis returns null")
  void shouldFailOpenWhenRedisReturnsNull() {
    // Given
    String ipAddress = "192.168.1.1";
    when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(null);

    // When
    boolean result = rateLimitingService.isWithinRateLimit(ipAddress);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should fail open when Redis throws exception")
  void shouldFailOpenWhenRedisThrowsException() {
    // Given
    String ipAddress = "192.168.1.1";
    when(zSetOperations.count(anyString(), anyDouble(), anyDouble()))
        .thenThrow(new RuntimeException("Redis connection error"));

    // When
    boolean result = rateLimitingService.isWithinRateLimit(ipAddress);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should calculate remaining attempts correctly")
  void shouldCalculateRemainingAttemptsCorrectly() {
    // Given
    String ipAddress = "192.168.1.1";
    when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(30L);

    // When
    int remaining = rateLimitingService.getRemainingAttempts(ipAddress);

    // Then
    assertThat(remaining).isEqualTo(70); // 100 - 30
  }

  @Test
  @DisplayName("Should return zero remaining attempts when limit exceeded")
  void shouldReturnZeroRemainingAttemptsWhenLimitExceeded() {
    // Given
    String ipAddress = "192.168.1.1";
    when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(150L);

    // When
    int remaining = rateLimitingService.getRemainingAttempts(ipAddress);

    // Then
    assertThat(remaining).isEqualTo(0);
  }

  @Test
  @DisplayName("Should calculate time until reset correctly")
  void shouldCalculateTimeUntilResetCorrectly() {
    // Given
    String ipAddress = "192.168.1.1";
    long oldestRequestTime = System.currentTimeMillis() - 30000; // 30 seconds ago
    when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
        .thenReturn(Set.of(String.valueOf(oldestRequestTime)));

    // When
    Duration timeUntilReset = rateLimitingService.getTimeUntilReset(ipAddress);

    // Then
    assertThat(timeUntilReset.getSeconds()).isGreaterThan(0);
    assertThat(timeUntilReset.getSeconds()).isLessThanOrEqualTo(30);
  }

  @Test
  @DisplayName("Should return zero duration when no requests in window")
  void shouldReturnZeroDurationWhenNoRequestsInWindow() {
    // Given
    String ipAddress = "192.168.1.1";
    when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
        .thenReturn(Set.of());

    // When
    Duration timeUntilReset = rateLimitingService.getTimeUntilReset(ipAddress);

    // Then
    assertThat(timeUntilReset).isEqualTo(Duration.ZERO);
  }

  @Test
  @DisplayName("Should clear rate limit for IP address")
  void shouldClearRateLimitForIpAddress() {
    // Given
    String ipAddress = "192.168.1.1";

    // When
    rateLimitingService.clearRateLimit(ipAddress);

    // Then
    verify(redisTemplate).delete("billing:rate_limit:" + ipAddress);
  }
}
