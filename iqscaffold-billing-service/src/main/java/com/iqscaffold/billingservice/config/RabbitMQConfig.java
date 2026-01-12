package com.iqscaffold.billingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for IQScaffold Billing Service
 * Configures exchanges, queues, bindings, and message converters
 */
@Configuration
public class RabbitMQConfig {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

  private final IqScaffoldProperties properties;
  private final ObjectMapper objectMapper;

  public RabbitMQConfig(final IqScaffoldProperties properties, final ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  // Exchange names
  public static final String EVENTS_EXCHANGE = "iqscaffold.events";
  public static final String DLX_EXCHANGE = "iqscaffold.dlx";

  // Queue names
  public static final String USER_EVENTS_QUEUE = "iqscaffold.user.events";
  public static final String TENANT_EVENTS_QUEUE = "iqscaffold.billing.tenant.events";
  public static final String BILLING_EVENTS_QUEUE = "iqscaffold.billing.events";
  public static final String NOTIFICATIONS_QUEUE = "iqscaffold.notifications";
  public static final String DLQ = "iqscaffold.dlq";

  // Routing keys
  public static final String PAYMENT_SUCCESSFUL_KEY = "billing.payment.successful";
  public static final String PAYMENT_FAILED_KEY = "billing.payment.failed";
  public static final String PAYMENT_REFUNDED_KEY = "billing.payment.refunded";
  public static final String MERCHANT_ONBOARDING_KEY = "billing.merchant.onboarding";
  public static final String MERCHANT_ONBOARDED_KEY = "billing.merchant.onboarded";
  public static final String MERCHANT_CAPABILITIES_UPDATED_KEY = "billing.merchant.capabilities.updated";
  public static final String INVOICE_GENERATED_KEY = "billing.invoice.generated";
  public static final String SUBSCRIPTION_CREATED_KEY = "billing.subscription.created";
  public static final String SUBSCRIPTION_UPDATED_KEY = "billing.subscription.updated";
  public static final String SUBSCRIPTION_CANCELED_KEY = "billing.subscription.canceled";
  public static final String SUBSCRIPTION_PAUSED_KEY = "billing.subscription.paused";
  public static final String SUBSCRIPTION_RESUMED_KEY = "billing.subscription.resumed";
  public static final String SUBSCRIPTION_TRIAL_ENDING_KEY = "billing.subscription.trial_ending";
  public static final String SUBSCRIPTION_PLAN_CHANGED_KEY = "billing.subscription.plan_changed";
  public static final String NOTIFICATION_EMAIL_KEY = "notification.email";

  /**
   * Main events exchange for application events
   */
  @Bean
  public TopicExchange eventsExchange() {
    return ExchangeBuilder
        .topicExchange(EVENTS_EXCHANGE)
        .durable(true)
        .build();
  }

  /**
   * Dead Letter Exchange for failed messages
   */
  @Bean
  public TopicExchange deadLetterExchange() {
    return ExchangeBuilder
        .topicExchange(DLX_EXCHANGE)
        .durable(true)
        .build();
  }

  /**
   * User events queue with dead letter routing
   * Receives user lifecycle events from User Service
   */
  @Bean
  public Queue userEventsQueue() {
    return QueueBuilder
        .durable(USER_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Tenant events queue with dead letter routing
   * Receives tenant lifecycle events from User Service
   */
  @Bean
  public Queue tenantEventsQueue() {
    return QueueBuilder
        .durable(TENANT_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Billing events queue with dead letter routing
   */
  @Bean
  public Queue billingEventsQueue() {
    return QueueBuilder
        .durable(BILLING_EVENTS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Notifications queue with dead letter routing
   */
  @Bean
  public Queue notificationsQueue() {
    return QueueBuilder
        .durable(NOTIFICATIONS_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Dead Letter Queue for failed messages
   */
  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder
        .durable(DLQ)
        .build();
  }

  /**
   * Bind user events queue to events exchange with user.# routing key
   * Uses user.# pattern to match all user events including user.password.reset
   */
  @Bean
  public Binding userEventsBinding() {
    return BindingBuilder
        .bind(userEventsQueue())
        .to(eventsExchange())
        .with("user.#");
  }

  /**
   * Bind tenant events queue to events exchange with tenant.# routing key
   * Receives all tenant lifecycle events (created, updated, deleted)
   */
  @Bean
  public Binding tenantEventsBinding() {
    return BindingBuilder
        .bind(tenantEventsQueue())
        .to(eventsExchange())
        .with("tenant.#");
  }

  /**
   * Bind billing events queue to events exchange with billing.* routing key
   */
  @Bean
  public Binding billingEventsBinding() {
    return BindingBuilder
        .bind(billingEventsQueue())
        .to(eventsExchange())
        .with("billing.*");
  }

  /**
   * Bind notifications queue to events exchange with notification.* routing key
   */
  @Bean
  public Binding notificationsBinding() {
    return BindingBuilder
        .bind(notificationsQueue())
        .to(eventsExchange())
        .with("notification.*");
  }

  /**
   * Bind dead letter queue to DLX with all routing keys
   */
  @Bean
  public Binding deadLetterBinding() {
    return BindingBuilder
        .bind(deadLetterQueue())
        .to(deadLetterExchange())
        .with("#");
  }

  /**
   * JSON message converter using Jackson
   */
  @Bean
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  /**
   * RabbitTemplate with JSON message converter and publisher confirms
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter());
    template.setMandatory(true);

    // Publisher confirms callback
    template.setConfirmCallback((correlationData, ack, cause) -> {
      if (ack) {
        log.debug("Message confirmed: {}", correlationData);
      } else {
        log.error("Message not confirmed: {}, cause: {}", correlationData, cause);
      }
    });

    // Publisher returns callback
    template.setReturnsCallback(returned -> {
      log.error("Message returned: {}, reply code: {}, reply text: {}, exchange: {}, routing key: {}",
          returned.getMessage(),
          returned.getReplyCode(),
          returned.getReplyText(),
          returned.getExchange(),
          returned.getRoutingKey());
    });

    return template;
  }

  /**
   * Listener container factory with JSON message converter
   */
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      SimpleRabbitListenerContainerFactoryConfigurer configurer) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(messageConverter());
    return factory;
  }
}