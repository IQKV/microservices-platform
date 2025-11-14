package org.gripday.authservice.domain.service;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

/**
 * Service for tracking email verification metrics. Provides custom metrics for monitoring email verification operations and cleanup tasks.
 */
@Service
public class EmailVerificationMetricsService {

  private final Counter emailsSentCounter;
  private final Counter emailsFailedCounter;
  private final Counter verificationsSuccessfulCounter;
  private final Counter verificationsFailedCounter;
  private final Counter tokensCleanedUpCounter;
  private final Timer emailSendTimer;
  private final Timer cleanupTimer;
  private final AtomicLong expiredTokensGauge;

  public EmailVerificationMetricsService(final MeterRegistry meterRegistry) {
    // Email sending metrics
    this.emailsSentCounter = Counter.builder("gripday_user_email_verification_sent_total")
        .description("Total number of verification emails sent")
        .register(meterRegistry);

    this.emailsFailedCounter = Counter.builder("gripday_user_email_verification_send_failed_total")
        .description("Total number of failed verification email sends")
        .register(meterRegistry);

    // Email verification metrics
    this.verificationsSuccessfulCounter = Counter.builder("gripday_user_email_verification_success_total")
        .description("Total number of successful email verifications")
        .register(meterRegistry);

    this.verificationsFailedCounter = Counter.builder("gripday_user_email_verification_failed_total")
        .description("Total number of failed email verification attempts")
        .register(meterRegistry);

    // Token cleanup metrics
    this.tokensCleanedUpCounter = Counter.builder("gripday_user_email_verification_tokens_cleaned_total")
        .description("Total number of expired verification tokens cleaned up")
        .register(meterRegistry);

    // Timing metrics
    this.emailSendTimer = Timer.builder("gripday_user_email_send_duration_seconds")
        .description("Time taken to send verification emails")
        .register(meterRegistry);

    this.cleanupTimer = Timer.builder("gripday_user_email_verification_cleanup_duration_seconds")
        .description("Time taken to perform token cleanup")
        .register(meterRegistry);

    // Gauge for current expired tokens count
    this.expiredTokensGauge = new AtomicLong(0);
    Gauge.builder("gripday_user_email_verification_expired_tokens_current", expiredTokensGauge, AtomicLong::get)
        .description("Current number of expired verification tokens")
        .register(meterRegistry);
  }

  /**
   * Record a successful email send.
   */
  public void recordEmailSent() {
    emailsSentCounter.increment();
  }

  /**
   * Record a failed email send.
   */
  public void recordEmailSendFailed() {
    emailsFailedCounter.increment();
  }

  /**
   * Record a successful email verification.
   */
  public void recordVerificationSuccess() {
    verificationsSuccessfulCounter.increment();
  }

  /**
   * Record a failed email verification attempt.
   */
  public void recordVerificationFailed() {
    verificationsFailedCounter.increment();
  }

  /**
   * Record tokens cleaned up during cleanup operation.
   *
   * @param count number of tokens cleaned up
   */
  public void recordTokensCleanedUp(long count) {
    tokensCleanedUpCounter.increment(count);
  }

  /**
   * Update the current count of expired tokens.
   *
   * @param count current number of expired tokens
   */
  public void updateExpiredTokensCount(long count) {
    expiredTokensGauge.set(count);
  }

  /**
   * Get a timer sample for email sending operations.
   *
   * @return Timer.Sample for measuring email send duration
   */
  public Timer.Sample startEmailSendTimer() {
    return Timer.start();
  }

  /**
   * Get a timer sample for cleanup operations.
   *
   * @return Timer.Sample for measuring cleanup duration
   */
  public Timer.Sample startCleanupTimer() {
    return Timer.start();
  }

  /**
   * Get the email send timer for stopping timer samples.
   *
   * @return the email send timer
   */
  public Timer getEmailSendTimer() {
    return emailSendTimer;
  }

  /**
   * Get the cleanup timer for stopping timer samples.
   *
   * @return the cleanup timer
   */
  public Timer getCleanupTimer() {
    return cleanupTimer;
  }
}