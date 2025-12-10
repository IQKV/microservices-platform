package com.iqscaffold.billingservice.config;

import javax.sql.DataSource;

import com.iqscaffold.billingservice.payment.PaymentProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for custom health checks in the billing service.
 *
 * <p>Provides health indicators for critical billing service dependencies:
 * <ul>
 *   <li><strong>Database:</strong> PostgreSQL connectivity and performance</li>
 *   <li><strong>Redis:</strong> Cache connectivity and performance</li>
 *   <li><strong>RabbitMQ:</strong> Message broker connectivity</li>
 *   <li><strong>Payment Provider:</strong> External payment provider availability</li>
 * </ul>
 *
 * <h2>Health Check Strategy</h2>
 * <p>Each health indicator performs lightweight checks to verify connectivity
 * and basic functionality without impacting service performance. Health checks
 * are exposed via Spring Boot Actuator at {@code /actuator/health}.
 *
 * <h2>Health Status Levels</h2>
 * <ul>
 *   <li><strong>UP:</strong> Component is healthy and operational</li>
 *   <li><strong>DOWN:</strong> Component is unavailable or failing</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Health check behavior is configured in application.yml:
 * <pre>{@code
 * management:
 *   endpoint:
 *     health:
 *       show-details: when-authorized
 *       probes:
 *         enabled: true
 *   health:
 *     livenessstate:
 *       enabled: true
 *     readinessstate:
 *       enabled: true
 * }</pre>
 *
 * @see HealthIndicator
 * @see Health
 */
