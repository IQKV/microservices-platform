package com.iqscaffold.userservice.emailverification;

import java.time.LocalDateTime;

import com.iqscaffold.userservice.shared.exception.EmailVerificationException;
import com.iqscaffold.userservice.tenancy.TenantContext;
import com.iqscaffold.userservice.tenancy.TenantRepository;
import com.iqscaffold.userservice.tenancy.TenantStatus;
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

  private final VerificationTokenRepository tokenRepository;
  private final VerificationMetrics metricsService;
  private final TenantRepository tenantRepository;

  public EmailVerificationTokenCleanupService(final VerificationTokenRepository tokenRepository,
                                              final VerificationMetrics metricsService,
                                              final TenantRepository tenantRepository) {
    this.tokenRepository = tokenRepository;
    this.metricsService = metricsService;
    this.tenantRepository = tenantRepository;
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

      var tenants = tenantRepository.findByStatus(TenantStatus.ACTIVE);
      long totalDeleted = 0L;
      long totalExpired = 0L;

      for (final var tenant : tenants) {
        var perTenant = TenantContext.executeInTenantContext(tenant.getTenantId(), () -> {
          var expiredCount = tokenRepository.countByExpiresAtBefore(cutoffTime);
          metricsService.updateExpiredTokensCount(expiredCount);
          var deletedCount = tokenRepository.deleteByExpiresAtBefore(cutoffTime);
          metricsService.recordTokensCleanedUp(deletedCount);
          return new long[] {expiredCount, deletedCount};
        });
        totalExpired += perTenant[0];
        totalDeleted += perTenant[1];
      }

      logger.info("Cleanup completed. Tenants processed: {}, expired tokens counted: {}, deleted: {}",
          tenants.size(), totalExpired, totalDeleted);

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

      var tenants = tenantRepository.findByStatus(TenantStatus.ACTIVE);
      long totalDeleted = 0L;

      for (final var tenant : tenants) {
        var deletedCount = TenantContext.executeInTenantContext(tenant.getTenantId(), () -> {
          var expiredCount = tokenRepository.countByExpiresAtBefore(cutoffTime);
          metricsService.updateExpiredTokensCount(expiredCount);
          var deleted = tokenRepository.deleteByExpiresAtBefore(cutoffTime);
          metricsService.recordTokensCleanedUp(deleted);
          return deleted;
        });
        totalDeleted += deletedCount;
      }

      logger.info("Manual cleanup completed. Tenants processed: {}, total deleted: {}",
          tenants.size(), totalDeleted);

      return totalDeleted;

    } catch (final Exception e) {
      logger.error("Error during manual cleanup of expired verification tokens", e);
      throw new EmailVerificationException("Failed to perform manual cleanup", e);
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
      var tenants = tenantRepository.findByStatus(TenantStatus.ACTIVE);
      long total = 0L;
      for (final var tenant : tenants) {
        var count = TenantContext.executeInTenantContext(tenant.getTenantId(), () ->
            tokenRepository.countByExpiresAtBefore(cutoffTime)
        );
        total += count;
      }
      return total;

    } catch (final Exception e) {
      logger.error("Error counting expired verification tokens", e);
      return 0;
    }
  }
}
