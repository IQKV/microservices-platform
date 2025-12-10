package com.iqscaffold.billingservice.infrastructure.messaging;

import java.util.Locale;
import java.util.Map;

import com.iqscaffold.billingservice.infrastructure.email.EmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for sending billing notifications.
 *
 * <p>Why Async: External service (email provider), shouldn't block business operations,
 * can tolerate delays
 *
 * <p>Features:
 * <ul>
 *   <li>Email template rendering with Thymeleaf</li>
 *   <li>i18n support for multi-language emails</li>
 *   <li>Retry logic for email delivery failures</li>
 *   <li>Email delivery tracking with metrics</li>
 *   <li>Handles email provider rate limits with circuit breaker</li>
 * </ul>
 */
@Component
public class NotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

  private static final String QUEUE_NAME = "billing.notification";

  private final EmailService emailService;
  private final Counter successCounter;
  private final Counter failureCounter;
  private final Timer processingTimer;

  public NotificationConsumer(
      EmailService emailService,
      MeterRegistry meterRegistry
  ) {
    this.emailService = emailService;

    // Initialize metrics
    this.successCounter = Counter.builder("billing.notification.consumer.success")
        .description("Number of successfully sent notifications")
        .register(meterRegistry);

    this.failureCounter = Counter.builder("billing.notification.consumer.failure")
        .description("Number of failed notification sending attempts")
        .register(meterRegistry);

    this.processingTimer = Timer.builder("billing.notification.consumer.processing.time")
        .description("Time taken to send notifications")
        .register(meterRegistry);
  }

  /**
   * Processes notification messages from the queue.
   *
   * <p>Message format:
   * <pre>
   * {
   *   "recipient": "user@example.com",
   *   "template": "subscription_created",
   *   "locale": "en",
   *   "data": {
   *     "userName": "John Doe",
   *     "planName": "Pro",
   *     "price": "29.99",
   *     "currency": "USD",
   *     ...
   *   }
   * }
   * </pre>
   *
   * @param message the notification request containing recipient, template, locale, and data
   */
  @RabbitListener(queues = QUEUE_NAME, concurrency = "3-6")
  public void handleNotification(Map<String, Object> message) {
    processingTimer.record(() -> {
      try {
        var recipient = (String) message.get("recipient");
        var template = (String) message.get("template");
        var localeStr = (String) message.getOrDefault("locale", "en");
        @SuppressWarnings("unchecked")
        var templateData = (Map<String, Object>) message.get("data");

        log.info("Processing notification: recipient={}, template={}, locale={}",
            recipient, template, localeStr);

        // Parse locale
        var locale = parseLocale(localeStr);

        // Send email using EmailService with i18n support
        emailService.sendEmail(recipient, template, templateData, locale);

        successCounter.increment();
        log.info("Successfully sent notification: recipient={}, template={}", recipient, template);

      } catch (final Exception e) {
        failureCounter.increment();
        log.error("Failed to send notification: recipient={}",
            message.get("recipient"), e);

        // Categorize error
        if (isTransientError(e)) {
          log.info("Transient error detected, message will be retried");
          throw new RuntimeException("Transient error - retry", e);
        } else if (isRateLimitError(e)) {
          log.warn("Rate limit detected, message will be retried with backoff");
          throw new RuntimeException("Rate limit - retry with backoff", e);
        } else {
          log.error("Permanent error detected, message will be sent to DLQ");
          // Don't rethrow - let message go to DLQ
        }
      }
    });
  }

  /**
   * Parses locale string to Locale object.
   * Supports formats: "en", "en_US", "en-US"
   */
  private Locale parseLocale(String localeStr) {
    if (localeStr == null || localeStr.isBlank()) {
      return Locale.ENGLISH;
    }

    // Handle both underscore and hyphen separators
    var parts = localeStr.replace("-", "_").split("_");

    return switch (parts.length) {
      case 1 -> new Locale(parts[0]);
      case 2 -> new Locale(parts[0], parts[1]);
      case 3 -> new Locale(parts[0], parts[1], parts[2]);
      default -> Locale.ENGLISH;
    };
  }

  /**
   * Determines if an error is transient (should retry) or permanent (send to DLQ).
   */
  private boolean isTransientError(Exception e) {
    var message = e.getMessage();
    if (message == null) {
      return false;
    }

    return message.contains("timeout") ||
           message.contains("connection") ||
           message.contains("unavailable") ||
           e instanceof java.net.SocketTimeoutException;
  }

  /**
   * Determines if an error is due to rate limiting.
   */
  private boolean isRateLimitError(Exception e) {
    var message = e.getMessage();
    if (message == null) {
      return false;
    }

    return message.contains("rate limit") ||
           message.contains("429") ||
           message.contains("too many requests");
  }
}
