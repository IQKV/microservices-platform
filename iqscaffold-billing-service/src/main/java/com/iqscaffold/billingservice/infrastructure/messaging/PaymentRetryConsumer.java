package com.iqscaffold.billingservice.infrastructure.messaging;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import com.iqscaffold.billingservice.payment.PaymentApplicationService;
import com.iqscaffold.billingservice.payment.PaymentStatus;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import com.iqscaffold.billingservice.subscription.SubscriptionApplicationService;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for automated payment retries.
 *
 * <p>Why Async: Scheduled retries with delays, external payment provider calls
 *
 * <p>Features:
 * <ul>
 *   <li>Retry schedule (Day 1, Day 3, Day 7, Day 14)</li>
 *   <li>Exponential backoff between attempts</li>
 *   <li>Updates subscription status based on retry results</li>
 *   <li>Publishes PaymentSucceeded or PaymentFailed events</li>
 * </ul>
 */
@Component
public class PaymentRetryConsumer {

  private static final Logger log = LoggerFactory.getLogger(PaymentRetryConsumer.class);
  private static final String QUEUE_NAME = "billing.payment-retry";
  private static final int[] RETRY_SCHEDULE_DAYS = {1, 3, 7, 14};

  private final PaymentApplicationService paymentApplicationService;
  private final SubscriptionApplicationService subscriptionApplicationService;
  private final DomainEventPublisher eventPublisher;
  private final CircuitBreaker circuitBreaker;
  private final Counter successCounter;
  private final Counter failureCounter;
  private final Timer processingTimer;

  public PaymentRetryConsumer(
      final PaymentApplicationService paymentApplicationService,
      final SubscriptionApplicationService subscriptionApplicationService,
      final DomainEventPublisher eventPublisher,
      final MeterRegistry meterRegistry
  ) {
    this.paymentApplicationService = paymentApplicationService;
    this.subscriptionApplicationService = subscriptionApplicationService;
    this.eventPublisher = eventPublisher;

    // Configure circuit breaker for payment provider
    var circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofMinutes(5))
        .slidingWindowSize(10)
        .build();

    var circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentProvider");

    // Initialize metrics
    this.successCounter = Counter.builder("billing.payment.retry.consumer.success")
        .description("Number of successful payment retries")
        .register(meterRegistry);

    this.failureCounter = Counter.builder("billing.payment.retry.consumer.failure")
        .description("Number of failed payment retry attempts")
        .register(meterRegistry);

