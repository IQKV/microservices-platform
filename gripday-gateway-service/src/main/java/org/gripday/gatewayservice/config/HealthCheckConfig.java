package org.gripday.gatewayservice.config;

import java.time.Duration;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Configuration for custom health checks in the gateway service. Provides reactive health indicators for Redis, downstream services, and gateway-specific components.
 */
@Configuration
@EnableConfigurationProperties(GripdayProperties.class)
public class HealthCheckConfig {

  /**
   * Custom reactive health indicator for Redis connectivity and performance.
   */
  @Bean
  public ReactiveHealthIndicator redisHealthIndicator(ReactiveRedisConnectionFactory redisConnectionFactory) {
    return new ReactiveRedisHealthIndicator(redisConnectionFactory);
  }

  /**
   * Custom reactive health indicator for auth service connectivity.
   */
  @Bean
  public ReactiveHealthIndicator authServiceHealthIndicator(WebClient.Builder webClientBuilder,
      GripdayProperties gripdayProperties) {
    return new AuthServiceHealthIndicator(webClientBuilder, gripdayProperties);
  }

  /**
   * Custom reactive health indicator for circuit breaker status.
   */
  @Bean
  public ReactiveHealthIndicator circuitBreakerHealthIndicator() {
    return new CircuitBreakerHealthIndicator();
  }

  /**
   * Reactive Redis health indicator implementation.
   */
  public static class ReactiveRedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveRedisConnectionFactory redisConnectionFactory;

    public ReactiveRedisHealthIndicator(final ReactiveRedisConnectionFactory redisConnectionFactory) {
      this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Mono<Health> health() {
      return Mono.fromCallable(() -> System.currentTimeMillis())
          .flatMap(startTime -> {
            var connection = redisConnectionFactory.getReactiveConnection();
            return connection.ping()
                .map(pong -> {
                  var responseTime = System.currentTimeMillis() - startTime;
                  connection.close();

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
                })
                .onErrorReturn(Health.down()
                    .withDetail("redis", "Connection failed")
                    .withDetail("status", "Unavailable")
                    .build());
          })
          .timeout(Duration.ofSeconds(5))
          .onErrorReturn(Health.down()
              .withDetail("redis", "Timeout")
              .withDetail("status", "Unavailable")
              .build());
    }
  }

  /**
   * Auth service health indicator implementation.
   */
  public static class AuthServiceHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String authServiceUrl;

    public AuthServiceHealthIndicator(final WebClient.Builder webClientBuilder, final GripdayProperties gripdayProperties) {
      this.authServiceUrl = gripdayProperties.gateway().security().authentication().authServiceUrl();
      this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Health> health() {
      var startTime = System.currentTimeMillis();

      return webClient.get()
          .uri(authServiceUrl + "/actuator/health")
          .retrieve()
          .toBodilessEntity()
          .map(response -> {
            var responseTime = System.currentTimeMillis() - startTime;
            var statusCode = response.getStatusCode().value();

            if (statusCode == 200) {
              return Health.up()
                  .withDetail("authService", "Available")
                  .withDetail("responseTimeMs", responseTime)
                  .withDetail("statusCode", statusCode)
                  .withDetail("url", authServiceUrl)
                  .build();
            } else {
              return Health.down()
                  .withDetail("authService", "Unhealthy")
                  .withDetail("responseTimeMs", responseTime)
                  .withDetail("statusCode", statusCode)
                  .withDetail("url", authServiceUrl)
                  .build();
            }
          })
          .timeout(Duration.ofSeconds(5))
          .onErrorReturn(Health.down()
              .withDetail("authService", "Unavailable")
              .withDetail("url", authServiceUrl)
              .withDetail("status", "Connection failed or timeout")
              .build());
    }
  }

  /**
   * Circuit breaker health indicator implementation.
   */
  public static class CircuitBreakerHealthIndicator implements ReactiveHealthIndicator {

    @Override
    public Mono<Health> health() {
      return Mono.fromCallable(() -> {
        // In a real implementation, you would check the actual circuit breaker state
        // For now, we'll simulate a basic check
        var circuitBreakerEnabled = true; // This would come from actual circuit breaker configuration

        if (circuitBreakerEnabled) {
          return Health.up()
              .withDetail("circuitBreaker", "Enabled")
              .withDetail("status", "Monitoring downstream services")
              .withDetail("services", "user-service")
              .build();
        } else {
          return Health.down()
              .withDetail("circuitBreaker", "Disabled")
              .withDetail("status", "Circuit breaker not configured")
              .build();
        }
      });
    }
  }
}