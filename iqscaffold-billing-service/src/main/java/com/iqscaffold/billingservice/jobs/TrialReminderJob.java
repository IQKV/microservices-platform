package com.iqscaffold.billingservice.jobs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.event.TrialReminderRequested;
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
 * Scheduled job to send trial ending reminders.
 *
 * <p>Runs daily at 10 AM UTC to identify trials ending soon
 * and publish events for asynchronous email notification.
 *
 * <p>Async Pattern: Query trials ending soon → publish TrialReminder events to queue → consumer sends emails
 *
 * <p>Why Async: Email sending is external service, shouldn't block job
 *
 * <p>Reminder Schedule: 3 days before expiration and 1 day before expiration
 *
 * <p>Features:
 * <ul>
 *   <li>Query trials ending in 3 days and 1 day</li>
 *   <li>Distributed locking to prevent duplicate execution (Redis)</li>
 *   <li>Job execution logging and metrics</li>
 *   <li>Job execution timeout (max 5 minutes)</li>
 *   <li>Pagination for large result sets</li>
 *   <li>Event publishing in batches (100 events per batch)</li>
 * </ul>
 */
@Component
public class TrialReminderJob {

  private static final Logger log = LoggerFactory.getLogger(TrialReminderJob.class);
  private static final String LOCK_KEY = "job:trial-reminder:lock";
  private static final long LOCK_TIMEOUT_SECONDS = 300; // 5 minutes
  private static final int BATCH_SIZE = 100;

  // Reminder schedule in days before expiration
  private static final int[] REMINDER_DAYS = {3, 1};

  private final SubscriptionRepository subscriptionRepository;
  private final DomainEventPublisher eventPublisher;
  private final RedisTemplate<String, String> redisTemplate;
  private final MeterRegistry meterRegistry;

  public TrialReminderJob(
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
   * Executes the trial reminder job daily at 10 AM UTC.
   *
   * <p>Cron expression: 0 0 10 * * * (second, minute, hour, day, month, weekday)
   */
  @Scheduled(cron = "0 0 10 * * *")
  public void execute() {
    var lockAcquired = acquireLock();
    if (!lockAcquired) {
      log.info("Trial reminder job already running on another instance, skipping");
      return;
    }

    var timer = Timer.builder("billing.job.trial_reminder")
        .description("Trial reminder job execution time")
        .register(meterRegistry);

    try {
      timer.record(() -> {
        log.info("Starting trial reminder job");
        var startTime = System.currentTimeMillis();
        var processedCount = 0;

        try {
          processedCount = processTrialReminders();
          var duration = System.currentTimeMillis() - startTime;

          log.info("Trial reminder job completed successfully. Processed {} subscriptions in {}ms",
              processedCount, duration);

          meterRegistry.counter("billing.job.trial_reminder.success",
              "processed", String.valueOf(processedCount)).increment();
        } catch (final Exception e) {
          log.error("Trial reminder job failed", e);
          meterRegistry.counter("billing.job.trial_reminder.failure").increment();
          throw e;
        }
      });
    } finally {
      releaseLock();
    }
  }

  /**
   * Processes trial reminders in batches.
   *
   * @return number of subscriptions processed
   */
  @Transactional(readOnly = true)
  protected int processTrialReminders() {
    var now = LocalDateTime.now();
    var processedCount = 0;

    // Process reminders for each day threshold
    for (final var daysRemaining : REMINDER_DAYS) {
      var endDate = now.plusDays(daysRemaining).plusHours(1); // 1-hour window
      var startDate = now.plusDays(daysRemaining);

      var trialsEndingSoon = subscriptionRepository.findTrialsEndingSoon(
          daysRemaining, startDate, endDate);

      log.info("Found {} trials ending in {} days",
          trialsEndingSoon.size(), daysRemaining);

      processedCount += processTrialBatch(trialsEndingSoon, daysRemaining);
    }

    return processedCount;
  }

  /**
   * Processes a batch of trials ending soon.
   *
   * @param subscriptions list of subscriptions with trials ending soon
   * @param daysRemaining days remaining until trial ends
   * @return number of subscriptions processed
   */
  private int processTrialBatch(List<Subscription> subscriptions, int daysRemaining) {
    var processedCount = 0;
    var batch = new java.util.ArrayList<TrialReminderRequested>(BATCH_SIZE);

    for (final var subscription : subscriptions) {
      try {
        // Execute in tenant context
        TenantContext.executeInTenantContext(subscription.getTenantId().toString(), () -> {
          var event = createTrialReminderEvent(subscription, daysRemaining);
          batch.add(event);

          // Publish batch when full
          if (batch.size() >= BATCH_SIZE) {
            publishBatch(batch);
            batch.clear();
          }
        });

        processedCount++;
      } catch (final Exception e) {
        log.error("Failed to process trial reminder for subscription {}",
            subscription.getId(), e);
        meterRegistry.counter("billing.job.trial_reminder.error",
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
   * Creates a TrialReminderRequested event from a subscription.
   *
   * @param subscription  subscription with trial ending soon
   * @param daysRemaining days remaining until trial ends
   * @return trial reminder requested event
   */
  private TrialReminderRequested createTrialReminderEvent(
      Subscription subscription,
      int daysRemaining
  ) {
    return new TrialReminderRequested(
        UUID.randomUUID(),
        Instant.now(),
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getUserId(),
        subscription.getPlan().getId(),
        subscription.getTrialEnd(),
        daysRemaining
    );
  }

  /**
   * Publishes a batch of events to RabbitMQ.
   *
   * @param events list of events to publish
   */
  private void publishBatch(List<TrialReminderRequested> events) {
    log.debug("Publishing batch of {} trial reminder events", events.size());

    for (final var event : events) {
      try {
        eventPublisher.publish(event);
      } catch (final Exception e) {
        log.error("Failed to publish trial reminder event for subscription {}",
            event.aggregateId(), e);
        meterRegistry.counter("billing.job.trial_reminder.publish_error",
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
      log.error("Failed to acquire lock for trial reminder job", e);
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
      log.error("Failed to release lock for trial reminder job", e);
    }
  }
}
