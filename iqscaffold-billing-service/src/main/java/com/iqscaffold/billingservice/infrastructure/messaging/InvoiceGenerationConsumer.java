package com.iqscaffold.billingservice.infrastructure.messaging;

import java.util.Map;

import com.iqscaffold.billingservice.invoice.InvoiceApplicationService;
import com.iqscaffold.billingservice.shared.event.DomainEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for asynchronous invoice generation.
 *
 * <p>Why Async: Long-running operation (PDF generation can take seconds),
 * shouldn't block subscription operations
 *
 * <p>Features:
 * <ul>
 *   <li>PDF generation using streaming to handle large invoices</li>
 *   <li>Retry logic for PDF generation failures</li>
 *   <li>Publishes InvoiceGenerated event after successful generation</li>
 * </ul>
 */
@Component
public class InvoiceGenerationConsumer {

  private static final Logger log = LoggerFactory.getLogger(InvoiceGenerationConsumer.class);
  private static final String QUEUE_NAME = "billing.invoice";

  private final InvoiceApplicationService invoiceApplicationService;
  private final DomainEventPublisher eventPublisher;
  private final Counter successCounter;
  private final Counter failureCounter;
  private final Timer processingTimer;

  public InvoiceGenerationConsumer(
      final InvoiceApplicationService invoiceApplicationService,
      final DomainEventPublisher eventPublisher,
      final MeterRegistry meterRegistry
  ) {
    this.invoiceApplicationService = invoiceApplicationService;
    this.eventPublisher = eventPublisher;

    // Initialize metrics
    this.successCounter = Counter.builder("billing.invoice.consumer.success")
        .description("Number of successfully generated invoices")
        .register(meterRegistry);

    this.failureCounter = Counter.builder("billing.invoice.consumer.failure")
        .description("Number of failed invoice generation attempts")
        .register(meterRegistry);

    this.processingTimer = Timer.builder("billing.invoice.consumer.processing.time")
        .description("Time taken to generate invoices")
        .register(meterRegistry);
  }

  /**
   * Processes invoice generation messages from the queue.
   *
   * @param message the invoice generation request containing subscriptionId and period info
   */
  @RabbitListener(queues = QUEUE_NAME, concurrency = "2-5")
  public void handleInvoiceGeneration(Map<String, Object> message) {
    processingTimer.record(() -> {
      try {
        var subscriptionId = (Long) message.get("subscriptionId");
        var tenantId = (String) message.get("tenantId");

        log.info("Generating invoice for subscription: subscriptionId={}, tenantId={}",
            subscriptionId, tenantId);

        // Generate invoice (includes PDF generation)
        var invoice = invoiceApplicationService.generateInvoiceForSubscription(subscriptionId);

        // Publish InvoiceGenerated event
        eventPublisher.publish(new com.iqscaffold.billingservice.shared.event.InvoiceGenerated(
            java.util.UUID.randomUUID(),
            java.time.Instant.now(),
            invoice.id(),
            java.util.UUID.fromString(tenantId),
            subscriptionId,
            invoice.invoiceNumber(),
            invoice.total(),
            invoice.currency(),
            invoice.dueDate().atZone(java.time.ZoneId.systemDefault()).toInstant()
        ));

        successCounter.increment();
        log.info("Successfully generated invoice: invoiceId={}, subscriptionId={}",
            invoice.id(), subscriptionId);

      } catch (final Exception e) {
        failureCounter.increment();
        log.error("Failed to generate invoice: subscriptionId={}",
            message.get("subscriptionId"), e);

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
           || message.contains("PDF generation")
           || e instanceof java.net.SocketTimeoutException
           || e instanceof java.sql.SQLTransientException;
  }
}
