package com.iqscaffold.contactservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Contact Service.
 * Configures exchanges and message converters for contact lifecycle events.
 */
@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE_NAME = "crm.events";
  public static final String CONTACT_CREATED_ROUTING_KEY = "contact.created";
  public static final String CONTACT_UPDATED_ROUTING_KEY = "contact.updated";
  public static final String CONTACT_DELETED_ROUTING_KEY = "contact.deleted";

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
