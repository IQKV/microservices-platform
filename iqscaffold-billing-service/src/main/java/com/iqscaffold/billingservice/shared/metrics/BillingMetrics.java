package com.iqscaffold.billingservice.shared.metrics;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Central metrics service for billing operations.
 *
 * <p>Provides business, operational, and error metrics for monitoring
 * billing service health and performance.
 *
 * <p>Metrics are exposed via Prometheus endpoint at /actuator/prometheus
 */
@Component
public class BillingMetrics {

  private static final Logger log = LoggerFactory.getLogger(BillingMetrics.class);

  private final MeterRegistry meterRegistry;

  // Business Metrics - Gauges for current values
  private final AtomicLong currentMrr = new AtomicLong(0);
  private final AtomicLong currentArr = new AtomicLong(0);
  private final AtomicLong activeSubscriptions = new AtomicLong(0);
  private final AtomicLong trialSubscriptions = new AtomicLong(0);
  private final AtomicLong canceledSubscriptions = new AtomicLong(0);

  // Business Metrics - Counters for events
  private final Counter subscriptionCreatedCounter;
  private final Counter subscriptionUpgradedCounter;
  private final Counter subscriptionDowngradedCounter;
  private final Counter subscriptionCanceledCounter;
  private final Counter trialStartedCounter;
  private final Counter trialConvertedCounter;
  private final Counter trialExpiredCounter;

  // Payment Metrics
  private final Counter paymentSuccessCounter;
  private final Counter paymentFailureCounter;
  private final Counter paymentRefundCounter;
  private final Timer paymentProcessingTimer;

  // Invoice Metrics
  private final Counter invoiceGeneratedCounter;
  private final Counter invoicePaidCounter;
  private final Counter invoiceVoidedCounter;
  private final Timer invoiceGenerationTimer;

  // Usage Metrics
  private final Counter usageRecordedCounter;
  private final Counter quotaExceededCounter;
  private final Timer usageRecordingTimer;

  // Webhook Metrics
  private final Counter webhookReceivedCounter;
  private final Counter webhookProcessedCounter;
  private final Counter webhookFailedCounter;
  private final Timer webhookProcessingTimer;

  // Error Metrics
  private final Counter apiErrorCounter;
  private final Counter databaseErrorCounter;

  public BillingMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;

    // Register business metric gauges
    Gauge.builder("billing.mrr", currentMrr, AtomicLong::get)
        .description("Monthly Recurring Revenue in cents")
        .baseUnit("cents")
        .register(meterRegistry);

    Gauge.builder("billing.arr", currentArr, AtomicLong::get)
        .description("Annual Recurring Revenue in cents")
        .baseUnit("cents")
        .register(meterRegistry);

    Gauge.builder("billing.subscriptions.active", activeSubscriptions, AtomicLong::get)
        .description("Number of active subscriptions")
        .register(meterRegistry);

    Gauge.builder("billing.subscriptions.trial", trialSubscriptions, AtomicLong::get)
        .description("Number of trial subscriptions")
        .register(meterRegistry);

    Gauge.builder("billing.subscriptions.canceled", canceledSubscriptions, AtomicLong::get)
        .description("Number of canceled subscriptions")
        .register(meterRegistry);

    // Initialize subscription counters
    subscriptionCreatedCounter = Counter.builder("billing.subscriptions.created")
        .description("Total subscriptions created")
        .register(meterRegistry);

    subscriptionUpgradedCounter = Counter.builder("billing.subscriptions.upgraded")
        .description("Total subscriptions upgraded")
        .register(meterRegistry);

    subscriptionDowngradedCounter = Counter.builder("billing.subscriptions.downgraded")
        .description("Total subscriptions downgraded")
        .register(meterRegistry);

    subscriptionCanceledCounter = Counter.builder("billing.subscriptions.canceled.total")
        .description("Total subscriptions canceled")
        .register(meterRegistry);

    trialStartedCounter = Counter.builder("billing.trials.started")
        .description("Total trials started")
        .register(meterRegistry);

    trialConvertedCounter = Counter.builder("billing.trials.converted")
        .description("Total trials converted to paid")
        .register(meterRegistry);

