package com.iqscaffold.billingservice.infrastructure.messaging;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Consumer for sending billing notifications.
 * 
 * <p>Why Async: External service (email provider), shouldn't block business operations, 
 * can tolerate delays
 * 
 * <p>Features:
 * <ul>
 *   <li>Email template rendering</li>
 *   <li>Retry logic for email delivery failures</li>
 *   <li>Email delivery tracking</li>
 *   <li>Handles email provider rate limits with circuit breaker</li>
 * </ul>
 */
@Component
@Slf4j
public class NotificationConsumer {

    private static final String QUEUE_NAME = "billing.notification";
    
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer processingTimer;

    public NotificationConsumer(
        RestTemplate restTemplate,
        MeterRegistry meterRegistry
    ) {
        this.restTemplate = restTemplate;
        
        // Configure circuit breaker for email service
        var circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .slidingWindowSize(10)
            .build();
        
        var circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("emailService");
        
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
     * @param message the notification request containing recipient, template, and data
     */
    @RabbitListener(queues = QUEUE_NAME, concurrency = "3-6")
    public void handleNotification(Map<String, Object> message) {
        processingTimer.record(() -> {
            try {
                var recipient = (String) message.get("recipient");
                var template = (String) message.get("template");
                var subject = (String) message.get("subject");
                @SuppressWarnings("unchecked")
                var templateData = (Map<String, Object>) message.get("data");
                
                log.info("Sending notification: recipient={}, template={}", recipient, template);
                
                // Send email with circuit breaker protection
                circuitBreaker.executeRunnable(() -> {
                    sendEmail(recipient, subject, template, templateData);
                });
                
                successCounter.increment();
                log.info("Successfully sent notification: recipient={}, template={}", recipient, template);
                
            } catch (Exception e) {
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
     * Sends an email using the email service.
     */
    private void sendEmail(
        String recipient,
        String subject,
        String template,
        Map<String, Object> templateData
    ) {
        // TODO: Replace with actual email service integration
        // For now, just log the email details
        log.info("Sending email: recipient={}, subject={}, template={}, data={}",
            recipient, subject, template, templateData);
        
        // Simulate email sending
        // In production, this would call the email service API:
        // restTemplate.postForEntity(emailServiceUrl, emailRequest, EmailResponse.class);
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
