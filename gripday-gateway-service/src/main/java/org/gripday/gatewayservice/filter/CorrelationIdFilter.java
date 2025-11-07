package org.gripday.gatewayservice.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Correlation ID filter for distributed request tracing. Generates or propagates correlation IDs for request tracking across microservices.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

  private static final String X_CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();

    // Get or generate correlation ID
    var correlationId = getOrGenerateCorrelationId(request);

    // Add correlation ID to MDC for structured logging
    MDC.put("correlationId", correlationId);

    // Store correlation ID in exchange attributes
    exchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);

    // Add correlation ID to request headers for downstream services
    var modifiedRequest = request.mutate()
        .header(X_CORRELATION_ID_HEADER, correlationId)
        .build();

    var modifiedExchange = exchange.mutate().request(modifiedRequest).build();

    logger.debug("Processing request with correlation ID: {}", correlationId);

    return chain.filter(modifiedExchange)
        .doFinally(signalType -> {
          // Clean up MDC after request processing
          MDC.remove("correlationId");
        });
  }

  private String getOrGenerateCorrelationId(org.springframework.http.server.reactive.ServerHttpRequest request) {
    // Check if correlation ID already exists in headers
    var existingCorrelationId = request.getHeaders().getFirst(X_CORRELATION_ID_HEADER);

    if (StringUtils.hasText(existingCorrelationId)) {
      logger.debug("Using existing correlation ID: {}", existingCorrelationId);
      return existingCorrelationId;
    }

    // Generate new correlation ID
    var newCorrelationId = generateCorrelationId();
    logger.debug("Generated new correlation ID: {}", newCorrelationId);
    return newCorrelationId;
  }

  private String generateCorrelationId() {
    // Generate UUID-based correlation ID with timestamp prefix for better traceability
    var timestamp = System.currentTimeMillis();
    var uuid = UUID.randomUUID().toString().replace("-", "");
    return String.format("%d-%s", timestamp, uuid.substring(0, 8));
  }

  @Override
  public int getOrder() {
    return -300; // Execute before tenant extraction and authentication filters
  }
}