    trialExpiredCounter = Counter.builder("billing.trials.expired")
        .description("Total trials expired")
        .register(meterRegistry);

    // Initialize payment counters and timers
    paymentSuccessCounter = Counter.builder("billing.payments.success")
        .description("Total successful payments")
        .register(meterRegistry);

    paymentFailureCounter = Counter.builder("billing.payments.failed")
        .description("Total failed payments")
        .register(meterRegistry);

    paymentRefundCounter = Counter.builder("billing.payments.refunded")
        .description("Total refunded payments")
        .register(meterRegistry);

    paymentProcessingTimer = Timer.builder("billing.payments.processing.time")
        .description("Payment processing duration")
        .register(meterRegistry);

    // Initialize invoice counters and timers
    invoiceGeneratedCounter = Counter.builder("billing.invoices.generated")
        .description("Total invoices generated")
        .register(meterRegistry);

    invoicePaidCounter = Counter.builder("billing.invoices.paid")
        .description("Total invoices paid")
        .register(meterRegistry);

    invoiceVoidedCounter = Counter.builder("billing.invoices.voided")
        .description("Total invoices voided")
        .register(meterRegistry);

    invoiceGenerationTimer = Timer.builder("billing.invoices.generation.time")
        .description("Invoice generation duration")
        .register(meterRegistry);

    // Initialize usage counters and timers
    usageRecordedCounter = Counter.builder("billing.usage.recorded")
        .description("Total usage records created")
        .register(meterRegistry);

    quotaExceededCounter = Counter.builder("billing.quota.exceeded")
        .description("Total quota exceeded events")
        .register(meterRegistry);

    usageRecordingTimer = Timer.builder("billing.usage.recording.time")
        .description("Usage recording duration")
        .register(meterRegistry);

    // Initialize webhook counters and timers
    webhookReceivedCounter = Counter.builder("billing.webhooks.received")
        .description("Total webhooks received")
        .register(meterRegistry);

    webhookProcessedCounter = Counter.builder("billing.webhooks.processed")
        .description("Total webhooks processed successfully")
        .register(meterRegistry);

    webhookFailedCounter = Counter.builder("billing.webhooks.failed")
        .description("Total webhook processing failures")
        .register(meterRegistry);

    webhookProcessingTimer = Timer.builder("billing.webhooks.processing.time")
        .description("Webhook processing duration")
        .register(meterRegistry);

    // Initialize error counters
    apiErrorCounter = Counter.builder("billing.errors.api")
        .description("Total API errors")
        .register(meterRegistry);

