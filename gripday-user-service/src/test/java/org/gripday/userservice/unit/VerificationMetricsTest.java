package org.gripday.userservice.unit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.gripday.userservice.domain.service.EmailVerificationMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EmailVerificationMetricsService. Tests metrics recording and timer functionality.
 */
class VerificationMetricsTest {

  private MeterRegistry meterRegistry;
  private EmailVerificationMetricsService metricsService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metricsService = new EmailVerificationMetricsService(meterRegistry);
  }

  @Test
  void shouldRecordEmailSentMetrics() {
    // When
    metricsService.recordEmailSent();
    metricsService.recordEmailSent();

    // Then
    var counter = meterRegistry.get("gripday_user_email_verification_sent_total").counter();
    assertThat(counter.count()).isEqualTo(2.0);
  }

  @Test
  void shouldRecordEmailSendFailedMetrics() {
    // When
    metricsService.recordEmailSendFailed();

    // Then
    var counter = meterRegistry.get("gripday_user_email_verification_send_failed_total").counter();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordVerificationSuccessMetrics() {
    // When
    metricsService.recordVerificationSuccess();
    metricsService.recordVerificationSuccess();
    metricsService.recordVerificationSuccess();

    // Then
    var counter = meterRegistry.get("gripday_user_email_verification_success_total").counter();
    assertThat(counter.count()).isEqualTo(3.0);
  }

  @Test
  void shouldRecordVerificationFailedMetrics() {
    // When
    metricsService.recordVerificationFailed();

    // Then
    var counter = meterRegistry.get("gripday_user_email_verification_failed_total").counter();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordTokensCleanedUpMetrics() {
    // When
    metricsService.recordTokensCleanedUp(5);
    metricsService.recordTokensCleanedUp(3);

    // Then
    var counter = meterRegistry.get("gripday_user_email_verification_tokens_cleaned_total").counter();
    assertThat(counter.count()).isEqualTo(8.0);
  }

  @Test
  void shouldUpdateExpiredTokensGauge() {
    // When
    metricsService.updateExpiredTokensCount(10);

    // Then
    var gauge = meterRegistry.get("gripday_user_email_verification_expired_tokens_current").gauge();
    assertThat(gauge.value()).isEqualTo(10.0);

    // When updated again
    metricsService.updateExpiredTokensCount(5);

    // Then
    assertThat(gauge.value()).isEqualTo(5.0);
  }

  @Test
  void shouldCreateEmailSendTimer() {
    // When
    var timerSample = metricsService.startEmailSendTimer();

    // Simulate some work
    try {
      Thread.sleep(10);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    var timer = meterRegistry.get("gripday_user_email_send_duration_seconds").timer();
    timerSample.stop(timer);

    // Then
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
  }

  @Test
  void shouldCreateCleanupTimer() {
    // When
    var timerSample = metricsService.startCleanupTimer();

    // Simulate some work
    try {
      Thread.sleep(10);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    var timer = meterRegistry.get("gripday_user_email_verification_cleanup_duration_seconds").timer();
    timerSample.stop(timer);

    // Then
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
  }

  @Test
  void shouldHaveCorrectMetricNames() {
    // When - trigger all metrics
    metricsService.recordEmailSent();
    metricsService.recordEmailSendFailed();
    metricsService.recordVerificationSuccess();
    metricsService.recordVerificationFailed();
    metricsService.recordTokensCleanedUp(1);
    metricsService.updateExpiredTokensCount(1);

    var emailTimer = metricsService.startEmailSendTimer();
    emailTimer.stop(metricsService.getEmailSendTimer());

    var cleanupTimer = metricsService.startCleanupTimer();
    cleanupTimer.stop(metricsService.getCleanupTimer());

    // Then - verify all expected metrics exist
    assertThat(meterRegistry.getMeters()).hasSize(8);

    // Verify metric names
    assertThat(meterRegistry.get("gripday_user_email_verification_sent_total")).isNotNull();
    assertThat(meterRegistry.get("gripday_user_email_verification_send_failed_total")).isNotNull();
    assertThat(meterRegistry.get("gripday_user_email_verification_success_total")).isNotNull();
    assertThat(meterRegistry.get("gripday_user_email_verification_failed_total")).isNotNull();
    assertThat(meterRegistry.get("gripday_user_email_verification_tokens_cleaned_total")).isNotNull();
    assertThat(meterRegistry.get("gripday_user_email_verification_expired_tokens_current")).isNotNull();
    assertThat(meterRegistry.get("gripday_user_email_send_duration_seconds")).isNotNull();
    assertThat(meterRegistry.get("gripday_user_email_verification_cleanup_duration_seconds")).isNotNull();
  }
}
