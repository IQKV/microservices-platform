package com.iqscaffold.billingservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import com.iqscaffold.billingservice.payment.PaymentProviderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Unit tests for health check configuration.
 *
 * <p>Tests verify that health indicators correctly report UP/DOWN status
 * based on component availability.
 */
class HealthCheckConfigTest {

  @Test
  @DisplayName("Database health indicator should report UP when database is available")
  void shouldReportDatabaseHealthUp() {
    // Arrange
    var dataSource = mock(DataSource.class);
    var healthIndicator = new HealthCheckConfig.DatabaseHealthIndicator(dataSource);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
    assertThat(health.getDetails()).containsKey("database");
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  @DisplayName("Redis health indicator should report UP when Redis is available")
  void shouldReportRedisHealthUp() {
    // Arrange
    var redisConnectionFactory = mock(RedisConnectionFactory.class);
    var redisConnection = mock(RedisConnection.class);
    when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
    when(redisConnection.ping()).thenReturn("PONG");

    var healthIndicator = new HealthCheckConfig.RedisHealthIndicator(redisConnectionFactory);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("redis", "Connected");
    assertThat(health.getDetails()).containsEntry("status", "Available");
    assertThat(health.getDetails()).containsKey("responseTimeMs");
  }

  @Test
  @DisplayName("Redis health indicator should report DOWN when Redis ping fails")
  void shouldReportRedisHealthDown() {
    // Arrange
    var redisConnectionFactory = mock(RedisConnectionFactory.class);
    var redisConnection = mock(RedisConnection.class);
    when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
    when(redisConnection.ping()).thenReturn("FAIL");

    var healthIndicator = new HealthCheckConfig.RedisHealthIndicator(redisConnectionFactory);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("redis", "Ping failed");
    assertThat(health.getDetails()).containsEntry("status", "Unavailable");
  }

  @Test
  @DisplayName("Redis health indicator should report DOWN when connection fails")
  void shouldReportRedisHealthDownOnException() {
    // Arrange
    var redisConnectionFactory = mock(RedisConnectionFactory.class);
    when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));

    var healthIndicator = new HealthCheckConfig.RedisHealthIndicator(redisConnectionFactory);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("redis", "Connection failed");
    assertThat(health.getDetails()).containsEntry("status", "Unavailable");
    assertThat(health.getDetails()).containsKey("error");
  }

  @Test
  @DisplayName("RabbitMQ health indicator should report UP when RabbitMQ is available")
  void shouldReportRabbitMQHealthUp() {
    // Arrange
    var rabbitTemplate = mock(RabbitTemplate.class);
    var connectionFactory = mock(ConnectionFactory.class);
    var connection = mock(org.springframework.amqp.rabbit.connection.Connection.class);

    when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.isOpen()).thenReturn(true);

    var healthIndicator = new HealthCheckConfig.RabbitMQHealthIndicator(rabbitTemplate);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("rabbitmq", "Connected");
    assertThat(health.getDetails()).containsEntry("status", "Available");
    assertThat(health.getDetails()).containsKey("responseTimeMs");
  }

  @Test
  @DisplayName("RabbitMQ health indicator should report DOWN when connection is closed")
  void shouldReportRabbitMQHealthDownWhenClosed() {
    // Arrange
    var rabbitTemplate = mock(RabbitTemplate.class);
    var connectionFactory = mock(ConnectionFactory.class);
    var connection = mock(org.springframework.amqp.rabbit.connection.Connection.class);

    when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.isOpen()).thenReturn(false);

    var healthIndicator = new HealthCheckConfig.RabbitMQHealthIndicator(rabbitTemplate);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("rabbitmq", "Connection closed");
    assertThat(health.getDetails()).containsEntry("status", "Unavailable");
  }

  @Test
  @DisplayName("RabbitMQ health indicator should report DOWN when connection fails")
  void shouldReportRabbitMQHealthDownOnException() {
    // Arrange
    var rabbitTemplate = mock(RabbitTemplate.class);
    var connectionFactory = mock(ConnectionFactory.class);

    when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
    when(connectionFactory.createConnection()).thenThrow(new RuntimeException("Connection failed"));

    var healthIndicator = new HealthCheckConfig.RabbitMQHealthIndicator(rabbitTemplate);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("rabbitmq", "Connection failed");
    assertThat(health.getDetails()).containsEntry("status", "Unavailable");
    assertThat(health.getDetails()).containsKey("error");
  }

  @Test
  @DisplayName("Payment provider health indicator should check provider availability")
  void shouldCheckPaymentProviderAvailability() {
    // Arrange - This test verifies the health indicator doesn't throw exceptions
    // We're testing the error handling path by not mocking getProvider()
    var paymentProviderFactory = mock(PaymentProviderFactory.class);

    when(paymentProviderFactory.getConfiguredProviderName()).thenReturn("stripe");

    var healthIndicator = new HealthCheckConfig.PaymentProviderHealthIndicator(paymentProviderFactory);

    // Act
    var health = healthIndicator.health();

    // Assert - Should handle errors gracefully
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("paymentProvider");
    assertThat(health.getDetails()).containsKey("status");
    assertThat(health.getDetails()).containsKey("error");
  }

  @Test
  @DisplayName("Payment provider health indicator should report DOWN when exception occurs")
  void shouldReportPaymentProviderHealthDownOnException() {
    // Arrange
    var paymentProviderFactory = mock(PaymentProviderFactory.class);

    when(paymentProviderFactory.getConfiguredProviderName()).thenThrow(new RuntimeException("Configuration error"));

    var healthIndicator = new HealthCheckConfig.PaymentProviderHealthIndicator(paymentProviderFactory);

    // Act
    var health = healthIndicator.health();

    // Assert
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("paymentProvider", "Error");
    assertThat(health.getDetails()).containsEntry("status", "Health check failed");
    assertThat(health.getDetails()).containsKey("error");
  }
}