    databaseErrorCounter = Counter.builder("billing.errors.database")
        .description("Total database errors")
        .register(meterRegistry);
  }

  // Business Metrics Methods

  /**
   * Update Monthly Recurring Revenue.
   *
   * @param mrrInCents MRR value in cents
   */
  public void updateMrr(long mrrInCents) {
    currentMrr.set(mrrInCents);
    log.debug("Updated MRR to {} cents", mrrInCents);
  }

  /**
   * Update Annual Recurring Revenue.
   *
   * @param arrInCents ARR value in cents
   */
  public void updateArr(long arrInCents) {
    currentArr.set(arrInCents);
    log.debug("Updated ARR to {} cents", arrInCents);
  }

  /**
   * Update active subscriptions count.
   *
   * @param count number of active subscriptions
   */
  public void updateActiveSubscriptions(long count) {
    activeSubscriptions.set(count);
  }

  /**
   * Update trial subscriptions count.
   *
   * @param count number of trial subscriptions
   */
  public void updateTrialSubscriptions(long count) {
    trialSubscriptions.set(count);
  }

  /**
   * Update canceled subscriptions count.
   *
   * @param count number of canceled subscriptions
   */
  public void updateCanceledSubscriptions(long count) {
    canceledSubscriptions.set(count);
  }

  /**
   * Record subscription created event.
   */
  public void recordSubscriptionCreated() {
    subscriptionCreatedCounter.increment();
  }

  /**
   * Record subscription upgraded event.
   */
  public void recordSubscriptionUpgraded() {
    subscriptionUpgradedCounter.increment();
  }

  /**
   * Record subscription downgraded event.
   */
  public void recordSubscriptionDowngraded() {
    subscriptionDowngradedCounter.increment();
  }

  /**
   * Record subscription canceled event.
   */
  public void recordSubscriptionCanceled() {
    subscriptionCanceledCounter.increment();
  }

  /**
   * Record trial started event.
   */
  public void recordTrialStarted() {
    trialStartedCounter.increment();
  }

  /**
   * Record trial converted to paid event.
   */
  public void recordTrialConverted() {
    trialConvertedCounter.increment();
  }

  /**
   * Record trial expired event.
   */
  public void recordTrialExpired() {
    trialExpiredCounter.increment();
  }

  // Payment Metrics Methods

  /**
   * Record successful payment.
   */
  public void recordPaymentSuccess() {
    paymentSuccessCounter.increment();
  }

  /**
   * Record failed payment.
   */
  public void recordPaymentFailure() {
    paymentFailureCounter.increment();
  }

  /**
   * Record refunded payment.
   */
  public void recordPaymentRefund() {
    paymentRefundCounter.increment();
  }

  /**
   * Get payment processing timer.
   *
   * @return timer for payment processing
   */
  public Timer getPaymentProcessingTimer() {
    return paymentProcessingTimer;
  }

  // Invoice Metrics Methods

  /**
   * Record invoice generated event.
   */
  public void recordInvoiceGenerated() {
    invoiceGeneratedCounter.increment();
  }

  /**
   * Record invoice paid event.
   */
  public void recordInvoicePaid() {
    invoicePaidCounter.increment();
  }

  /**
   * Record invoice voided event.
   */
  public void recordInvoiceVoided() {
    invoiceVoidedCounter.increment();
  }

  /**
   * Get invoice generation timer.
   *
   * @return timer for invoice generation
   */
  public Timer getInvoiceGenerationTimer() {
    return invoiceGenerationTimer;
  }

  // Usage Metrics Methods

  /**
   * Record usage recorded event.
   */
  public void recordUsageRecorded() {
    usageRecordedCounter.increment();
  }

  /**
   * Record quota exceeded event.
   */
  public void recordQuotaExceeded() {
    quotaExceededCounter.increment();
  }

  /**
   * Get usage recording timer.
   *
   * @return timer for usage recording
   */
  public Timer getUsageRecordingTimer() {
    return usageRecordingTimer;
  }

  // Webhook Metrics Methods

  /**
   * Record webhook received event.
   */
  public void recordWebhookReceived() {
    webhookReceivedCounter.increment();
  }

  /**
   * Record webhook processed successfully.
   */
  public void recordWebhookProcessed() {
    webhookProcessedCounter.increment();
  }

  /**
   * Record webhook processing failure.
   */
  public void recordWebhookFailed() {
    webhookFailedCounter.increment();
  }

  /**
   * Get webhook processing timer.
   *
   * @return timer for webhook processing
   */
  public Timer getWebhookProcessingTimer() {
    return webhookProcessingTimer;
  }

  // Error Metrics Methods

  /**
   * Record API error.
   *
   * @param endpoint the endpoint where error occurred
   */
  public void recordApiError(String endpoint) {
    apiErrorCounter.increment();
    Counter.builder("billing.errors.api.by.endpoint")
        .tag("endpoint", endpoint)
        .description("API errors by endpoint")
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record database error.
   */
  public void recordDatabaseError() {
    databaseErrorCounter.increment();
  }

  /**
   * Record custom counter metric.
   *
   * @param name metric name
   * @param tags metric tags
   */
  public void recordCounter(String name, String... tags) {
    Counter.builder(name)
        .tags(tags)
        .register(meterRegistry)
        .increment();
  }

  /**
   * Record custom gauge metric.
   *
   * @param name  metric name
   * @param value metric value
   * @param tags  metric tags
   */
  public void recordGauge(String name, Number value, String... tags) {
    Gauge.builder(name, value, Number::doubleValue)
        .tags(tags)
        .register(meterRegistry);
  }
}
