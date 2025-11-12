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
@EnableConfigurationProperties(GripdayGatewayObservabilityProperties.class)
@AutoConfiguration(before = ObservationAutoConfiguration.class)
public class ObservabilityConfig {

  private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

  private final GripdayGatewayObservabilityProperties observabilityProperties;
  private final Environment environment;

  public ObservabilityConfig(final GripdayGatewayObservabilityProperties observabilityProperties, final Environment environment) {
    this.observabilityProperties = observabilityProperties;
    this.environment = environment;
  }

  /**
   * Configures OpenTelemetry SDK with environment-specific settings for reactive applications.
   */
  @Bean
  @ConditionalOnProperty(name = "gripday.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
  public OpenTelemetry openTelemetry() {
    var tracingProps = observabilityProperties.tracing();

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

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      var request = exchange.getRequest();
      var response = exchange.getResponse();

      // Generate or extract correlation ID
      var correlationId = getOrGenerateCorrelationId(request);
      var requestId = getOrGenerateRequestId(request);
      var tenantId = request.getHeaders().getFirst(TENANT_ID_HEADER);

      // Add to response headers
      response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
      response.getHeaders().add(REQUEST_ID_HEADER, requestId);

      // Add to MDC for logging (reactive context)
      return chain.filter(exchange)
          .contextWrite(ctx -> {
            ctx = ctx.put("correlationId", correlationId);
            ctx = ctx.put("requestId", requestId);
            if (tenantId != null) {
              ctx = ctx.put("tenantId", tenantId);
            }
            return ctx;
          })
          .doOnEach(signal -> {
            if (signal.hasValue() || signal.hasError()) {
              // Set MDC for logging
              MDC.put("correlationId", correlationId);
              MDC.put("requestId", requestId);
              if (tenantId != null) {
                MDC.put("tenantId", tenantId);
              }
            }
          })
          .doFinally(signalType -> MDC.clear());
    }

    private String getOrGenerateCorrelationId(org.springframework.http.server.reactive.ServerHttpRequest request) {
      var correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
      return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private String getOrGenerateRequestId(org.springframework.http.server.reactive.ServerHttpRequest request) {
      var requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
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
      this.requestTimer = Timer.builder("gripday.gateway.request.duration")
          .description("Time taken for gateway request processing")
          .register(meterRegistry);
      this.authenticationTimer = Timer.builder("gripday.gateway.authentication.duration")
          .description("Time taken for authentication validation")
          .register(meterRegistry);
      this.rateLimitTimer = Timer.builder("gripday.gateway.ratelimit.duration")
          .description("Time taken for rate limit checking")
          .register(meterRegistry);
    }

    public Timer.Sample startRequestTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordRequestSuccess(Timer.Sample sample, String route) {
      sample.stop(Timer.builder("gripday.gateway.request.success")
          .tag("route", route)
          .description("Successful gateway requests")
          .register(meterRegistry));
      meterRegistry.counter("gripday.gateway.request.total", "result", "success", "route", route).increment();
    }

    public void recordRequestFailure(String route, String reason, int statusCode) {
      meterRegistry.counter("gripday.gateway.request.total",
          "result", "failure",
          "route", route,
          "reason", reason,
          "status", String.valueOf(statusCode)).increment();
    }

    public Timer.Sample startAuthenticationTimer() {
      return Timer.start(meterRegistry);
    }

    public void recordAuthenticationSuccess(Timer.Sample sample) {
      sample.stop(authenticationTimer);
      meterRegistry.counter("gripday.gateway.authentication.total", "result", "success").increment();
    }

    public void recordAuthenticationFailure(String reason) {
      meterRegistry.counter("gripday.gateway.authentication.total", "result", "failure", "reason", reason).increment();
    }

    public void recordRateLimitHit(String endpoint, String tenantId) {
      meterRegistry.counter("gripday.gateway.ratelimit.hit",
          "endpoint", endpoint,
          "tenant", tenantId != null ? tenantId : "unknown").increment();
    }

    public void recordCircuitBreakerOpen(String service) {
      meterRegistry.counter("gripday.gateway.circuitbreaker.open", "service", service).increment();
    }

    public void recordCircuitBreakerClosed(String service) {
      meterRegistry.counter("gripday.gateway.circuitbreaker.closed", "service", service).increment();
    }

    public void recordRouteLatency(String route, long latencyMs) {
      meterRegistry.timer("gripday.gateway.route.latency", "route", route)
          .record(Duration.ofMillis(latencyMs));
    }

    public void recordActiveConnections(int count) {
      meterRegistry.gauge("gripday.gateway.connections.active", count);
    }

    public void recordTotalRequests(String method, String route) {
      meterRegistry.counter("gripday.gateway.requests.total", "method", method, "route", route).increment();
    }

    public void recordResponseStatus(int statusCode, String route) {
      var statusClass = getStatusClass(statusCode);
      meterRegistry.counter("gripday.gateway.responses.total",
          "status_code", String.valueOf(statusCode),
          "status_class", statusClass,
          "route", route).increment();
    }

    public void recordTenantRequests(String tenantId, String endpoint) {
      meterRegistry.counter("gripday.gateway.tenant.requests",
          "tenant", tenantId != null ? tenantId : "unknown",
          "endpoint", endpoint).increment();
    }

    public void recordCorsRequests(String origin, String method) {
      meterRegistry.counter("gripday.gateway.cors.requests",
          "origin", origin != null ? origin : "unknown",
          "method", method).increment();
    }

    public void recordTransformationTime(String type, long durationMs) {
      meterRegistry.timer("gripday.gateway.transformation.duration", "type", type)
          .record(Duration.ofMillis(durationMs));
    }

    public void recordLoadBalancingDecision(String service, String instance) {
      meterRegistry.counter("gripday.gateway.loadbalancing.decisions",
          "service", service,
          "instance", instance).increment();
    }

    public void recordHealthCheckResult(String service, boolean healthy) {
      meterRegistry.counter("gripday.gateway.healthcheck.results",
          "service", service,
          "result", healthy ? "healthy" : "unhealthy").increment();
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