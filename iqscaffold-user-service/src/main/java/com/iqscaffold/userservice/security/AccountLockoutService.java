package com.iqscaffold.userservice.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for managing account lockouts after failed login attempts. Uses Redis for distributed lockout state management.
 */
@Service
public class AccountLockoutService {

  private final RedisTemplate<String, String> redisTemplate;

  // Lockout configuration
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
  private static final Duration FAILED_ATTEMPTS_WINDOW = Duration.ofMinutes(30);
  private static final String FAILED_ATTEMPTS_PREFIX = "failed_attempts:";
  private static final String LOCKOUT_PREFIX = "lockout:";

  public AccountLockoutService(final RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Record a failed login attempt for a user. Returns true if the account should be locked.
   */
  public boolean recordFailedAttempt(String username) {
    var failedAttemptsKey = FAILED_ATTEMPTS_PREFIX + username.toLowerCase(java.util.Locale.ROOT);
    var lockoutKey = LOCKOUT_PREFIX + username.toLowerCase(java.util.Locale.ROOT);

    try {
      // Check if account is already locked
      if (isAccountLocked(username)) {
        return true;
      }

      // Increment failed attempts counter
      var failedAttempts = redisTemplate.opsForValue().increment(failedAttemptsKey);

      // Null check for increment result
      if (failedAttempts == null) {
        return false;
      }

      // Set expiration for failed attempts counter
      redisTemplate.expire(failedAttemptsKey, FAILED_ATTEMPTS_WINDOW.toSeconds(), TimeUnit.SECONDS);

      // Check if we've reached the lockout threshold
      if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
        // Lock the account
        var lockoutExpiry = Instant.now().plus(LOCKOUT_DURATION).toEpochMilli();
        redisTemplate.opsForValue().set(lockoutKey, String.valueOf(lockoutExpiry),
            LOCKOUT_DURATION.toSeconds(), TimeUnit.SECONDS);

        // Clear failed attempts counter since account is now locked
        redisTemplate.delete(failedAttemptsKey);

        return true;
      }

      return false;

    } catch (final Exception e) {
      // If Redis is unavailable, don't lock accounts (fail open for availability)
      System.err.println("Account lockout error: " + e.getMessage());
      return false;
    }
  }

  /**
   * Check if an account is currently locked.
   */
  public boolean isAccountLocked(String username) {
    var lockoutKey = LOCKOUT_PREFIX + username.toLowerCase(java.util.Locale.ROOT);

    try {
      var lockoutExpiryStr = redisTemplate.opsForValue().get(lockoutKey);

      if (lockoutExpiryStr == null) {
        return false;
      }

      var lockoutExpiry = Long.parseLong(lockoutExpiryStr);
      var currentTime = Instant.now().toEpochMilli();

      if (currentTime >= lockoutExpiry) {
        // Lockout has expired, clean up
        redisTemplate.delete(lockoutKey);
        return false;
      }

      return true;

    } catch (final Exception e) {
      // If Redis is unavailable, assume account is not locked
      return false;
    }
  }

  /**
   * Clear failed attempts for a user (called on successful login).
   */
  public void clearFailedAttempts(String username) {
    var failedAttemptsKey = FAILED_ATTEMPTS_PREFIX + username.toLowerCase(java.util.Locale.ROOT);

    try {
      redisTemplate.delete(failedAttemptsKey);
    } catch (final Exception e) {
      System.err.println("Error clearing failed attempts: " + e.getMessage());
    }
  }

  /**
   * Get the number of failed attempts for a user.
   */
  public int getFailedAttempts(String username) {
    var failedAttemptsKey = FAILED_ATTEMPTS_PREFIX + username.toLowerCase(java.util.Locale.ROOT);

    try {
      var failedAttemptsStr = redisTemplate.opsForValue().get(failedAttemptsKey);
      return failedAttemptsStr != null ? Integer.parseInt(failedAttemptsStr) : 0;
    } catch (final Exception e) {
      return 0;
    }
  }

  /**
   * Get remaining time until account unlock.
   */
  public Duration getTimeUntilUnlock(String username) {
    var lockoutKey = LOCKOUT_PREFIX + username.toLowerCase(java.util.Locale.ROOT);

    try {
      var lockoutExpiryStr = redisTemplate.opsForValue().get(lockoutKey);

      if (lockoutExpiryStr == null) {
        return Duration.ZERO;
      }

      var lockoutExpiry = Long.parseLong(lockoutExpiryStr);
      var currentTime = Instant.now().toEpochMilli();
      var remainingTime = lockoutExpiry - currentTime;

      return Duration.ofMillis(Math.max(0, remainingTime));

    } catch (final Exception e) {
      return Duration.ZERO;
    }
  }

  /**
   * Manually unlock an account (for admin purposes).
   */
  public void unlockAccount(String username) {
    var failedAttemptsKey = FAILED_ATTEMPTS_PREFIX + username.toLowerCase(java.util.Locale.ROOT);
    var lockoutKey = LOCKOUT_PREFIX + username.toLowerCase(java.util.Locale.ROOT);

    try {
      redisTemplate.delete(failedAttemptsKey);
      redisTemplate.delete(lockoutKey);
    } catch (final Exception e) {
      System.err.println("Error unlocking account: " + e.getMessage());
    }
  }
}