@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class HealthCheckConfig {

  private static final Logger log = LoggerFactory.getLogger(HealthCheckConfig.class);

  /**
   * Custom health indicator for database connectivity and performance.
   *
   * <p>Performs a simple query to verify PostgreSQL connectivity and measures
   * response time. This check is critical as the database stores all billing
   * data including subscriptions, invoices, and payments.
   *
   * @param dataSource the PostgreSQL data source
   * @return database health indicator
   */
  @Bean
  public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
    return new DatabaseHealthIndicator(dataSource);
  }

  /**
   * Custom health indicator for Redis connectivity and performance.
   *
   * <p>Verifies Redis connectivity using a PING command and measures response
   * time. Redis is used for caching subscription data, rate limiting, and
   * session management.
   *
   * @param redisConnectionFactory the Redis connection factory
   * @return Redis health indicator
   */
  @Bean
  public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
    return new RedisHealthIndicator(redisConnectionFactory);
  }

  /**
   * Custom health indicator for RabbitMQ connectivity.
   *
   * <p>Verifies RabbitMQ connectivity by checking the connection status.
   * RabbitMQ is used for asynchronous event processing including usage
   * recording, invoice generation, and webhook processing.
   *
   * @param rabbitTemplate the RabbitMQ template
   * @return RabbitMQ health indicator
   */
  @Bean
  public HealthIndicator rabbitMQHealthIndicator(RabbitTemplate rabbitTemplate) {
    return new RabbitMQHealthIndicator(rabbitTemplate);
  }

  /**
   * Custom health indicator for payment provider availability.
   *
   * <p>Checks if the configured payment provider (Stripe, PayPal, or Manual)
   * is properly configured and available. This check verifies that required
   * credentials are present but does not make external API calls.
   *
   * @param paymentProviderFactory the payment provider factory
   * @return payment provider health indicator
   */
  @Bean
  public HealthIndicator paymentProviderHealthIndicator(PaymentProviderFactory paymentProviderFactory) {
    return new PaymentProviderHealthIndicator(paymentProviderFactory);
  }

  /**
   * Database health indicator implementation.
   *
   * <p>Executes a simple {@code SELECT 1} query to verify database connectivity
   * and measures response time. This is a lightweight check that doesn't impact
   * database performance.
   */
  public static class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(final DataSource dataSource) {
      this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Health health() {
      try {
        var startTime = System.currentTimeMillis();

        // Test database connectivity with a simple query
        var result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        var responseTime = System.currentTimeMillis() - startTime;

        if (result != null && result == 1) {
          return Health.up()
              .withDetail("database", "PostgreSQL")
              .withDetail("responseTimeMs", responseTime)
              .withDetail("status", "Connected")
              .build();
        } else {
          return Health.down()
              .withDetail("database", "PostgreSQL")
              .withDetail("responseTimeMs", responseTime)
              .withDetail("status", "Query failed")
              .build();
        }
      } catch (final Exception e) {
        log.error("Database health check failed", e);
        return Health.down()
            .withDetail("database", "PostgreSQL")
            .withDetail("status", "Connection failed")
            .withDetail("error", e.getMessage())
            .build();
      }
    }
  }

  /**
   * Redis health indicator implementation.
   *
   * <p>Executes a PING command to verify Redis connectivity and measures
   * response time. Redis is critical for caching and rate limiting.
   */
  public static class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(final RedisConnectionFactory redisConnectionFactory) {
      this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
      try {
        var startTime = System.currentTimeMillis();

        // Test Redis connectivity
        var connection = redisConnectionFactory.getConnection();
        var pong = connection.ping();
        connection.close();

        var responseTime = System.currentTimeMillis() - startTime;

        if ("PONG".equals(pong)) {
          return Health.up()
              .withDetail("redis", "Connected")
              .withDetail("responseTimeMs", responseTime)
              .withDetail("status", "Available")
              .build();
        } else {
          return Health.down()
              .withDetail("redis", "Ping failed")
              .withDetail("responseTimeMs", responseTime)
              .withDetail("status", "Unavailable")
              .build();
        }
      } catch (final Exception e) {
        log.error("Redis health check failed", e);
        return Health.down()
            .withDetail("redis", "Connection failed")
            .withDetail("status", "Unavailable")
            .withDetail("error", e.getMessage())
            .build();
      }
    }
  }

  /**
   * RabbitMQ health indicator implementation.
   *
   * <p>Checks RabbitMQ connection status to verify message broker availability.
   * RabbitMQ is used for asynchronous event processing and is critical for
   * usage recording, invoice generation, and webhook processing.
   */
  public static class RabbitMQHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQHealthIndicator.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQHealthIndicator(final RabbitTemplate rabbitTemplate) {
      this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public Health health() {
      try {
        var startTime = System.currentTimeMillis();

        // Test RabbitMQ connectivity by checking connection
        var connectionFactory = rabbitTemplate.getConnectionFactory();
        var connection = connectionFactory.createConnection();

        var responseTime = System.currentTimeMillis() - startTime;

        if (connection != null && connection.isOpen()) {
          connection.close();
          return Health.up()
              .withDetail("rabbitmq", "Connected")
              .withDetail("responseTimeMs", responseTime)
              .withDetail("status", "Available")
              .build();
        } else {
          if (connection != null) {
            connection.close();
          }
          return Health.down()
              .withDetail("rabbitmq", "Connection closed")
              .withDetail("responseTimeMs", responseTime)
              .withDetail("status", "Unavailable")
              .build();
        }
      } catch (final Exception e) {
        log.error("RabbitMQ health check failed", e);
        return Health.down()
            .withDetail("rabbitmq", "Connection failed")
            .withDetail("status", "Unavailable")
            .withDetail("error", e.getMessage())
            .build();
      }
    }
  }

  /**
   * Payment provider health indicator implementation.
   *
   * <p>Verifies that the configured payment provider (Stripe, PayPal, or Manual)
   * is properly configured with required credentials. This check does not make
   * external API calls to avoid impacting health check performance and external
   * rate limits.
   *
   * <h3>Health Check Logic</h3>
   * <ul>
   *   <li><strong>UP:</strong> Configured provider is available with credentials</li>
   *   <li><strong>UP (Fallback):</strong> Configured provider unavailable, using manual fallback</li>
   *   <li><strong>DOWN:</strong> No provider available (should never happen)</li>
   * </ul>
   */
  public static class PaymentProviderHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviderHealthIndicator.class);

    private final PaymentProviderFactory paymentProviderFactory;

    public PaymentProviderHealthIndicator(final PaymentProviderFactory paymentProviderFactory) {
      this.paymentProviderFactory = paymentProviderFactory;
    }

    @Override
    public Health health() {
      try {
        var startTime = System.currentTimeMillis();

        // Get configured provider name
        String configuredProvider = paymentProviderFactory.getConfiguredProviderName();

        // Check if configured provider is available
        boolean isConfiguredProviderAvailable = paymentProviderFactory.isConfiguredProviderAvailable();

        // Get actual provider (may be fallback)
        var actualProvider = paymentProviderFactory.getProvider();
        String actualProviderName = actualProvider.getProviderName();

        var responseTime = System.currentTimeMillis() - startTime;

        // Build health status based on provider availability
        if (isConfiguredProviderAvailable) {
          return Health.up()
              .withDetail("paymentProvider", actualProviderName)
              .withDetail("configuredProvider", configuredProvider)
              .withDetail("status", "Available")
              .withDetail("fallback", false)
              .withDetail("responseTimeMs", responseTime)
              .build();
        } else if ("manual".equalsIgnoreCase(actualProviderName)) {
          // Configured provider unavailable, using manual fallback
          return Health.up()
              .withDetail("paymentProvider", actualProviderName)
              .withDetail("configuredProvider", configuredProvider)
              .withDetail("status", "Fallback to manual provider")
              .withDetail("fallback", true)
              .withDetail("responseTimeMs", responseTime)
              .build();
        } else {
          // Should never happen, but handle gracefully
          return Health.down()
              .withDetail("paymentProvider", "None")
              .withDetail("configuredProvider", configuredProvider)
              .withDetail("status", "No provider available")
              .withDetail("responseTimeMs", responseTime)
              .build();
        }
      } catch (final Exception e) {
        log.error("Payment provider health check failed", e);
        return Health.down()
            .withDetail("paymentProvider", "Error")
            .withDetail("status", "Health check failed")
            .withDetail("error", e.getMessage())
            .build();
      }
    }
  }
}
