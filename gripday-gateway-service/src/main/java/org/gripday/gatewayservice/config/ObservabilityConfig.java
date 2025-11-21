package org.gripday.gatewayservice.config;

import java.time.Duration;
import java.util.UUID;

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
import org.gripday.gatewayservice.common.GatewayConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Configuration for observability features in the gateway service. Provides correlation ID generation, MDC management, and custom metrics for reactive applications with environment-specific
 * settings.
 */
@Configuration
@EnableConfigurationProperties(GripdayProperties.class)
@AutoConfiguration(before = ObservationAutoConfiguration.class)
public class ObservabilityConfig {

  private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

  private final GripdayProperties gripdayProperties;
  private final Environment environment;

  public ObservabilityConfig(final GripdayProperties gripdayProperties, final Environment environment) {
    this.gripdayProperties = gripdayProperties;
    this.environment = environment;
  }

  /**
   * Configures OpenTelemetry SDK with environment-specific settings for reactive applications.
   */
  @Bean
  @ConditionalOnProperty(name = "gripday.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
  public OpenTelemetry openTelemetry() {
    var tracingProps = gripdayProperties.observability().tracing();

    // Create resource with service information
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
   * Global filter to add correlation ID and trace information for reactive requests.
   */
  @Bean
  public CorrelationIdGlobalFilter correlationIdGlobalFilter() {
    return new CorrelationIdGlobalFilter();
  }

  /**
   * Custom metrics for gateway service monitoring.
   */
  @Bean
  public GatewayServiceMetrics gatewayServiceMetrics(MeterRegistry meterRegistry) {
    return new GatewayServiceMetrics(meterRegistry);
  }

  /**
   * Gets the active Spring profile for environment identification.
   */
  private String getActiveProfile() {
    var activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length > 0 ? activeProfiles[0] : "default";
  }

  /**
   * Global filter that adds correlation ID and trace information to reactive context.
   */
  public static class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      var request = exchange.getRequest();
      var response = exchange.getResponse();

      // Generate or extract correlation ID
      var correlationId = getOrGenerateCorrelationId(request);
      var requestId = getOrGenerateRequestId(request);
      var tenantId = request.getHeaders().getFirst(GatewayConstants.Headers.X_TENANT_ID);

      // Add to response headers
      response.getHeaders().add(GatewayConstants.Headers.X_CORRELATION_ID, correlationId);
      response.getHeaders().add(GatewayConstants.Headers.X_REQUEST_ID, requestId);

      // Add to MDC for logging (reactive context)
      return chain.filter(exchange)
          .contextWrite(ctx -> {
            ctx = ctx.put(GatewayConstants.MdcKeys.CORRELATION_ID, correlationId);
            ctx = ctx.put(GatewayConstants.MdcKeys.REQUEST_ID, requestId);
            if (tenantId != null) {
              ctx = ctx.put(GatewayConstants.MdcKeys.TENANT_ID, tenantId);
            }
            return ctx;
          })
          .doOnEach(signal -> {
            if (signal.hasValue() || signal.hasError()) {
              // Set MDC for logging
              MDC.put(GatewayConstants.MdcKeys.CORRELATION_ID, correlationId);
              MDC.put(GatewayConstants.MdcKeys.REQUEST_ID, requestId);
              if (tenantId != null) {
                MDC.put(GatewayConstants.MdcKeys.TENANT_ID, tenantId);
              }
            }
          })
          .doFinally(signalType -> MDC.clear());
    }

    private String getOrGenerateCorrelationId(org.springframework.http.server.reactive.ServerHttpRequest request) {
      var correlationId = request.getHeaders().getFirst(GatewayConstants.Headers.X_CORRELATION_ID);
      return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private String getOrGenerateRequestId(org.springframework.http.server.reactive.ServerHttpRequest request) {
      var requestId = request.getHeaders().getFirst(GatewayConstants.Headers.X_REQUEST_ID);
      return requestId != null ? requestId : "req-" + System.currentTimeMillis();
    }

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }
  }

  /**
   * Custom metrics for gateway service.
   */
  public static class GatewayServiceMetrics {

    private final Timer requestTimer;
    private final Timer authenticationTimer;
    private final Timer rateLimitTimer;
    private final MeterRegistry meterRegistry;

    public GatewayServiceMetrics(final MeterRegistry meterRegistry) {
      this.meterRegistry = meterRegistry;
      this.requestTimer = Timer.builder(GatewayConstants.Metrics.REQUEST_DURATION)
          .description("Time taken for gateway request processing")
          .register(meterRegistry);
      this.authenticationTimer = Timer.builder(GatewayConstants.Metrics.AUTHENTICATION_DURATION)
          .description("Time taken for authentication validation")
          .register(meterRegistry);
      this.rateLimitTimer = Timer.builder(GatewayConstants.Metrics.RATE_LIMIT_DURATION)
          .description("Time taken for rate limit checking")
          .register(meterRegistry);
    }

    public Timer.Sample startRequestTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordRequestSuccess(Timer.Sample sample, String route) {
      sample.stop(Timer.builder(GatewayConstants.Metrics.REQUEST_SUCCESS)
          .tag(GatewayConstants.Metrics.TAG_ROUTE, route)
          .description("Successful gateway requests")
          .register(meterRegistry));
      meterRegistry.counter(GatewayConstants.Metrics.REQUEST_TOTAL,
          GatewayConstants.Metrics.TAG_RESULT, GatewayConstants.Metrics.RESULT_SUCCESS,
          GatewayConstants.Metrics.TAG_ROUTE, route).increment();
    }