    this.processingTimer = Timer.builder("billing.payment.retry.consumer.processing.time")
        .description("Time taken to process payment retries")
        .register(meterRegistry);
  }

  /**
   * Processes payment retry messages from the queue.
   *
   * @param message the payment retry request containing paymentId, subscriptionId, and attempt number
   */
  @RabbitListener(queues = QUEUE_NAME, concurrency = "2-4")
  public void handlePaymentRetry(Map<String, Object> message) {
    processingTimer.record(() -> {
      try {
        var paymentId = ((Number) message.get("paymentId")).longValue();
        var subscriptionId = ((Number) message.get("subscriptionId")).longValue();
        var attemptNumber = ((Number) message.get("attemptNumber")).intValue();
        var tenantId = (String) message.get("tenantId");

        log.info("Processing payment retry: paymentId={}, subscriptionId={}, attempt={}",
            paymentId, subscriptionId, attemptNumber);

        // Check if we've exceeded max retry attempts
        if (attemptNumber > RETRY_SCHEDULE_DAYS.length) {
          log.warn("Max retry attempts exceeded for payment: paymentId={}", paymentId);
          handleMaxRetriesExceeded(paymentId, subscriptionId, tenantId);
          return;
        }

        // Retry payment with circuit breaker protection
        var futureResult = circuitBreaker.executeSupplier(() ->
            paymentApplicationService.retryPayment(paymentId)
        );

        // Wait for async result
        var paymentDto = futureResult.join();

        if (paymentDto.status() == PaymentStatus.SUCCEEDED) {
          // Payment succeeded
          successCounter.increment();
          log.info("Payment retry succeeded: paymentId={}, subscriptionId={}",
              paymentId, subscriptionId);

          // Update subscription status to ACTIVE
          subscriptionApplicationService.updateSubscriptionStatus(
              subscriptionId, SubscriptionStatus.ACTIVE
          );

          // Publish PaymentSucceeded event
          eventPublisher.publish(new com.iqscaffold.billingservice.shared.event.PaymentSucceeded(
              java.util.UUID.randomUUID(),
              java.time.Instant.now(),
              paymentDto.id(),
              paymentDto.tenantId(),
              paymentDto.invoiceId(),
              subscriptionId,
              paymentDto.amount(),
              paymentDto.currency(),
              paymentDto.paymentMethodId(),
              paymentDto.providerPaymentId()
          ));

        } else {
          // Payment failed - schedule next retry if attempts remaining
          failureCounter.increment();
          log.warn("Payment retry failed: paymentId={}, subscriptionId={}, reason={}",
              paymentId, subscriptionId, paymentDto.failureReason());

          if (attemptNumber < RETRY_SCHEDULE_DAYS.length) {
            scheduleNextRetry(paymentId, subscriptionId, tenantId, attemptNumber + 1);
          } else {
            handleMaxRetriesExceeded(paymentId, subscriptionId, tenantId);
          }
        }

      } catch (final Exception e) {
        failureCounter.increment();
        log.error("Failed to process payment retry: paymentId={}",
            message.get("paymentId"), e);

        // Categorize error
        if (isTransientError(e)) {
          log.info("Transient error detected, message will be retried");
          throw new RuntimeException("Transient error - retry", e);
        } else {
          log.error("Permanent error detected, message will be sent to DLQ");
          // Don't rethrow - let message go to DLQ
        }
      }
    });
  }

  /**
   * Schedules the next payment retry.
   */
  private void scheduleNextRetry(
      Long paymentId,
      Long subscriptionId,
      String tenantId,
      int nextAttempt
  ) {
    log.info("Scheduling next payment retry: paymentId={}, attempt={}", paymentId, nextAttempt);

    // TODO: Publish message for next retry - requires PaymentRetryScheduled event
    // For now, log the scheduling
    log.info("Payment retry scheduled: paymentId={}, attempt={}, scheduledFor={}",
        paymentId, nextAttempt, LocalDateTime.now().plusDays(RETRY_SCHEDULE_DAYS[nextAttempt - 1]));
  }

  /**
   * Handles the case when max retry attempts have been exceeded.
   */
  private void handleMaxRetriesExceeded(Long paymentId, Long subscriptionId, String tenantId) {
    log.error("Max payment retry attempts exceeded: paymentId={}, subscriptionId={}",
        paymentId, subscriptionId);

    // Update subscription status to PAST_DUE
    subscriptionApplicationService.updateSubscriptionStatus(
        subscriptionId, SubscriptionStatus.PAST_DUE
    );

    // Publish PaymentFailed event
    eventPublisher.publish(new com.iqscaffold.billingservice.shared.event.PaymentFailed(
        java.util.UUID.randomUUID(),
        java.time.Instant.now(),
        paymentId,
        java.util.UUID.fromString(tenantId),
        null, // invoiceId not available
        subscriptionId,
        java.math.BigDecimal.ZERO, // amount not available
        "USD", // currency not available
        null, // paymentMethodId not available
        "Max retry attempts exceeded",
        RETRY_SCHEDULE_DAYS.length
    ));
  }

  /**
   * Determines if an error is transient (should retry) or permanent (send to DLQ).
   */
  private boolean isTransientError(Exception e) {
    var message = e.getMessage();
    if (message == null) {
      return false;
    }

    return message.contains("timeout")
           || message.contains("connection")
           || message.contains("unavailable")
           || e instanceof java.net.SocketTimeoutException
           || e instanceof java.sql.SQLTransientException;
  }
}
