package com.iqscaffold.billingservice.security;

import com.iqscaffold.billingservice.config.BillingProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for implementing rate limiting using Redis.
 * 
 * <p>Provides IP-based rate limiting for public endpoints using a sliding window
 * approach with Redis sorted sets. This prevents abuse of public APIs while
 * allowing legitimate traffic.
 * 
 * <p>Configuration is driven by {@link BillingProperties.Security.RateLimiting}.
 */
@Service
public class RateLimitingService {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

  private static final String RATE_LIMIT_PREFIX = "billing:rate_limit:";

  private final RedisTemplate<String, String> redisTemplate;
  private final BillingProperties billingProperties;

  public RateLimitingService(
      RedisTemplate<String, String> redisTemplate,
      BillingProperties billingProperties
  ) {
    this.redisTemplate = redisTemplate;
    this.billingProperties = billingProperties;
  }

  /**
   * Check if the IP address is within rate limits.
   * 
   * <p>Uses sliding window approach with Redis sorted sets to track requests
   * over time. Old requests outside the window are automatically removed.
   * 
   * @param ipAddress the client IP address
   * @return true if within rate limit, false if exceeded
   */
  public boolean isWithinRateLimit(String ipAddress) {
    if (!billingProperties.security().rateLimiting().enabled()) {
      return true;
    }

    String key = RATE_LIMIT_PREFIX + ipAddress;
    long currentTime = Instant.now().toEpochMilli();
    long windowStart = currentTime - Duration.ofMinutes(1).toMillis();

    try {
      // Remove old entries outside the window
      redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

      // Count current requests in the window
      Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);

      // Null check for count result
      if (currentCount == null) {
        logger.warn("Redis returned null count for rate limit key: {}", key);
        return true; // Fail open if Redis returns null
      }

      int maxRequests = billingProperties.security().rateLimiting().requestsPerMinute();
      
      if (currentCount >= maxRequests) {
        logger.warn("Rate limit exceeded for IP: {} (count: {}, limit: {})", 
            ipAddress, currentCount, maxRequests);
        return false;
      }

      // Add current request to the window
      redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);

      // Set expiration for the key (2 minutes to allow for window cleanup)
      redisTemplate.expire(key, 2, TimeUnit.MINUTES);

      return true;

    } catch (Exception e) {
      // If Redis is unavailable, allow the request (fail open)
      logger.error("Rate limiting error for IP {}: {}", ipAddress, e.getMessage(), e);
      return true;
    }
  }

  /**
   * Get remaining attempts for an IP address.
   * 
   * @param ipAddress the client IP address
   * @return the number of remaining requests allowed in the current window
   */
  public int getRemainingAttempts(String ipAddress) {
    if (!billingProperties.security().rateLimiting().enabled()) {
      return billingProperties.security().rateLimiting().requestsPerMinute();
    }

    String key = RATE_LIMIT_PREFIX + ipAddress;
    long currentTime = Instant.now().toEpochMilli();
    long windowStart = currentTime - Duration.ofMinutes(1).toMillis();

    try {
      Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);

      // Null check for count result
      if (currentCount == null) {
        return billingProperties.security().rateLimiting().requestsPerMinute();
      }

      int maxRequests = billingProperties.security().rateLimiting().requestsPerMinute();
      return Math.max(0, maxRequests - currentCount.intValue());
      
    } catch (Exception e) {
      logger.error("Error getting remaining attempts for IP {}: {}", ipAddress, e.getMessage());
      return billingProperties.security().rateLimiting().requestsPerMinute();
    }
  }

  /**
   * Get time until rate limit resets for an IP address.
   * 
   * @param ipAddress the client IP address
   * @return the duration until the rate limit window resets
   */
  public Duration getTimeUntilReset(String ipAddress) {
    String key = RATE_LIMIT_PREFIX + ipAddress;
    long currentTime = Instant.now().toEpochMilli();
    long windowStart = currentTime - Duration.ofMinutes(1).toMillis();

    try {
      // Get the oldest request in the current window
      var oldestRequests = redisTemplate.opsForZSet()
          .rangeByScore(key, windowStart, currentTime, 0, 1);

      // Null check for rangeByScore result
      if (oldestRequests == null || oldestRequests.isEmpty()) {
        return Duration.ZERO;
      }

      long oldestRequestTime = Long.parseLong(oldestRequests.iterator().next());
      long resetTime = oldestRequestTime + Duration.ofMinutes(1).toMillis();
      long timeUntilReset = resetTime - currentTime;

      return Duration.ofMillis(Math.max(0, timeUntilReset));

    } catch (Exception e) {
      logger.error("Error getting time until reset for IP {}: {}", ipAddress, e.getMessage());
      return Duration.ZERO;
    }
  }

  /**
   * Clear rate limit for an IP address.
   * 
   * <p>This is useful for testing or administrative purposes.
   * 
   * @param ipAddress the client IP address
   */
  public void clearRateLimit(String ipAddress) {
    String key = RATE_LIMIT_PREFIX + ipAddress;
    redisTemplate.delete(key);
    logger.info("Cleared rate limit for IP: {}", ipAddress);
  }
}