    public void recordRequestFailure(String route, String reason, int statusCode) {
      meterRegistry.counter(GatewayConstants.Metrics.REQUEST_TOTAL,
          GatewayConstants.Metrics.TAG_RESULT, GatewayConstants.Metrics.RESULT_FAILURE,
          GatewayConstants.Metrics.TAG_ROUTE, route,
          GatewayConstants.Metrics.TAG_REASON, reason,
          GatewayConstants.Metrics.TAG_STATUS, String.valueOf(statusCode)).increment();
    }

    public Timer.Sample startAuthenticationTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordAuthenticationSuccess(Timer.Sample sample) {
      sample.stop(authenticationTimer);
      meterRegistry.counter(GatewayConstants.Metrics.AUTHENTICATION_TOTAL,
          GatewayConstants.Metrics.TAG_RESULT, GatewayConstants.Metrics.RESULT_SUCCESS).increment();
    }

    public void recordAuthenticationFailure(String reason) {
      meterRegistry.counter(GatewayConstants.Metrics.AUTHENTICATION_TOTAL,
          GatewayConstants.Metrics.TAG_RESULT, GatewayConstants.Metrics.RESULT_FAILURE,
          GatewayConstants.Metrics.TAG_REASON, reason).increment();
    }

    public void recordRateLimitHit(String endpoint, String tenantId) {
      meterRegistry.counter(GatewayConstants.Metrics.RATE_LIMIT_HIT,
          GatewayConstants.Metrics.TAG_ENDPOINT, endpoint,
          GatewayConstants.Metrics.TAG_TENANT, tenantId != null ? tenantId : GatewayConstants.Metrics.TENANT_UNKNOWN).increment();
    }

    public void recordCircuitBreakerOpen(String service) {
      meterRegistry.counter(GatewayConstants.Metrics.CIRCUIT_BREAKER_OPEN,
          GatewayConstants.Metrics.TAG_SERVICE, service).increment();
    }

    public void recordCircuitBreakerClosed(String service) {
      meterRegistry.counter(GatewayConstants.Metrics.CIRCUIT_BREAKER_CLOSED,
          GatewayConstants.Metrics.TAG_SERVICE, service).increment();
    }

    public void recordRouteLatency(String route, long latencyMs) {
      meterRegistry.timer(GatewayConstants.Metrics.ROUTE_LATENCY,
              GatewayConstants.Metrics.TAG_ROUTE, route)
          .record(Duration.ofMillis(latencyMs));
    }

    public void recordActiveConnections(int count) {
      meterRegistry.gauge(GatewayConstants.Metrics.CONNECTIONS_ACTIVE, count);
    }

    public void recordTotalRequests(String method, String route) {
      meterRegistry.counter(GatewayConstants.Metrics.REQUESTS_TOTAL,
          GatewayConstants.Metrics.TAG_METHOD, method,
          GatewayConstants.Metrics.TAG_ROUTE, route).increment();
    }

    public void recordResponseStatus(int statusCode, String route) {
      var statusClass = getStatusClass(statusCode);
      meterRegistry.counter(GatewayConstants.Metrics.RESPONSES_TOTAL,
          GatewayConstants.Metrics.TAG_STATUS_CODE, String.valueOf(statusCode),
          GatewayConstants.Metrics.TAG_STATUS_CLASS, statusClass,
          GatewayConstants.Metrics.TAG_ROUTE, route).increment();
    }

    public void recordTenantRequests(String tenantId, String endpoint) {
      meterRegistry.counter(GatewayConstants.Metrics.TENANT_REQUESTS,
          GatewayConstants.Metrics.TAG_TENANT, tenantId != null ? tenantId : GatewayConstants.Metrics.TENANT_UNKNOWN,
          GatewayConstants.Metrics.TAG_ENDPOINT, endpoint).increment();
    }

    public void recordCorsRequests(String origin, String method) {
      meterRegistry.counter(GatewayConstants.Metrics.CORS_REQUESTS,
          GatewayConstants.Metrics.TAG_ORIGIN, origin != null ? origin : GatewayConstants.Metrics.ORIGIN_UNKNOWN,
          GatewayConstants.Metrics.TAG_METHOD, method).increment();
    }

    public void recordTransformationTime(String type, long durationMs) {
      meterRegistry.timer(GatewayConstants.Metrics.TRANSFORMATION_DURATION,
              GatewayConstants.Metrics.TAG_TYPE, type)
          .record(Duration.ofMillis(durationMs));
    }

    public void recordLoadBalancingDecision(String service, String instance) {
      meterRegistry.counter(GatewayConstants.Metrics.LOAD_BALANCING_DECISIONS,
          GatewayConstants.Metrics.TAG_SERVICE, service,
          GatewayConstants.Metrics.TAG_INSTANCE, instance).increment();
    }

    public void recordHealthCheckResult(String service, boolean healthy) {
      meterRegistry.counter(GatewayConstants.Metrics.HEALTH_CHECK_RESULTS,
          GatewayConstants.Metrics.TAG_SERVICE, service,
          GatewayConstants.Metrics.TAG_RESULT, healthy ? GatewayConstants.Metrics.RESULT_HEALTHY : GatewayConstants.Metrics.RESULT_UNHEALTHY).increment();
    }

    private String getStatusClass(int statusCode) {
      if (statusCode >= 200 && statusCode < 300) {
        return "2xx";
      }
      if (statusCode >= 300 && statusCode < 400) {
        return "3xx";
      }
      if (statusCode >= 400 && statusCode < 500) {
        return "4xx";
      }
      if (statusCode >= 500) {
        return "5xx";
      }
      return "1xx";
    }
  }
}
