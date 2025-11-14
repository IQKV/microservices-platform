package org.gripday.userservice.domain.service;

import java.time.LocalDateTime;

import org.gripday.userservice.infrastructure.repository.EmailVerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for scheduled cleanup of expired email verification tokens. Runs daily to remove tokens older than 48 hours to maintain database hygiene.
 */
@Service
public class EmailVerificationTokenCleanupService {

  private static final Logger logger = LoggerFactory.getLogger(EmailVerificationTokenCleanupService.class);

  private final EmailVerificationTokenRepository tokenRepository;
  private final EmailVerificationMetricsService metricsService;

  public EmailVerificationTokenCleanupService(final EmailVerificationTokenRepository tokenRepository,
      final EmailVerificationMetricsService metricsService) {
    this.tokenRepository = tokenRepository;
    this.metricsService = metricsService;
  }

  /**
   * Scheduled cleanup task that runs daily at 2:00 AM. Removes all verification tokens that are older than 48 hours. Uses cron expression: "0 0 2 * * ?" (second, minute, hour, day, month,
   * weekday)
   */
  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void cleanupExpiredTokens() {
    logger.info("Starting scheduled cleanup of expired email verification tokens");

    var timerSample = metricsService.startCleanupTimer();

    try {
      var cutoffTime = LocalDateTime.now().minusHours(48);

      // Count expired tokens before cleanup for metrics
      var expiredCount = tokenRepository.countByExpiresAtBefore(cutoffTime);
      metricsService.updateExpiredTokensCount(expiredCount);

      // Perform cleanup
      var deletedCount = tokenRepository.deleteByExpiresAtBefore(cutoffTime);

      // Record metrics
      metricsService.recordTokensCleanedUp(deletedCount);

      logger.info("Cleanup completed successfully. Deleted {} expired verification tokens older than {}",
          deletedCount, cutoffTime);

    } catch (final Exception e) {
      logger.error("Error during scheduled cleanup of expired verification tokens", e);
      // Don't rethrow - we don't want to break the scheduler
    } finally {
      timerSample.stop(metricsService.getCleanupTimer());
    }
  }

  /**
   * Manual cleanup method that can be called programmatically. Useful for testing or manual maintenance operations.
   *
   * @return number of tokens deleted
   */
  @Transactional
  public long performManualCleanup() {
    logger.info("Performing manual cleanup of expired email verification tokens");

    var timerSample = metricsService.startCleanupTimer();

    try {
      var cutoffTime = LocalDateTime.now().minusHours(48);

      // Count expired tokens before cleanup for metrics
      var expiredCount = tokenRepository.countByExpiresAtBefore(cutoffTime);
      metricsService.updateExpiredTokensCount(expiredCount);

      // Perform cleanup
      var deletedCount = tokenRepository.deleteByExpiresAtBefore(cutoffTime);

      // Record metrics
      metricsService.recordTokensCleanedUp(deletedCount);

      logger.info("Manual cleanup completed successfully. Deleted {} expired verification tokens older than {}",
          deletedCount, cutoffTime);

      return deletedCount;

    } catch (final Exception e) {
      logger.error("Error during manual cleanup of expired verification tokens", e);
      throw new RuntimeException("Failed to perform manual cleanup", e);
    } finally {
      timerSample.stop(metricsService.getCleanupTimer());
    }
  }

  /**
   * Get count of expired tokens without deleting them. Useful for monitoring and metrics.
   *
   * @return number of expired tokens
   */
  @Transactional(readOnly = true)
  public long getExpiredTokenCount() {
    try {
      var cutoffTime = LocalDateTime.now().minusHours(48);
      return tokenRepository.countByExpiresAtBefore(cutoffTime);

    } catch (final Exception e) {
      logger.error("Error counting expired verification tokens", e);
      return 0;
    }
  }
}