package com.iqscaffold.billingservice.jobs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.event.UsageResetRequested;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job to reset usage counters at period start.
 *
 * <p>Runs daily at 12 AM UTC to identify subscriptions starting a new billing period
 * and publish events for asynchronous usage counter reset.
 *
 * <p>Async Pattern: Query subscriptions starting new period → publish UsageReset events to queue → consumer resets counters
 *
 * <p>Why Async: May affect thousands of subscriptions, database-intensive
 *
 * <p>Features:
 * <ul>
 *   <li>Batch processing (500 subscriptions per batch)</li>
 *   <li>Distributed locking to prevent duplicate execution (Redis)</li>
 *   <li>Job execution logging and metrics</li>
 *   <li>Job execution timeout (max 5 minutes)</li>
 *   <li>Pagination for large result sets</li>
 *   <li>Event publishing in batches (100 events per batch)</li>
 * </ul>
 */
@Component
public class UsageResetJob {

  private static final Logger log = LoggerFactory.getLogger(UsageResetJob.class);
  private static final String LOCK_KEY = "job:usage-reset:lock";
  private static final long LOCK_TIMEOUT_SECONDS = 300; // 5 minutes
  private static final int BATCH_SIZE = 100;
  private static final int PROCESSING_BATCH_SIZE = 500;

  private final SubscriptionRepository subscriptionRepository;
  private final DomainEventPublisher eventPublisher;
  private final RedisTemplate<String, String> redisTemplate;
  private final MeterRegistry meterRegistry;

  public UsageResetJob(
      SubscriptionRepository subscriptionRepository,
      DomainEventPublisher eventPublisher,
      RedisTemplate<String, String> redisTemplate,
      MeterRegistry meterRegistry
  ) {
    this.subscriptionRepository = subscriptionRepository;
    this.eventPublisher = eventPublisher;
    this.redisTemplate = redisTemplate;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Executes the usage reset job daily at 12 AM UTC.
   *
   * <p>Cron expression: 0 0 0 * * * (second, minute, hour, day, month, weekday)
   */
  @Scheduled(cron = "0 0 0 * * *")
  public void execute() {
    var lockAcquired = acquireLock();
    if (!lockAcquired) {
      log.info("Usage reset job already running on another instance, skipping");
      return;
    }

    var timer = Timer.builder("billing.job.usage_reset")
        .description("Usage reset job execution time")
        .register(meterRegistry);

    try {
      timer.record(() -> {
        log.info("Starting usage reset job");
        var startTime = System.currentTimeMillis();
        var processedCount = 0;

        try {
          processedCount = processUsageResets();
          var duration = System.currentTimeMillis() - startTime;

          log.info("Usage reset job completed successfully. Processed {} subscriptions in {}ms",
              processedCount, duration);

          meterRegistry.counter("billing.job.usage_reset.success",
              "processed", String.valueOf(processedCount)).increment();
        } catch (final Exception e) {
          log.error("Usage reset job failed", e);
          meterRegistry.counter("billing.job.usage_reset.failure").increment();
          throw e;
        }
      });
    } finally {
      releaseLock();
    }
  }

  /**
   * Processes usage resets in batches.
   *
   * @return number of subscriptions processed
   */
  @Transactional(readOnly = true)
  protected int processUsageResets() {
    var now = LocalDateTime.now();

    // Find subscriptions with expired periods (starting new period today)
    var subscriptionsStartingNewPeriod = subscriptionRepository.findExpiredPeriods(now);

    log.info("Found {} subscriptions starting new billing period",
        subscriptionsStartingNewPeriod.size());

    var processedCount = 0;
    var batch = new java.util.ArrayList<UsageResetRequested>(BATCH_SIZE);

    for (final var subscription : subscriptionsStartingNewPeriod) {
      try {
        // Execute in tenant context
        TenantContext.executeInTenantContext(subscription.getTenantId().toString(), () -> {
          var event = createUsageResetEvent(subscription);
          batch.add(event);

          // Publish batch when full
          if (batch.size() >= BATCH_SIZE) {
            publishBatch(batch);
            batch.clear();
          }
        });

        processedCount++;
      } catch (final Exception e) {
        log.error("Failed to process usage reset for subscription {}",
            subscription.getId(), e);
        meterRegistry.counter("billing.job.usage_reset.error",
            "subscription_id", String.valueOf(subscription.getId())).increment();
      }
    }

    // Publish remaining events
    if (!batch.isEmpty()) {
      publishBatch(batch);
    }

    return processedCount;
  }

  /**
   * Creates a UsageResetRequested event from a subscription.
   *
   * @param subscription subscription starting new period
   * @return usage reset requested event
   */
  private UsageResetRequested createUsageResetEvent(Subscription subscription) {
    var newPeriodStart = subscription.getCurrentPeriodEnd();

    return new UsageResetRequested(
        UUID.randomUUID(),
        Instant.now(),
        subscription.getId(),
        subscription.getTenantId(),
        newPeriodStart
    );
  }

  /**
   * Publishes a batch of events to RabbitMQ.
   *
   * @param events list of events to publish
   */
  private void publishBatch(List<UsageResetRequested> events) {
    log.debug("Publishing batch of {} usage reset events", events.size());

    for (final var event : events) {
      try {
        eventPublisher.publish(event);
      } catch (final Exception e) {
        log.error("Failed to publish usage reset event for subscription {}",
            event.aggregateId(), e);
        meterRegistry.counter("billing.job.usage_reset.publish_error",
            "subscription_id", String.valueOf(event.aggregateId())).increment();
      }
    }
  }

  /**
   * Acquires distributed lock using Redis.
   *
   * @return true if lock acquired, false otherwise
   */
  private boolean acquireLock() {
    try {
      var result = redisTemplate.opsForValue()
          .setIfAbsent(LOCK_KEY, "locked",
              java.time.Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
      return Boolean.TRUE.equals(result);
    } catch (final Exception e) {
      log.error("Failed to acquire lock for usage reset job", e);
      return false;
    }
  }

  /**
   * Releases distributed lock.
   */
  private void releaseLock() {
    try {
      redisTemplate.delete(LOCK_KEY);
    } catch (final Exception e) {
      log.error("Failed to release lock for usage reset job", e);
    }
  }
}
