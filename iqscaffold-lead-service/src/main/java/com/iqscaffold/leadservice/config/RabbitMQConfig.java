package com.iqscaffold.leadservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Lead Service.
 * Configures exchanges, queues, and bindings for CRM events.
 */
@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE_NAME = "crm.events";
  public static final String CONTACT_CREATED_QUEUE = "lead-service.contact.created";
  public static final String CONTACT_UPDATED_QUEUE = "lead-service.contact.updated";
  public static final String CONTACT_DELETED_QUEUE = "lead-service.contact.deleted";
  public static final String CONTACT_CREATED_ROUTING_KEY = "contact.created";
  public static final String CONTACT_UPDATED_ROUTING_KEY = "contact.updated";
  public static final String CONTACT_DELETED_ROUTING_KEY = "contact.deleted";
  public static final String LEAD_CREATED_ROUTING_KEY = "lead.created";
  public static final String LEAD_UPDATED_ROUTING_KEY = "lead.updated";
  public static final String LEAD_DELETED_ROUTING_KEY = "lead.deleted";
  public static final String LEAD_CONVERTED_ROUTING_KEY = "lead.converted";

  /**
   * Creates the CRM events topic exchange.
   * This exchange is shared across all CRM services.
   *
   * @return TopicExchange for CRM events
   */
  @Bean
  public TopicExchange crmEventsExchange() {
    return new TopicExchange(EXCHANGE_NAME, true, false);
  }

  /**
   * Creates queue for contact created events.
   *
   * @return Durable queue for contact created events
   */
  @Bean
  public Queue contactCreatedQueue() {
    return new Queue(CONTACT_CREATED_QUEUE, true);
  }

  /**
   * Creates queue for contact updated events.
   *
   * @return Durable queue for contact updated events
   */
  @Bean
  public Queue contactUpdatedQueue() {
    return new Queue(CONTACT_UPDATED_QUEUE, true);
  }

  /**
   * Creates queue for contact deleted events.
   *
   * @return Durable queue for contact deleted events
   */
  @Bean
  public Queue contactDeletedQueue() {
    return new Queue(CONTACT_DELETED_QUEUE, true);
  }

  /**
   * Binds contact created queue to exchange.
   *
   * @param contactCreatedQueue Queue to bind
   * @param crmEventsExchange   Exchange to bind to
   * @return Binding configuration
   */
  @Bean
  public Binding contactCreatedBinding(
      final Queue contactCreatedQueue,
      final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(contactCreatedQueue)
        .to(crmEventsExchange)
        .with(CONTACT_CREATED_ROUTING_KEY);
  }

  /**
   * Binds contact updated queue to exchange.
   *
   * @param contactUpdatedQueue Queue to bind
   * @param crmEventsExchange   Exchange to bind to
   * @return Binding configuration
   */
  @Bean
  public Binding contactUpdatedBinding(
      final Queue contactUpdatedQueue,
      final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(contactUpdatedQueue)
        .to(crmEventsExchange)
        .with(CONTACT_UPDATED_ROUTING_KEY);
  }

  /**
   * Binds contact deleted queue to exchange.
   *
   * @param contactDeletedQueue Queue to bind
   * @param crmEventsExchange   Exchange to bind to
   * @return Binding configuration
   */
  @Bean
  public Binding contactDeletedBinding(
      final Queue contactDeletedQueue,
      final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(contactDeletedQueue)
        .to(crmEventsExchange)
        .with(CONTACT_DELETED_ROUTING_KEY);
  }

  /**
   * Creates a JSON message converter for RabbitMQ messages.
   *
   * @return Jackson2JsonMessageConverter
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * Creates a RabbitTemplate with JSON message converter.
   *
   * @param connectionFactory RabbitMQ connection factory
   * @return Configured RabbitTemplate
   */
  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }

  /**
   * Creates a RabbitListener container factory with JSON message converter.
   *
   * @param connectionFactory RabbitMQ connection factory
   * @return Configured SimpleRabbitListenerContainerFactory
   */
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      final ConnectionFactory connectionFactory) {
    final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    return factory;
  }
}
