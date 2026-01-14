package com.iqscaffold.pipelineservice.config;

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
 * RabbitMQ configuration for Pipeline Service.
 * Configures exchanges, queues, and bindings for lead lifecycle events.
 */
@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE_NAME = "crm.events";
  public static final String LEAD_CREATED_QUEUE = "pipeline-service.lead.created";
  public static final String LEAD_DELETED_QUEUE = "pipeline-service.lead.deleted";
  public static final String LEAD_CREATED_ROUTING_KEY = "lead.created";
  public static final String LEAD_DELETED_ROUTING_KEY = "lead.deleted";
  public static final String STAGE_CHANGED_ROUTING_KEY = "stage.changed";

  @Bean
  public TopicExchange crmEventsExchange() {
    return new TopicExchange(EXCHANGE_NAME, true, false);
  }

  @Bean
  public Queue leadCreatedQueue() {
    return new Queue(LEAD_CREATED_QUEUE, true);
  }

  @Bean
  public Queue leadDeletedQueue() {
    return new Queue(LEAD_DELETED_QUEUE, true);
  }

  @Bean
  public Binding leadCreatedBinding(final Queue leadCreatedQueue, final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(leadCreatedQueue)
        .to(crmEventsExchange)
        .with(LEAD_CREATED_ROUTING_KEY);
  }

  @Bean
  public Binding leadDeletedBinding(final Queue leadDeletedQueue, final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(leadDeletedQueue)
        .to(crmEventsExchange)
        .with(LEAD_DELETED_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      final ConnectionFactory connectionFactory) {
    final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    return factory;
  }
}
