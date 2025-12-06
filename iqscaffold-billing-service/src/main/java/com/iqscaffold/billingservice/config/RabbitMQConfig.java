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
 *   <li>Message TTL and retry policies with exponential backoff</li>
 * </ul>
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Exchange name
    private static final String BILLING_EVENTS_EXCHANGE = "billing.events";
    
    // Dead letter exchange
    private static final String DLX_EXCHANGE = "billing.events.dlx";
    
    // Queue names
    private static final String USAGE_QUEUE = "billing.usage";
    private static final String INVOICE_QUEUE = "billing.invoice";
    private static final String WEBHOOK_QUEUE = "billing.webhook";
    private static final String NOTIFICATION_QUEUE = "billing.notification";
    private static final String PAYMENT_RETRY_QUEUE = "billing.payment-retry";
    
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

    // ========== Usage Queue Configuration ==========
    
    /**
     * Queue for asynchronous usage recording.
     * High volume (10,000+ records/sec), batch processing enabled.
     * 
     * @return the usage queue
     */
    @Bean
    public Queue usageQueue() {
        return QueueBuilder.durable(USAGE_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USAGE_DLQ)
            .withArgument("x-message-ttl", 3600000) // 1 hour TTL
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
    public Binding usageDlqBinding(Queue usageDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(usageDeadLetterQueue)
            .to(deadLetterExchange)
            .with(USAGE_DLQ);
    }

    // ========== Invoice Queue Configuration ==========
    
    /**
     * Queue for asynchronous invoice generation.
     * Long-running operation (PDF generation), priority processing.
     * 
     * @return the invoice queue
     */
    @Bean
    public Queue invoiceQueue() {
        return QueueBuilder.durable(INVOICE_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVOICE_DLQ)
            .withArgument("x-message-ttl", 7200000) // 2 hours TTL
            .withArgument("x-max-priority", 10) // Enable priority
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
    public Binding invoiceDlqBinding(Queue invoiceDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(invoiceDeadLetterQueue)
            .to(deadLetterExchange)
            .with(INVOICE_DLQ);
    }

    // ========== Webhook Queue Configuration ==========
    
    /**
     * Queue for asynchronous webhook processing.
     * External events from payment providers, idempotency critical.
     * 
     * @return the webhook queue
     */
    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(WEBHOOK_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", WEBHOOK_DLQ)
            .withArgument("x-message-ttl", 3600000) // 1 hour TTL
            .withArgument("x-max-priority", 10) // Enable priority
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
    public Binding webhookDlqBinding(Queue webhookDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(webhookDeadLetterQueue)
            .to(deadLetterExchange)
            .with(WEBHOOK_DLQ);
    }

    // ========== Notification Queue Configuration ==========
    
    /**
     * Queue for sending billing notifications.
     * External service (email provider), can tolerate delays.
     * 
     * @return the notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
            .withArgument("x-message-ttl", 86400000) // 24 hours TTL
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
    public Binding notificationDlqBinding(Queue notificationDeadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
            .to(deadLetterExchange)
            .with(NOTIFICATION_DLQ);
    }

    // ========== Payment Retry Queue Configuration ==========
    
    /**
     * Queue for automated payment retries.
     * Scheduled retries with exponential backoff.
     * 
     * @return the payment retry queue
     */
    @Bean
    public Queue paymentRetryQueue() {
        return QueueBuilder.durable(PAYMENT_RETRY_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_RETRY_DLQ)
            .withArgument("x-message-ttl", 86400000) // 24 hours TTL
            .withArgument("x-max-priority", 10) // Enable priority (payment processing is critical)
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
