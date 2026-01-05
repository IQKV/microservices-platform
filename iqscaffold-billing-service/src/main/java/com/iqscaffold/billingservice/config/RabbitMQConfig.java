package com.iqscaffold.billingservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for billing service messaging
 */
@Configuration
public class RabbitMQConfig {

  // Exchange names
  public static final String EVENTS_EXCHANGE = "iqscaffold.events";

  // Routing keys
  public static final String PAYMENT_SUCCESSFUL_KEY = "billing.payment.successful";
  public static final String PAYMENT_FAILED_KEY = "billing.payment.failed";
  public static final String PAYMENT_REFUNDED_KEY = "billing.payment.refunded";
  public static final String MERCHANT_ONBOARDING_KEY = "billing.merchant.onboarding";
  public static final String INVOICE_GENERATED_KEY = "billing.invoice.generated";
  public static final String NOTIFICATION_EMAIL_KEY = "notification.email";

  // Queue names
  public static final String BILLING_EVENTS_QUEUE = "billing.events";
  public static final String NOTIFICATION_QUEUE = "notification.email";

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange(EVENTS_EXCHANGE, true, false);
  }

  @Bean
  public Queue billingEventsQueue() {
    return new Queue(BILLING_EVENTS_QUEUE, true);
  }

  @Bean
  public Queue notificationQueue() {
    return new Queue(NOTIFICATION_QUEUE, true);
  }

  @Bean
  public Binding paymentSuccessfulBinding() {
    return BindingBuilder.bind(billingEventsQueue())
        .to(eventsExchange())
        .with(PAYMENT_SUCCESSFUL_KEY);
  }

  @Bean
  public Binding paymentFailedBinding() {
    return BindingBuilder.bind(billingEventsQueue())
        .to(eventsExchange())
        .with(PAYMENT_FAILED_KEY);
  }

  @Bean
  public Binding paymentRefundedBinding() {
    return BindingBuilder.bind(billingEventsQueue())
        .to(eventsExchange())
        .with(PAYMENT_REFUNDED_KEY);
  }

  @Bean
  public Binding merchantOnboardingBinding() {
    return BindingBuilder.bind(billingEventsQueue())
        .to(eventsExchange())
        .with(MERCHANT_ONBOARDING_KEY);
  }

  @Bean
  public Binding invoiceGeneratedBinding() {
    return BindingBuilder.bind(billingEventsQueue())
        .to(eventsExchange())
        .with(INVOICE_GENERATED_KEY);
  }

  @Bean
  public Binding notificationBinding() {
    return BindingBuilder.bind(notificationQueue())
        .to(eventsExchange())
        .with(NOTIFICATION_EMAIL_KEY);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    return template;
  }
}