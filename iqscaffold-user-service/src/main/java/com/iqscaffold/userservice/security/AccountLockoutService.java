package com.iqscaffold.userservice.security;

import java.time.Duration;

/**
 * Interface for account lockout service providing distributed brute force protection.
 *
 * <p>This interface defines the contract for account lockout operations including:
 * <ul>
 *   <li>Failed attempt tracking and recording</li>
 *   <li>Account lockout status checking</li>
 *   <li>Automatic lockout triggering</li>
 *   <li>Failed attempt clearing on successful authentication</li>
 *   <li>Manual account unlocking for administrative purposes</li>
 * </ul>
 *
 * <h4>Security Features:</h4>
 * <ul>
 *   <li><strong>Brute Force Protection</strong> - Prevents rapid password guessing</li>
 *   <li><strong>Distributed Tracking</strong> - Redis-based tracking across instances</li>
 *   <li><strong>Progressive Lockout</strong> - Configurable thresholds and durations</li>
 *   <li><strong>Automatic Cleanup</strong> - TTL-based cleanup prevents memory leaks</li>
 * </ul>
 */
public interface AccountLockoutService {

  /**
   * Record a failed authentication attempt and determine if account should be locked.
   *
   * @param username The username for which to record the failed attempt
   * @return true if the account should be locked due to exceeding the threshold
   */
  boolean recordFailedAttempt(String username);

  /**
   * Check if an account is currently locked.
   *
   * @param username The username to check for lockout status
   * @return true if the account is currently locked
   */
  boolean isAccountLocked(String username);

  /**
   * Clear failed attempts for a user (called on successful login).
   *
   * @param username The username for which to clear failed attempts
   */
  void clearFailedAttempts(String username);

  /**
   * Get the number of failed attempts for a user.
   *
   * @param username The username to check
   * @return The number of failed attempts recorded
   */
  int getFailedAttempts(String username);

  /**
   * Get remaining time until account unlock.
   *
   * @param username The username to check
   * @return Duration until unlock, or Duration.ZERO if not locked
   */
  Duration getTimeUntilUnlock(String username);

  /**
   * Manually unlock an account (for admin purposes).
   *
   * @param username The username to unlock
   */
  void unlockAccount(String username);
}
