package com.iqscaffold.userservice.config;

import java.time.Instant;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled cleanup service for expired tokens and sessions.
 * Prevents Redis memory bloat by removing stale data.
 */
@Service
@ConditionalOnBean(RedisTemplate.class)
public class RedisTokenCleanupService {

  private static final Logger logger = LoggerFactory.getLogger(RedisTokenCleanupService.class);

  private final RedisTemplate<String, String> redisTemplate;
  private final MeterRegistry meterRegistry;

  public RedisTokenCleanupService(
      @org.springframework.beans.factory.annotation.Autowired(required = false) final RedisTemplate<String, String> redisTemplate,
      final MeterRegistry meterRegistry) {
    this.redisTemplate = redisTemplate;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Clean up expired tokens and sessions daily at 2 AM.
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void cleanupExpiredTokens() {
    if (redisTemplate == null) {
      logger.debug("Redis not configured, skipping token cleanup");
      return;
    }

    logger.info("Starting token cleanup job");
    var startTime = Instant.now();

    try {
      var refreshTokensRemoved = cleanupExpiredRefreshTokens();
      var revocationRecordsRemoved = cleanupOldRevocationRecords();
      var sessionsRemoved = cleanupExpiredSessions();
      var blacklistEntriesRemoved = cleanupExpiredBlacklistEntries();

      var duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();

      logger.info("Token cleanup completed in {}ms. Removed: {} refresh tokens, {} revocation records, {} sessions, {} blacklist entries",
          duration, refreshTokensRemoved, revocationRecordsRemoved, sessionsRemoved, blacklistEntriesRemoved);

      // Record metrics
      meterRegistry.counter("token.cleanup.refresh_tokens", "status", "removed").increment(refreshTokensRemoved);
      meterRegistry.counter("token.cleanup.revocations", "status", "removed").increment(revocationRecordsRemoved);
      meterRegistry.counter("token.cleanup.sessions", "status", "removed").increment(sessionsRemoved);
      meterRegistry.counter("token.cleanup.blacklist", "status", "removed").increment(blacklistEntriesRemoved);
      meterRegistry.timer("token.cleanup.duration").record(java.time.Duration.ofMillis(duration));

    } catch (final Exception e) {
      logger.error("Token cleanup job failed", e);
      meterRegistry.counter("token.cleanup.errors").increment();
    }
  }

  /**
   * Remove expired refresh tokens (older than 7 days).
   */
  private long cleanupExpiredRefreshTokens() {
    var pattern = "refresh:token:*";
    var keys = redisTemplate.keys(pattern);

    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;

    for (final var key : keys) {
      try {
        // Check if token is expired
        var ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl < 0) {
          redisTemplate.delete(key);
          removed++;
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup refresh token: {}", key, e);
      }
    }

    return removed;
  }

  /**
   * Remove old revocation records (older than 30 days).
   */
  private long cleanupOldRevocationRecords() {
    var pattern = "revoked:refresh:*";
    var keys = redisTemplate.keys(pattern);

    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;
    var cutoffTime = Instant.now().minusSeconds(30 * 24 * 60 * 60); // 30 days

    for (final var key : keys) {
      try {
        var revokedAtStr = redisTemplate.opsForValue().get(key);
        if (revokedAtStr != null) {
          var revokedAt = Instant.ofEpochSecond(Long.parseLong(revokedAtStr));
          if (revokedAt.isBefore(cutoffTime)) {
            redisTemplate.delete(key);
            removed++;
          }
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup revocation record: {}", key, e);
      }
    }

    return removed;
  }

  /**
   * Remove expired sessions.
   */
  private long cleanupExpiredSessions() {
    var pattern = "session:*";
    var keys = redisTemplate.keys(pattern);

    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;

    for (final var key : keys) {
      try {
        var ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl < 0) {
          redisTemplate.delete(key);
          removed++;
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup session: {}", key, e);
      }
    }

    return removed;
  }

  /**
   * Remove expired blacklist entries (already handled by TTL, but double-check).
   */
  private long cleanupExpiredBlacklistEntries() {
    var pattern = "blacklist:token:*";
    var keys = redisTemplate.keys(pattern);

    if (keys == null || keys.isEmpty()) {
      return 0;
    }

    var removed = 0L;

    for (final var key : keys) {
      try {
        var ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl < 0) {
          redisTemplate.delete(key);
          removed++;
        }
      } catch (final Exception e) {
        logger.warn("Failed to cleanup blacklist entry: {}", key, e);
      }
    }

    return removed;
  }
}
