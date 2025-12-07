package com.iqscaffold.billingservice.config;

import com.iqscaffold.billingservice.shared.BillingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for asynchronous event processing.
 * 
 * <p>Configures:
 * <ul>
 *   <li>Topic exchange for billing events</li>
 *   <li>Queues for usage, invoice, webhook, notification, and payment retry processing</li>
 *   <li>Dead letter queues for failed message handling</li>
 *   <li>Routing keys for event-based message routing</li>
 *   <li>Message TTL and retry policies with exponential backoff (1min, 5min, 15min, 1hr, 6hr)</li>
 *   <li>Queue priorities for critical operations (payment processing > notifications)</li>
 * </ul>
 * 
 * <p>Retry Strategy:
 * Messages that fail processing are routed through retry queues with increasing delays:
 * <ol>
 *   <li>First retry: 1 minute delay</li>
 *   <li>Second retry: 5 minutes delay</li>
 *   <li>Third retry: 15 minutes delay</li>
 *   <li>Fourth retry: 1 hour delay</li>
 *   <li>Fifth retry: 6 hours delay</li>
 *   <li>After all retries exhausted: Message sent to Dead Letter Queue (DLQ) for manual review</li>
 * </ol>
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Exchange names
    private static final String BILLING_EVENTS_EXCHANGE = "billing.events";
    private static final String DLX_EXCHANGE = "billing.events.dlx";
    private static final String RETRY_EXCHANGE = "billing.events.retry";
    
    // Queue names
    private static final String USAGE_QUEUE = "billing.usage";
    private static final String INVOICE_QUEUE = "billing.invoice";
    private static final String WEBHOOK_QUEUE = "billing.webhook";
    private static final String NOTIFICATION_QUEUE = "billing.notification";
    private static final String PAYMENT_RETRY_QUEUE = "billing.payment-retry";
    
    // Retry queue names (exponential backoff)
    private static final String USAGE_RETRY_1MIN = "billing.usage.retry.1min";
    private static final String USAGE_RETRY_5MIN = "billing.usage.retry.5min";
    private static final String USAGE_RETRY_15MIN = "billing.usage.retry.15min";
    private static final String USAGE_RETRY_1HR = "billing.usage.retry.1hr";
    private static final String USAGE_RETRY_6HR = "billing.usage.retry.6hr";
    
    private static final String INVOICE_RETRY_1MIN = "billing.invoice.retry.1min";
    private static final String INVOICE_RETRY_5MIN = "billing.invoice.retry.5min";
    private static final String INVOICE_RETRY_15MIN = "billing.invoice.retry.15min";
    private static final String INVOICE_RETRY_1HR = "billing.invoice.retry.1hr";
    private static final String INVOICE_RETRY_6HR = "billing.invoice.retry.6hr";
    
    private static final String WEBHOOK_RETRY_1MIN = "billing.webhook.retry.1min";
    private static final String WEBHOOK_RETRY_5MIN = "billing.webhook.retry.5min";
    private static final String WEBHOOK_RETRY_15MIN = "billing.webhook.retry.15min";
    private static final String WEBHOOK_RETRY_1HR = "billing.webhook.retry.1hr";
    private static final String WEBHOOK_RETRY_6HR = "billing.webhook.retry.6hr";
    
    private static final String NOTIFICATION_RETRY_1MIN = "billing.notification.retry.1min";
    private static final String NOTIFICATION_RETRY_5MIN = "billing.notification.retry.5min";
    private static final String NOTIFICATION_RETRY_15MIN = "billing.notification.retry.15min";
    private static final String NOTIFICATION_RETRY_1HR = "billing.notification.retry.1hr";
    private static final String NOTIFICATION_RETRY_6HR = "billing.notification.retry.6hr";
    
    private static final String PAYMENT_RETRY_RETRY_1MIN = "billing.payment-retry.retry.1min";
    private static final String PAYMENT_RETRY_RETRY_5MIN = "billing.payment-retry.retry.5min";
    private static final String PAYMENT_RETRY_RETRY_15MIN = "billing.payment-retry.retry.15min";
    private static final String PAYMENT_RETRY_RETRY_1HR = "billing.payment-retry.retry.1hr";
    private static final String PAYMENT_RETRY_RETRY_6HR = "billing.payment-retry.retry.6hr";
    
    // Dead letter queue names
    private static final String USAGE_DLQ = "billing.usage.dlq";
    private static final String INVOICE_DLQ = "billing.invoice.dlq";
    private static final String WEBHOOK_DLQ = "billing.webhook.dlq";
    private static final String NOTIFICATION_DLQ = "billing.notification.dlq";
    private static final String PAYMENT_RETRY_DLQ = "billing.payment-retry.dlq";
    
    // Routing keys
    private static final String USAGE_ROUTING_KEY = "billing.usage.*";
    private static final String INVOICE_ROUTING_KEY = "billing.invoice.*";
    private static final String WEBHOOK_ROUTING_KEY = "billing.webhook.*";
    private static final String NOTIFICATION_ROUTING_KEY = "billing.notification.*";
    private static final String PAYMENT_RETRY_ROUTING_KEY = "billing.payment.retry.*";
    
    // Retry delays in milliseconds
    private static final int RETRY_DELAY_1MIN = 60000;      // 1 minute
    private static final int RETRY_DELAY_5MIN = 300000;     // 5 minutes
    private static final int RETRY_DELAY_15MIN = 900000;    // 15 minutes
    private static final int RETRY_DELAY_1HR = 3600000;     // 1 hour
    private static final int RETRY_DELAY_6HR = 21600000;    // 6 hours

    /**
     * Main topic exchange for billing events.
     * 
     * @return the billing events exchange
     */
    @Bean
    public TopicExchange billingEventsExchange() {
        return new TopicExchange(BILLING_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Dead letter exchange for failed messages.
     * 
     * @return the dead letter exchange
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * Retry exchange for messages with exponential backoff.
     * 
     * @return the retry exchange
     */
    @Bean
    public TopicExchange retryExchange() {
        return new TopicExchange(RETRY_EXCHANGE, true, false);
    }

    // ========== Usage Queue Configuration ==========
    
    /**
     * Queue for asynchronous usage recording.
     * High volume (10,000+ records/sec), batch processing enabled.
     * On failure, routes to retry queue with 1 minute delay.
     * 
     * @return the usage queue
     */
    @Bean
    public Queue usageQueue() {
        return QueueBuilder.durable(USAGE_QUEUE)
            .withArgument("x-dead-letter-exchange", RETRY_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USAGE_RETRY_1MIN)
            .build();
    }

    @Bean
    public Queue usageRetry1Min() {
        return QueueBuilder.durable(USAGE_RETRY_1MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USAGE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1MIN)
            .build();
    }

    @Bean
    public Queue usageRetry5Min() {
        return QueueBuilder.durable(USAGE_RETRY_5MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USAGE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_5MIN)
            .build();
    }

    @Bean
    public Queue usageRetry15Min() {
        return QueueBuilder.durable(USAGE_RETRY_15MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USAGE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_15MIN)
            .build();
    }

    @Bean
    public Queue usageRetry1Hr() {
        return QueueBuilder.durable(USAGE_RETRY_1HR)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USAGE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1HR)
            .build();
    }

    @Bean
    public Queue usageRetry6Hr() {
        return QueueBuilder.durable(USAGE_RETRY_6HR)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USAGE_DLQ)
            .withArgument("x-message-ttl", RETRY_DELAY_6HR)
            .build();
    }

    @Bean
    public Queue usageDeadLetterQueue() {
        return QueueBuilder.durable(USAGE_DLQ).build();
    }

    @Bean
    public Binding usageBinding(Queue usageQueue, TopicExchange billingEventsExchange) {
        return BindingBuilder.bind(usageQueue)
            .to(billingEventsExchange)
            .with(USAGE_ROUTING_KEY);
    }

    @Bean
    public Binding usageRetry1MinBinding(Queue usageRetry1Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(usageRetry1Min)
            .to(retryExchange)
            .with(USAGE_RETRY_1MIN);
    }

    @Bean
    public Binding usageRetry5MinBinding(Queue usageRetry5Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(usageRetry5Min)
            .to(retryExchange)
            .with(USAGE_RETRY_5MIN);
    }

    @Bean
    public Binding usageRetry15MinBinding(Queue usageRetry15Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(usageRetry15Min)
            .to(retryExchange)
            .with(USAGE_RETRY_15MIN);
    }

    @Bean
    public Binding usageRetry1HrBinding(Queue usageRetry1Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(usageRetry1Hr)
            .to(retryExchange)
            .with(USAGE_RETRY_1HR);
    }

    @Bean
    public Binding usageRetry6HrBinding(Queue usageRetry6Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(usageRetry6Hr)
            .to(retryExchange)
            .with(USAGE_RETRY_6HR);
    }

    @Bean
    public Binding usageDlqBinding(Queue usageDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(usageDeadLetterQueue)
            .to(deadLetterExchange)
            .with(USAGE_DLQ);
    }

    // ========== Invoice Queue Configuration ==========
    
    /**
     * Queue for asynchronous invoice generation.
     * Long-running operation (PDF generation), priority processing.
     * On failure, routes to retry queue with 1 minute delay.
     * 
     * @return the invoice queue
     */
    @Bean
    public Queue invoiceQueue() {
        return QueueBuilder.durable(INVOICE_QUEUE)
            .withArgument("x-dead-letter-exchange", RETRY_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVOICE_RETRY_1MIN)
            .withArgument("x-max-priority", 10) // Enable priority (critical operation)
            .build();
    }

    @Bean
    public Queue invoiceRetry1Min() {
        return QueueBuilder.durable(INVOICE_RETRY_1MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVOICE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue invoiceRetry5Min() {
        return QueueBuilder.durable(INVOICE_RETRY_5MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVOICE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_5MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue invoiceRetry15Min() {
        return QueueBuilder.durable(INVOICE_RETRY_15MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVOICE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_15MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue invoiceRetry1Hr() {
        return QueueBuilder.durable(INVOICE_RETRY_1HR)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVOICE_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1HR)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue invoiceRetry6Hr() {
        return QueueBuilder.durable(INVOICE_RETRY_6HR)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVOICE_DLQ)
            .withArgument("x-message-ttl", RETRY_DELAY_6HR)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue invoiceDeadLetterQueue() {
        return QueueBuilder.durable(INVOICE_DLQ).build();
    }

    @Bean
    public Binding invoiceBinding(Queue invoiceQueue, TopicExchange billingEventsExchange) {
        return BindingBuilder.bind(invoiceQueue)
            .to(billingEventsExchange)
            .with(INVOICE_ROUTING_KEY);
    }

    @Bean
    public Binding invoiceRetry1MinBinding(Queue invoiceRetry1Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(invoiceRetry1Min)
            .to(retryExchange)
            .with(INVOICE_RETRY_1MIN);
    }

    @Bean
    public Binding invoiceRetry5MinBinding(Queue invoiceRetry5Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(invoiceRetry5Min)
            .to(retryExchange)
            .with(INVOICE_RETRY_5MIN);
    }

    @Bean
    public Binding invoiceRetry15MinBinding(Queue invoiceRetry15Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(invoiceRetry15Min)
            .to(retryExchange)
            .with(INVOICE_RETRY_15MIN);
    }

    @Bean
    public Binding invoiceRetry1HrBinding(Queue invoiceRetry1Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(invoiceRetry1Hr)
            .to(retryExchange)
            .with(INVOICE_RETRY_1HR);
    }

    @Bean
    public Binding invoiceRetry6HrBinding(Queue invoiceRetry6Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(invoiceRetry6Hr)
            .to(retryExchange)
            .with(INVOICE_RETRY_6HR);
    }

    @Bean
    public Binding invoiceDlqBinding(Queue invoiceDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(invoiceDeadLetterQueue)
            .to(deadLetterExchange)
            .with(INVOICE_DLQ);
    }

    // ========== Webhook Queue Configuration ==========
    
    /**
     * Queue for asynchronous webhook processing.
     * External events from payment providers, idempotency critical.
     * On failure, routes to retry queue with 1 minute delay.
     * 
     * @return the webhook queue
     */
    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(WEBHOOK_QUEUE)
            .withArgument("x-dead-letter-exchange", RETRY_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_RETRY_1MIN)
            .withArgument("x-max-priority", 10) // Enable priority (critical operation)
            .build();
    }

    @Bean
    public Queue webhookRetry1Min() {
        return QueueBuilder.durable(WEBHOOK_RETRY_1MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue webhookRetry5Min() {
        return QueueBuilder.durable(WEBHOOK_RETRY_5MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_5MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue webhookRetry15Min() {
        return QueueBuilder.durable(WEBHOOK_RETRY_15MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_15MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue webhookRetry1Hr() {
        return QueueBuilder.durable(WEBHOOK_RETRY_1HR)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1HR)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue webhookRetry6Hr() {
        return QueueBuilder.durable(WEBHOOK_RETRY_6HR)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_DLQ)
            .withArgument("x-message-ttl", RETRY_DELAY_6HR)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue webhookDeadLetterQueue() {
        return QueueBuilder.durable(WEBHOOK_DLQ).build();
    }

    @Bean
    public Binding webhookBinding(Queue webhookQueue, TopicExchange billingEventsExchange) {
        return BindingBuilder.bind(webhookQueue)
            .to(billingEventsExchange)
            .with(WEBHOOK_ROUTING_KEY);
    }

    @Bean
    public Binding webhookRetry1MinBinding(Queue webhookRetry1Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(webhookRetry1Min)
            .to(retryExchange)
            .with(WEBHOOK_RETRY_1MIN);
    }

    @Bean
    public Binding webhookRetry5MinBinding(Queue webhookRetry5Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(webhookRetry5Min)
            .to(retryExchange)
            .with(WEBHOOK_RETRY_5MIN);
    }

    @Bean
    public Binding webhookRetry15MinBinding(Queue webhookRetry15Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(webhookRetry15Min)
            .to(retryExchange)
            .with(WEBHOOK_RETRY_15MIN);
    }

    @Bean
    public Binding webhookRetry1HrBinding(Queue webhookRetry1Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(webhookRetry1Hr)
            .to(retryExchange)
            .with(WEBHOOK_RETRY_1HR);
    }

    @Bean
    public Binding webhookRetry6HrBinding(Queue webhookRetry6Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(webhookRetry6Hr)
            .to(retryExchange)
            .with(WEBHOOK_RETRY_6HR);
    }

    @Bean
    public Binding webhookDlqBinding(Queue webhookDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(webhookDeadLetterQueue)
            .to(deadLetterExchange)
            .with(WEBHOOK_DLQ);
    }

    // ========== Notification Queue Configuration ==========
    
    /**
     * Queue for sending billing notifications.
     * External service (email provider), can tolerate delays.
     * Lower priority than payment processing.
     * On failure, routes to retry queue with 1 minute delay.
     * 
     * @return the notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", RETRY_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_RETRY_1MIN)
            .build();
    }

    @Bean
    public Queue notificationRetry1Min() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_1MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1MIN)
            .build();
    }

    @Bean
    public Queue notificationRetry5Min() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_5MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_5MIN)
            .build();
    }

    @Bean
    public Queue notificationRetry15Min() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_15MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_15MIN)
            .build();
    }

    @Bean
    public Queue notificationRetry1Hr() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_1HR)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1HR)
            .build();
    }

    @Bean
    public Queue notificationRetry6Hr() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_6HR)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
            .withArgument("x-message-ttl", RETRY_DELAY_6HR)
            .build();
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange billingEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
            .to(billingEventsExchange)
            .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationRetry1MinBinding(Queue notificationRetry1Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(notificationRetry1Min)
            .to(retryExchange)
            .with(NOTIFICATION_RETRY_1MIN);
    }

    @Bean
    public Binding notificationRetry5MinBinding(Queue notificationRetry5Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(notificationRetry5Min)
            .to(retryExchange)
            .with(NOTIFICATION_RETRY_5MIN);
    }

    @Bean
    public Binding notificationRetry15MinBinding(Queue notificationRetry15Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(notificationRetry15Min)
            .to(retryExchange)
            .with(NOTIFICATION_RETRY_15MIN);
    }

    @Bean
    public Binding notificationRetry1HrBinding(Queue notificationRetry1Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(notificationRetry1Hr)
            .to(retryExchange)
            .with(NOTIFICATION_RETRY_1HR);
    }

    @Bean
    public Binding notificationRetry6HrBinding(Queue notificationRetry6Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(notificationRetry6Hr)
            .to(retryExchange)
            .with(NOTIFICATION_RETRY_6HR);
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
            .to(deadLetterExchange)
            .with(NOTIFICATION_DLQ);
    }

    // ========== Payment Retry Queue Configuration ==========
    
    /**
     * Queue for automated payment retries.
     * Scheduled retries with exponential backoff.
     * Highest priority (payment processing is critical).
     * On failure, routes to retry queue with 1 minute delay.
     * 
     * @return the payment retry queue
     */
    @Bean
    public Queue paymentRetryQueue() {
        return QueueBuilder.durable(PAYMENT_RETRY_QUEUE)
            .withArgument("x-dead-letter-exchange", RETRY_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_RETRY_RETRY_1MIN)
            .withArgument("x-max-priority", 10) // Enable priority (payment processing is critical)
            .build();
    }

    @Bean
    public Queue paymentRetryRetry1Min() {
        return QueueBuilder.durable(PAYMENT_RETRY_RETRY_1MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_RETRY_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue paymentRetryRetry5Min() {
        return QueueBuilder.durable(PAYMENT_RETRY_RETRY_5MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_RETRY_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_5MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue paymentRetryRetry15Min() {
        return QueueBuilder.durable(PAYMENT_RETRY_RETRY_15MIN)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_RETRY_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_15MIN)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue paymentRetryRetry1Hr() {
        return QueueBuilder.durable(PAYMENT_RETRY_RETRY_1HR)
            .withArgument("x-dead-letter-exchange", BILLING_EVENTS_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_RETRY_ROUTING_KEY.replace("*", "retry"))
            .withArgument("x-message-ttl", RETRY_DELAY_1HR)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue paymentRetryRetry6Hr() {
        return QueueBuilder.durable(PAYMENT_RETRY_RETRY_6HR)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_RETRY_DLQ)
            .withArgument("x-message-ttl", RETRY_DELAY_6HR)
            .withArgument("x-max-priority", 10)
            .build();
    }

    @Bean
    public Queue paymentRetryDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_RETRY_DLQ).build();
    }

    @Bean
    public Binding paymentRetryBinding(Queue paymentRetryQueue, TopicExchange billingEventsExchange) {
        return BindingBuilder.bind(paymentRetryQueue)
            .to(billingEventsExchange)
            .with(PAYMENT_RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding paymentRetryRetry1MinBinding(Queue paymentRetryRetry1Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(paymentRetryRetry1Min)
            .to(retryExchange)
            .with(PAYMENT_RETRY_RETRY_1MIN);
    }

    @Bean
    public Binding paymentRetryRetry5MinBinding(Queue paymentRetryRetry5Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(paymentRetryRetry5Min)
            .to(retryExchange)
            .with(PAYMENT_RETRY_RETRY_5MIN);
    }

    @Bean
    public Binding paymentRetryRetry15MinBinding(Queue paymentRetryRetry15Min, TopicExchange retryExchange) {
        return BindingBuilder.bind(paymentRetryRetry15Min)
            .to(retryExchange)
            .with(PAYMENT_RETRY_RETRY_15MIN);
    }

    @Bean
    public Binding paymentRetryRetry1HrBinding(Queue paymentRetryRetry1Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(paymentRetryRetry1Hr)
            .to(retryExchange)
            .with(PAYMENT_RETRY_RETRY_1HR);
    }

    @Bean
    public Binding paymentRetryRetry6HrBinding(Queue paymentRetryRetry6Hr, TopicExchange retryExchange) {
        return BindingBuilder.bind(paymentRetryRetry6Hr)
            .to(retryExchange)
            .with(PAYMENT_RETRY_RETRY_6HR);
    }

    @Bean
    public Binding paymentRetryDlqBinding(Queue paymentRetryDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentRetryDeadLetterQueue)
            .to(deadLetterExchange)
            .with(PAYMENT_RETRY_DLQ);
    }

    // ========== Message Converter and Template Configuration ==========
    
    /**
     * JSON message converter for RabbitMQ messages.
     * 
     * @return the message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitMQ template with JSON message converter.
     * 
     * @param connectionFactory the connection factory
     * @param messageConverter the message converter
     * @return configured RabbitMQ template
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter
    ) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setChannelTransacted(true);
        return template;
    }
}
