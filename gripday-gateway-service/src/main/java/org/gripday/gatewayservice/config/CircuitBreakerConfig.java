package org.gripday.gatewayservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Circuit breaker configuration using Resilience4j. Provides fault tolerance patterns for service failures.
 */
@Configuration
public class CircuitBreakerConfig {

  private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfig.class);

  private final GripdayProperties gripdayProperties;

  public CircuitBreakerConfig(final GripdayProperties gripdayProperties) {
    this.gripdayProperties = gripdayProperties;
  }

  @Bean
  public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
    var cbConfig = gripdayProperties.gateway().circuitBreaker();

    return factory -> {
      if (cbConfig.enabled()) {
        factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(createCircuitBreakerConfig())
            .build());

        logger.info("Circuit breaker configured with failure rate threshold: {}%, slow call threshold: {}%",
            cbConfig.failureRateThreshold(), cbConfig.slowCallRateThreshold());
      } else {
        logger.info("Circuit breaker is disabled");
      }
    };
  }

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    var config = createCircuitBreakerConfig();
    var registry = CircuitBreakerRegistry.of(config);

    // Event listeners can be added here for monitoring if needed
    logger.info("Circuit breaker registry configured with {} circuit breakers", registry.getAllCircuitBreakers().size());

    return registry;
  }

  @Bean
  public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("user-service");
  }

  private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig createCircuitBreakerConfig() {
    var cbConfig = gripdayProperties.gateway().circuitBreaker();

    return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
        .failureRateThreshold(cbConfig.failureRateThreshold())
        .slowCallRateThreshold(cbConfig.slowCallRateThreshold())
        .slowCallDurationThreshold(cbConfig.slowCallDurationThreshold())
        .minimumNumberOfCalls(cbConfig.minimumNumberOfCalls())
        .waitDurationInOpenState(cbConfig.waitDurationInOpenState())
        .slidingWindowSize(cbConfig.slidingWindowSize())
        .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.valueOf(cbConfig.slidingWindowType()))
        .permittedNumberOfCallsInHalfOpenState(5)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .recordExceptions(
            java.net.ConnectException.class,
            java.net.SocketTimeoutException.class,
            java.io.IOException.class,
            org.springframework.web.reactive.function.client.WebClientRequestException.class
        )
        .ignoreExceptions(
            IllegalArgumentException.class,
            IllegalStateException.class
        )
        .build();
  }
}