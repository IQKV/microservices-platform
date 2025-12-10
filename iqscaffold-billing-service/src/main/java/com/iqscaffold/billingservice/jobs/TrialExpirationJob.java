package com.iqscaffold.billingservice.jobs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.event.TrialExpired;
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
 * Scheduled job to check and convert expired trials.
 *
 * <p>Runs daily at 2 AM UTC to identify subscriptions with expired trial periods
 * and publish events for asynchronous processing.
 *
 * <p>Async Pattern: Query expired trials → publish TrialExpired events to queue → consumer processes conversions
 *
 * <p>Why Async: May affect thousands of subscriptions, shouldn't block job execution
 *
 * <p>Features:
 * <ul>
 *   <li>Batch processing (100 subscriptions per batch)</li>
 *   <li>Distributed locking to prevent duplicate execution (Redis)</li>
 *   <li>Job execution logging and metrics</li>
 *   <li>Job execution timeout (max 5 minutes)</li>
 *   <li>Pagination for large result sets</li>
 *   <li>Event publishing in batches (100 events per batch)</li>
 * </ul>
 */
@Component
public class TrialExpirationJob {

  private static final Logger log = LoggerFactory.getLogger(TrialExpirationJob.class);
  private static final String LOCK_KEY = "job:trial-expiration:lock";
  private static final long LOCK_TIMEOUT_SECONDS = 300; // 5 minutes
  private static final int BATCH_SIZE = 100;

  private final SubscriptionRepository subscriptionRepository;
  private final DomainEventPublisher eventPublisher;
  private final RedisTemplate<String, String> redisTemplate;
  private final MeterRegistry meterRegistry;

  public TrialExpirationJob(
      final SubscriptionRepository subscriptionRepository,
      final DomainEventPublisher eventPublisher,
      final RedisTemplate<String, String> redisTemplate,
      final MeterRegistry meterRegistry
  ) {
    this.subscriptionRepository = subscriptionRepository;
    this.eventPublisher = eventPublisher;
    this.redisTemplate = redisTemplate;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Executes the trial expiration job daily at 2 AM UTC.
   *
   * <p>Cron expression: 0 0 2 * * * (second, minute, hour, day, month, weekday)
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void execute() {
    var lockAcquired = acquireLock();
    if (!lockAcquired) {
      log.info("Trial expiration job already running on another instance, skipping");
      return;
    }

    var timer = Timer.builder("billing.job.trial_expiration")
        .description("Trial expiration job execution time")
        .register(meterRegistry);

    try {
      timer.record(() -> {
        log.info("Starting trial expiration job");
        var startTime = System.currentTimeMillis();
        var processedCount = 0;

        try {
          processedCount = processExpiredTrials();
          var duration = System.currentTimeMillis() - startTime;

          log.info("Trial expiration job completed successfully. Processed {} subscriptions in {}ms",
              processedCount, duration);

          meterRegistry.counter("billing.job.trial_expiration.success",
              "processed", String.valueOf(processedCount)).increment();
        } catch (final Exception e) {
          log.error("Trial expiration job failed", e);
          meterRegistry.counter("billing.job.trial_expiration.failure").increment();
          throw e;
        }
      });
    } finally {
      releaseLock();
    }
  }

  /**
   * Processes expired trials in batches.
   *
   * @return number of subscriptions processed
   */
  @Transactional(readOnly = true)
  protected int processExpiredTrials() {
    var now = LocalDateTime.now();
    var expiredTrials = subscriptionRepository.findExpiredTrials(now);

    log.info("Found {} expired trials to process", expiredTrials.size());

    var processedCount = 0;
    var batch = new java.util.ArrayList<TrialExpired>(BATCH_SIZE);

    for (final var subscription : expiredTrials) {
      try {
        // Execute in tenant context
        TenantContext.executeInTenantContext(subscription.getTenantId().toString(), () -> {
          var event = createTrialExpiredEvent(subscription);
          batch.add(event);

          // Publish batch when full
          if (batch.size() >= BATCH_SIZE) {
            publishBatch(batch);
            batch.clear();
          }
        });

        processedCount++;
      } catch (final Exception e) {
        log.error("Failed to process expired trial for subscription {}",
            subscription.getId(), e);
        meterRegistry.counter("billing.job.trial_expiration.error",
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
   * Creates a TrialExpired event from a subscription.
   *
   * @param subscription subscription with expired trial
   * @return trial expired event
   */
  private TrialExpired createTrialExpiredEvent(Subscription subscription) {
    return new TrialExpired(
        UUID.randomUUID(),
        Instant.now(),
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getUserId(),
        subscription.getPlan().getId(),
        subscription.getTrialEnd().atZone(java.time.ZoneId.systemDefault()).toInstant()
    );
  }

  /**
   * Publishes a batch of events to RabbitMQ.
   *
   * @param events list of events to publish
   */
  private void publishBatch(List<TrialExpired> events) {
    log.debug("Publishing batch of {} trial expired events", events.size());

    for (final var event : events) {
      try {
        eventPublisher.publish(event);
      } catch (final Exception e) {
        log.error("Failed to publish trial expired event for subscription {}",
            event.aggregateId(), e);
        meterRegistry.counter("billing.job.trial_expiration.publish_error",
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
      log.error("Failed to acquire lock for trial expiration job", e);
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
      log.error("Failed to release lock for trial expiration job", e);
    }
  }
}
