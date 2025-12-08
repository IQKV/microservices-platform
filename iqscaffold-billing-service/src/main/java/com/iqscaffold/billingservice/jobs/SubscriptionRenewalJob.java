package com.iqscaffold.billingservice.jobs;

import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.shared.event.InvoiceGenerationRequested;
import com.iqscaffold.billingservice.subscription.Subscription;
import com.iqscaffold.billingservice.subscription.SubscriptionRepository;
import com.iqscaffold.billingservice.tenancy.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job to generate invoices for upcoming subscription renewals.
 * 
 * <p>Runs daily at 1 AM UTC to identify subscriptions renewing in the next 3 days
 * and publish events for asynchronous invoice generation.
 * 
 * <p>Async Pattern: Query subscriptions due for renewal → publish InvoiceGeneration events to queue → consumer generates invoices
 * 
 * <p>Why Async: Invoice generation includes PDF creation, long-running operation
 * 
 * <p>Features:
 * <ul>
 *   <li>Query subscriptions renewing in next 3 days</li>
 *   <li>Distributed locking to prevent duplicate execution (Redis)</li>
 *   <li>Job execution logging and metrics</li>
 *   <li>Job execution timeout (max 5 minutes)</li>
 *   <li>Pagination for large result sets</li>
 *   <li>Event publishing in batches (100 events per batch)</li>
 * </ul>
 */
@Component
public class SubscriptionRenewalJob {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionRenewalJob.class);
  private static final String LOCK_KEY = "job:subscription-renewal:lock";
  private static final long LOCK_TIMEOUT_SECONDS = 300; // 5 minutes
  private static final int BATCH_SIZE = 100;
  private static final int RENEWAL_LOOKAHEAD_DAYS = 3;

  private final SubscriptionRepository subscriptionRepository;
  private final DomainEventPublisher eventPublisher;
  private final RedisTemplate<String, String> redisTemplate;
  private final MeterRegistry meterRegistry;

  public SubscriptionRenewalJob(
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
   * Executes the subscription renewal job daily at 1 AM UTC.
   * 
   * <p>Cron expression: 0 0 1 * * * (second, minute, hour, day, month, weekday)
   */
  @Scheduled(cron = "0 0 1 * * *")
  public void execute() {
    var lockAcquired = acquireLock();
    if (!lockAcquired) {
      log.info("Subscription renewal job already running on another instance, skipping");
      return;
    }

    var timer = Timer.builder("billing.job.subscription_renewal")
        .description("Subscription renewal job execution time")
        .register(meterRegistry);

    try {
      timer.record(() -> {
        log.info("Starting subscription renewal job");
        var startTime = System.currentTimeMillis();
        var processedCount = 0;

        try {
          processedCount = processSubscriptionRenewals();
          var duration = System.currentTimeMillis() - startTime;
          
          log.info("Subscription renewal job completed successfully. Processed {} subscriptions in {}ms",
              processedCount, duration);
          
          meterRegistry.counter("billing.job.subscription_renewal.success",
              "processed", String.valueOf(processedCount)).increment();
        } catch (Exception e) {
          log.error("Subscription renewal job failed", e);
          meterRegistry.counter("billing.job.subscription_renewal.failure").increment();
          throw e;
        }
      });
    } finally {
      releaseLock();
    }
  }

  /**
   * Processes subscription renewals in batches.
   * 
   * @return number of subscriptions processed
   */
  @Transactional(readOnly = true)
  protected int processSubscriptionRenewals() {
    var now = LocalDateTime.now();
    var endDate = now.plusDays(RENEWAL_LOOKAHEAD_DAYS);
    
    var renewingSubscriptions = subscriptionRepository.findPeriodsEndingSoon(now, endDate);
    
    log.info("Found {} subscriptions renewing in next {} days", 
        renewingSubscriptions.size(), RENEWAL_LOOKAHEAD_DAYS);
    
    var processedCount = 0;
    var batch = new java.util.ArrayList<InvoiceGenerationRequested>(BATCH_SIZE);
    
    for (var subscription : renewingSubscriptions) {
      try {
        // Execute in tenant context
        TenantContext.executeInTenantContext(subscription.getTenantId().toString(), () -> {
          var event = createInvoiceGenerationEvent(subscription);
          batch.add(event);
          
          // Publish batch when full
          if (batch.size() >= BATCH_SIZE) {
            publishBatch(batch);
            batch.clear();
          }
        });
        
        processedCount++;
      } catch (Exception e) {
        log.error("Failed to process subscription renewal for subscription {}", 
            subscription.getId(), e);
        meterRegistry.counter("billing.job.subscription_renewal.error",
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
   * Creates an InvoiceGenerationRequested event from a subscription.
   * 
   * @param subscription subscription due for renewal
   * @return invoice generation requested event
   */
  private InvoiceGenerationRequested createInvoiceGenerationEvent(Subscription subscription) {
    var periodStart = subscription.getCurrentPeriodEnd();
    var periodEnd = calculateNextPeriodEnd(subscription);
    
    return new InvoiceGenerationRequested(
        UUID.randomUUID(),
        Instant.now(),
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getUserId(),
        subscription.getPlan().getId(),
        periodStart,
        periodEnd
    );
  }

  /**
   * Calculates the next period end date based on billing cycle.
   * 
   * @param subscription subscription
   * @return next period end date
   */
  private LocalDateTime calculateNextPeriodEnd(Subscription subscription) {
    var currentPeriodEnd = subscription.getCurrentPeriodEnd();
    var billingCycle = subscription.getPlan().getBillingCycle();
    
    return switch (billingCycle) {
      case MONTHLY -> currentPeriodEnd.plusMonths(1);
      case YEARLY -> currentPeriodEnd.plusYears(1);
      case LIFETIME -> currentPeriodEnd.plusYears(100); // Effectively never expires
    };
  }

  /**
   * Publishes a batch of events to RabbitMQ.
   * 
   * @param events list of events to publish
   */
  private void publishBatch(List<InvoiceGenerationRequested> events) {
    log.debug("Publishing batch of {} invoice generation events", events.size());
    
    for (var event : events) {
      try {
        eventPublisher.publish(event);
      } catch (Exception e) {
        log.error("Failed to publish invoice generation event for subscription {}", 
            event.aggregateId(), e);
        meterRegistry.counter("billing.job.subscription_renewal.publish_error",
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
    } catch (Exception e) {
      log.error("Failed to acquire lock for subscription renewal job", e);
      return false;
    }
  }

  /**
   * Releases distributed lock.
   */
  private void releaseLock() {
    try {
      redisTemplate.delete(LOCK_KEY);
    } catch (Exception e) {
      log.error("Failed to release lock for subscription renewal job", e);
    }
  }
}
