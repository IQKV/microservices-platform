package org.gripday.userservice.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for implementing rate limiting using Redis. Provides IP-based rate limiting for authentication endpoints.
 */
@Service
public class RateLimitingService {

  private final RedisTemplate<String, String> redisTemplate;

  // Rate limiting configuration
  private static final int MAX_ATTEMPTS_PER_MINUTE = 5;
  private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
  private static final String RATE_LIMIT_PREFIX = "rate_limit:";

  public RateLimitingService(final RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Check if the IP address is within rate limits. Uses sliding window approach with Redis.
   */
  public boolean isWithinRateLimit(String ipAddress) {
    var key = RATE_LIMIT_PREFIX + ipAddress;
    var currentTime = Instant.now().toEpochMilli();
    var windowStart = currentTime - RATE_LIMIT_WINDOW.toMillis();

    try {
      // Remove old entries outside the window
      redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

      // Count current requests in the window
      var currentCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);

      // Null check for count result
      if (currentCount == null) {
        return true; // Fail open if Redis returns null
      }

      if (currentCount >= MAX_ATTEMPTS_PER_MINUTE) {
        return false;
      }

      // Add current request to the window
      redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);

      // Set expiration for the key
      redisTemplate.expire(key, RATE_LIMIT_WINDOW.toSeconds(), TimeUnit.SECONDS);

      return true;

    } catch (final Exception e) {
      // If Redis is unavailable, allow the request (fail open)
      System.err.println("Rate limiting error: " + e.getMessage());
      return true;
    }
  }

  /**
   * Get remaining attempts for an IP address.
   */
  public int getRemainingAttempts(String ipAddress) {
    var key = RATE_LIMIT_PREFIX + ipAddress;
    var currentTime = Instant.now().toEpochMilli();
    var windowStart = currentTime - RATE_LIMIT_WINDOW.toMillis();

    try {
      var currentCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);

      // Null check for count result
      if (currentCount == null) {
        return MAX_ATTEMPTS_PER_MINUTE;
      }

      return Math.max(0, MAX_ATTEMPTS_PER_MINUTE - currentCount.intValue());
    } catch (final Exception e) {
      return MAX_ATTEMPTS_PER_MINUTE;
    }
  }

  /**
   * Get time until rate limit resets for an IP address.
   */
  public Duration getTimeUntilReset(String ipAddress) {
    var key = RATE_LIMIT_PREFIX + ipAddress;
    var currentTime = Instant.now().toEpochMilli();
    var windowStart = currentTime - RATE_LIMIT_WINDOW.toMillis();

    try {
      // Get the oldest request in the current window
      var oldestRequests = redisTemplate.opsForZSet().rangeByScore(key, windowStart, currentTime, 0, 1);

      // Null check for rangeByScore result
      if (oldestRequests == null || oldestRequests.isEmpty()) {
        return Duration.ZERO;
      }

      var oldestRequestTime = Long.parseLong(oldestRequests.iterator().next());
      var resetTime = oldestRequestTime + RATE_LIMIT_WINDOW.toMillis();
      var timeUntilReset = resetTime - currentTime;

      return Duration.ofMillis(Math.max(0, timeUntilReset));

    } catch (final Exception e) {
      return Duration.ZERO;
    }
  }

  /**
   * Clear rate limit for an IP address (for testing or admin purposes).
   */
  public void clearRateLimit(String ipAddress) {
    var key = RATE_LIMIT_PREFIX + ipAddress;
    redisTemplate.delete(key);
  }
}
