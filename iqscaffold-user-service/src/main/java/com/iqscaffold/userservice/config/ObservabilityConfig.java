package com.iqscaffold.userservice.config;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for observability features including OpenTelemetry, metrics, and logging. Provides correlation ID generation, MDC management, and custom metrics with environment-specific
 * settings.
 */
@Configuration
@EnableConfigurationProperties(IqScaffoldProperties.class)
@AutoConfiguration(before = ObservationAutoConfiguration.class)
public class ObservabilityConfig {

  private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

  private final IqScaffoldProperties.Observability observabilityProperties;
  private final Environment environment;

  public ObservabilityConfig(final IqScaffoldProperties properties, final Environment environment) {
    this.observabilityProperties = properties.observability();
    this.environment = environment;
  }

  /**
   * Configures OpenTelemetry SDK with environment-specific settings.
   */
  @Bean
  @ConditionalOnProperty(name = "iqscaffold.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
  public OpenTelemetry openTelemetry() {
    var tracingProps = observabilityProperties.tracing();

    // Create a resource with service information
    var resource = Resource.getDefault()
        .merge(Resource.create(Attributes.builder()
            .put("service.name", tracingProps.serviceName())
            .put("service.version", "1.0.0")
            .put("deployment.environment", getActiveProfile())
            .build()));

    // Configure OTLP exporter with environment-specific settings
    var spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(tracingProps.endpoint())
        .setTimeout(tracingProps.timeout())
        .build();

    // Configure tracer provider with sampling
    var tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
            .setMaxExportBatchSize(512)
            .setScheduleDelay(Duration.ofSeconds(5))
            .build())
        .setResource(resource)
        .setSampler(Sampler.traceIdRatioBased(tracingProps.samplingRate()))
        .build();

    // Build OpenTelemetry SDK
    var openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .buildAndRegisterGlobal();

    logger.info("OpenTelemetry configured for service: {} with endpoint: {} and sampling rate: {}",
        tracingProps.serviceName(), tracingProps.endpoint(), tracingProps.samplingRate());

    return openTelemetry;
  }

  /**
   * Enables @Observed annotation support for automatic observation of methods.
   */
  @Bean
  public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
    return new ObservedAspect(observationRegistry);
  }

  /**
   * Custom metrics for authentication service monitoring.
   */
  @Bean
  public UserServiceMetrics userServiceMetrics(MeterRegistry meterRegistry) {
    return new UserServiceMetrics(meterRegistry);
  }

  /**
   * Gets the active Spring profile for environment identification.
   */
  private String getActiveProfile() {
    var activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length > 0 ? activeProfiles[0] : "default";
  }

  /**
   * Custom metrics for authentication service.
   */
  public static class UserServiceMetrics {

    private final Timer authenticationTimer;
    private final Timer userRegistrationTimer;
    private final Timer tokenRefreshTimer;
    private final Timer databaseQueryTimer;
    private final Timer redisOperationTimer;
    private final MeterRegistry meterRegistry;

    public UserServiceMetrics(final MeterRegistry meterRegistry) {
      this.meterRegistry = meterRegistry;
      this.authenticationTimer = Timer.builder("iqscaffold.auth.authentication.duration")
          .description("Time taken for user authentication")
          .register(meterRegistry);
      this.userRegistrationTimer = Timer.builder("iqscaffold.auth.registration.duration")
          .description("Time taken for user registration")
          .register(meterRegistry);
      this.tokenRefreshTimer = Timer.builder("iqscaffold.auth.token.refresh.duration")
          .description("Time taken for token refresh")
          .register(meterRegistry);
      this.databaseQueryTimer = Timer.builder("iqscaffold.auth.database.query.duration")
          .description("Time taken for database queries")
          .register(meterRegistry);
      this.redisOperationTimer = Timer.builder("iqscaffold.auth.redis.operation.duration")
          .description("Time taken for Redis operations")
          .register(meterRegistry);
    }

    public Timer.Sample startAuthenticationTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordAuthenticationSuccess(Timer.Sample sample) {
      sample.stop(Timer.builder("iqscaffold.auth.authentication.success")
          .description("Successful authentication attempts")
          .register(meterRegistry));
      meterRegistry.counter("iqscaffold.auth.authentication.total", "result", "success").increment();
    }

    public void recordAuthenticationFailure(String reason) {
      meterRegistry.counter("iqscaffold.auth.authentication.total", "result", "failure", "reason", reason).increment();
    }

    public Timer.Sample startRegistrationTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordRegistrationSuccess(Timer.Sample sample) {
      sample.stop(userRegistrationTimer);
      meterRegistry.counter("iqscaffold.auth.registration.total", "result", "success").increment();
    }

    public void recordRegistrationFailure(String reason) {
      meterRegistry.counter("iqscaffold.auth.registration.total", "result", "failure", "reason", reason).increment();
    }

    public Timer.Sample startTokenRefreshTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordTokenRefreshSuccess(Timer.Sample sample) {
      sample.stop(tokenRefreshTimer);
      meterRegistry.counter("iqscaffold.auth.token.refresh.total", "result", "success").increment();
    }

    public void recordTokenRefreshFailure(String reason) {
      meterRegistry.counter("iqscaffold.auth.token.refresh.total", "result", "failure", "reason", reason).increment();
    }

    public Timer.Sample startDatabaseQueryTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordDatabaseQuerySuccess(Timer.Sample sample, String operation) {
      sample.stop(Timer.builder("iqscaffold.auth.database.query.success")
          .tag("operation", operation)
          .description("Successful database queries")
          .register(meterRegistry));
      meterRegistry.counter("iqscaffold.auth.database.query.total", "result", "success", "operation", operation).increment();
    }

    public void recordDatabaseQueryFailure(String operation, String reason) {
      meterRegistry.counter("iqscaffold.auth.database.query.total",
          "result", "failure",
          "operation", operation,
          "reason", reason).increment();
    }

    public Timer.Sample startRedisOperationTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordRedisOperationSuccess(Timer.Sample sample, String operation) {
      sample.stop(Timer.builder("iqscaffold.auth.redis.operation.success")
          .tag("operation", operation)
          .description("Successful Redis operations")
          .register(meterRegistry));
      meterRegistry.counter("iqscaffold.auth.redis.operation.total", "result", "success", "operation", operation).increment();
    }

    public void recordRedisOperationFailure(String operation, String reason) {
      meterRegistry.counter("iqscaffold.auth.redis.operation.total",
          "result", "failure",
          "operation", operation,
          "reason", reason).increment();
    }

    public void recordActiveUsers(int count) {
      meterRegistry.gauge("iqscaffold.auth.users.active", count);
    }

    public void recordTotalUsers(int count) {
      meterRegistry.gauge("iqscaffold.auth.users.total", count);
    }

    public void recordFailedLoginAttempts(String username, String reason) {
      meterRegistry.counter("iqscaffold.auth.login.failed", "reason", reason).increment();
    }

    public void recordPasswordResetRequest(String method) {
      meterRegistry.counter("iqscaffold.auth.password.reset.request", "method", method).increment();
    }

    public void recordAccountLockout(String reason) {
      meterRegistry.counter("iqscaffold.auth.account.lockout", "reason", reason).increment();
    }
  }
}
