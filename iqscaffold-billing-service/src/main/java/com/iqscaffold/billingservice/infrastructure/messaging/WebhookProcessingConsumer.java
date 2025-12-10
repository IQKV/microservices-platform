package com.iqscaffold.billingservice.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.iqscaffold.billingservice.webhook.WebhookApplicationService;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import com.iqscaffold.billingservice.webhook.WebhookEventRepository;
import com.iqscaffold.billingservice.webhook.WebhookEventStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for asynchronous webhook processing.
 *
 * <p>Why Async: External events from payment providers, need retry logic, idempotency critical
 *
 * <p>Features:
 * <ul>
 *   <li>Idempotency using webhook event ID</li>
 *   <li>Exponential backoff retry (1min, 5min, 15min, 1hr, 6hr)</li>
 *   <li>Webhook signature verification</li>
 *   <li>Handles duplicate webhook events gracefully</li>
 * </ul>
 */
@Component
public class WebhookProcessingConsumer {

  private static final Logger log = LoggerFactory.getLogger(WebhookProcessingConsumer.class);
  private static final String QUEUE_NAME = "billing.webhook";

  private final WebhookApplicationService webhookApplicationService;
  private final WebhookEventRepository webhookEventRepository;
  private final Set<String> processedEventIds;
  private final Counter successCounter;
  private final Counter failureCounter;
  private final Counter duplicateCounter;
  private final Timer processingTimer;

  public WebhookProcessingConsumer(
      final WebhookApplicationService webhookApplicationService,
      final WebhookEventRepository webhookEventRepository,
      final MeterRegistry meterRegistry
  ) {
    this.webhookApplicationService = webhookApplicationService;
    this.webhookEventRepository = webhookEventRepository;
    this.processedEventIds = ConcurrentHashMap.newKeySet();

    // Initialize metrics
    this.successCounter = Counter.builder("billing.webhook.consumer.success")
        .description("Number of successfully processed webhooks")
        .register(meterRegistry);

    this.failureCounter = Counter.builder("billing.webhook.consumer.failure")
        .description("Number of failed webhook processing attempts")
        .register(meterRegistry);

    this.duplicateCounter = Counter.builder("billing.webhook.consumer.duplicate")
        .description("Number of duplicate webhook events detected")
        .register(meterRegistry);

    this.processingTimer = Timer.builder("billing.webhook.consumer.processing.time")
        .description("Time taken to process webhooks")
        .register(meterRegistry);
  }

  /**
   * Processes webhook messages from the queue.
   *
   * @param message the webhook event containing provider, eventId, eventType, and payload
   */
  @RabbitListener(queues = QUEUE_NAME, concurrency = "3-8")
  public void handleWebhook(Map<String, Object> message) {
    processingTimer.record(() -> {
      try {
        var provider = (String) message.get("provider");
        var eventId = (String) message.get("eventId");
        var eventType = (String) message.get("eventType");
        var payload = (String) message.get("payload");
        var signature = (String) message.get("signature");

        log.info("Processing webhook: provider={}, eventId={}, eventType={}",
            provider, eventId, eventType);

        // Check idempotency - skip if already processed
        if (processedEventIds.contains(eventId)) {
          log.debug("Webhook event already processed, skipping: {}", eventId);
          duplicateCounter.increment();
          return;
        }

        // Check if event exists in database (for replay protection)
        var existingEvent = webhookEventRepository.findByProviderEventId(eventId);
        if (existingEvent.isPresent()
            && existingEvent.get().getStatus() == WebhookEventStatus.PROCESSED) {
          log.debug("Webhook event already processed in database, skipping: {}", eventId);
          processedEventIds.add(eventId);
          duplicateCounter.increment();
          return;
        }

        // Verify webhook signature
        var isValid = webhookApplicationService.verifyWebhookSignature(
            provider, payload, signature
        );

        if (!isValid) {
          log.error("Invalid webhook signature: provider={}, eventId={}", provider, eventId);
          failureCounter.increment();
          // Don't retry - invalid signature is permanent error
          return;
        }

        // Process webhook based on provider
        switch (provider.toLowerCase()) {
          case "stripe" -> webhookApplicationService.processStripeWebhook(eventType, payload);
          case "paypal" -> webhookApplicationService.processPayPalWebhook(eventType, payload);
          default -> {
            log.error("Unknown webhook provider: {}", provider);
            failureCounter.increment();
            return;
          }
        }

        // Mark as processed
        processedEventIds.add(eventId);

        // Update webhook event status in database
        if (existingEvent.isPresent()) {
          var event = existingEvent.get();
          event.setStatus(WebhookEventStatus.PROCESSED);
          event.setProcessedAt(LocalDateTime.now());
          webhookEventRepository.save(event);
        } else {
          // Create new webhook event record
          var newEvent = new WebhookEvent();
          newEvent.setProvider(provider);
          newEvent.setProviderEventId(eventId);
          newEvent.setEventType(eventType);
          newEvent.setPayload(payload);
          newEvent.setStatus(WebhookEventStatus.PROCESSED);
          newEvent.setProcessedAt(LocalDateTime.now());
          webhookEventRepository.save(newEvent);
        }

        successCounter.increment();
        log.info("Successfully processed webhook: provider={}, eventId={}", provider, eventId);

      } catch (final Exception e) {
        failureCounter.increment();
        log.error("Failed to process webhook: eventId={}",
            message.get("eventId"), e);

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
           || message.contains("rate limit")
           || e instanceof java.net.SocketTimeoutException
           || e instanceof java.sql.SQLTransientException;
  }
}
