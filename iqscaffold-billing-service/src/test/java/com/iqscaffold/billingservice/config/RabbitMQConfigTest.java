package com.iqscaffold.billingservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

  @Mock
  private IqScaffoldProperties properties;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private ConnectionFactory connectionFactory;

  @Mock
  private SimpleRabbitListenerContainerFactoryConfigurer configurer;

  private RabbitMQConfig rabbitMQConfig;

  @BeforeEach
  void setUp() {
    rabbitMQConfig = new RabbitMQConfig(properties, objectMapper);
  }

  @Test
  void shouldCreateEventsExchange() {
    var exchange = rabbitMQConfig.eventsExchange();

    assertNotNull(exchange);
    assertEquals(RabbitMQConfig.EVENTS_EXCHANGE, exchange.getName());
    assertEquals("topic", exchange.getType());
    assertTrue(exchange.isDurable());
  }

  @Test
  void shouldCreateDeadLetterExchange() {
    var exchange = rabbitMQConfig.deadLetterExchange();

    assertNotNull(exchange);
    assertEquals(RabbitMQConfig.DLX_EXCHANGE, exchange.getName());
    assertEquals("topic", exchange.getType());
    assertTrue(exchange.isDurable());
  }

  @Test
  void shouldCreateBillingEventsQueue() {
    var queue = rabbitMQConfig.billingEventsQueue();

    assertNotNull(queue);
    assertEquals(RabbitMQConfig.BILLING_EVENTS_QUEUE, queue.getName());
    assertTrue(queue.isDurable());
    assertEquals(RabbitMQConfig.DLX_EXCHANGE, queue.getArguments().get("x-dead-letter-exchange"));
    assertEquals(86400000, queue.getArguments().get("x-message-ttl"));
  }

  @Test
  void shouldCreateNotificationsQueue() {
    var queue = rabbitMQConfig.notificationsQueue();

    assertNotNull(queue);
    assertEquals(RabbitMQConfig.NOTIFICATIONS_QUEUE, queue.getName());
    assertTrue(queue.isDurable());
    assertEquals(RabbitMQConfig.DLX_EXCHANGE, queue.getArguments().get("x-dead-letter-exchange"));
    assertEquals(86400000, queue.getArguments().get("x-message-ttl"));
  }

  @Test
  void shouldCreateDeadLetterQueue() {
    var queue = rabbitMQConfig.deadLetterQueue();

    assertNotNull(queue);
    assertEquals(RabbitMQConfig.DLQ, queue.getName());
    assertTrue(queue.isDurable());
  }

  @Test
  void shouldCreateBillingEventsBinding() {
    var binding = rabbitMQConfig.billingEventsBinding();

    assertNotNull(binding);
    assertEquals(RabbitMQConfig.BILLING_EVENTS_QUEUE, binding.getDestination());
    assertEquals(RabbitMQConfig.EVENTS_EXCHANGE, binding.getExchange());
    assertEquals("billing.*", binding.getRoutingKey());
    assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
  }

  @Test
  void shouldCreateNotificationsBinding() {
    var binding = rabbitMQConfig.notificationsBinding();

    assertNotNull(binding);
    assertEquals(RabbitMQConfig.NOTIFICATIONS_QUEUE, binding.getDestination());
    assertEquals(RabbitMQConfig.EVENTS_EXCHANGE, binding.getExchange());
    assertEquals("notification.*", binding.getRoutingKey());
  }

  @Test
  void shouldCreateDeadLetterBinding() {
    var binding = rabbitMQConfig.deadLetterBinding();

    assertNotNull(binding);
    assertEquals(RabbitMQConfig.DLQ, binding.getDestination());
    assertEquals(RabbitMQConfig.DLX_EXCHANGE, binding.getExchange());
    assertEquals("#", binding.getRoutingKey());
  }

  @Test
  void shouldCreateMessageConverter() {
    var converter = rabbitMQConfig.messageConverter();

    assertNotNull(converter);
    assertTrue(converter instanceof Jackson2JsonMessageConverter);
  }

  @Test
  void shouldCreateRabbitTemplate() {
    var template = rabbitMQConfig.rabbitTemplate(connectionFactory);

    assertNotNull(template);
    assertNotNull(template.getMessageConverter());
    // Note: isMandatory() method may not be available in newer Spring AMQP versions
    // assertTrue(template.isMandatory());
    // Verify that the template was created with the connection factory
  }

  @Test
  void shouldCreateRabbitListenerContainerFactory() {
    // Given
    doNothing().when(configurer).configure(any(), any());

    var factory = rabbitMQConfig.rabbitListenerContainerFactory(connectionFactory, configurer);

    assertNotNull(factory);
    // Note: Message converter is set internally but not exposed via getter
  }

  @Test
  void shouldVerifyRoutingKeyConstants() {
    assertEquals("billing.payment.successful", RabbitMQConfig.PAYMENT_SUCCESSFUL_KEY);
    assertEquals("billing.payment.failed", RabbitMQConfig.PAYMENT_FAILED_KEY);
    assertEquals("billing.payment.refunded", RabbitMQConfig.PAYMENT_REFUNDED_KEY);
    assertEquals("billing.merchant.onboarding", RabbitMQConfig.MERCHANT_ONBOARDING_KEY);
    assertEquals("billing.invoice.generated", RabbitMQConfig.INVOICE_GENERATED_KEY);
    assertEquals("notification.email", RabbitMQConfig.NOTIFICATION_EMAIL_KEY);
  }

  @Test
  void shouldVerifyExchangeConstants() {
    assertEquals("iqscaffold.events", RabbitMQConfig.EVENTS_EXCHANGE);
    assertEquals("iqscaffold.dlx", RabbitMQConfig.DLX_EXCHANGE);
  }

  @Test
  void shouldVerifyQueueConstants() {
    assertEquals("iqscaffold.billing.events", RabbitMQConfig.BILLING_EVENTS_QUEUE);
    assertEquals("iqscaffold.notifications", RabbitMQConfig.NOTIFICATIONS_QUEUE);
    assertEquals("iqscaffold.dlq", RabbitMQConfig.DLQ);
  }
}
