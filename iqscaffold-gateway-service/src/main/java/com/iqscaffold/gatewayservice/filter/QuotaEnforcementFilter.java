package com.iqscaffold.gatewayservice.filter;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import com.iqscaffold.gatewayservice.exception.QuotaExceededException;
import com.iqscaffold.gatewayservice.service.BillingServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Quota enforcement filter for API call quotas.
 *
 * <p>This filter enforces API call quotas based on subscription plans by:
 * <ul>
 *   <li>Checking quota availability with billing service before processing request</li>
 *   <li>Recording API call usage asynchronously after successful requests</li>
 *   <li>Rejecting requests with HTTP 429 when quota is exceeded</li>
 *   <li>Gracefully degrading when billing service is unavailable</li>
 * </ul>
 *
 * <p>The filter runs after authentication (order -40) but before routing to ensure
 * tenant context is available and quota is enforced before downstream services
 * process the request.
 *
 * <p>Configuration:
 * <pre>
 * iqscaffold:
 *   gateway:
 *     integration:
 *       billing-service-enabled: true
 *       billing-service-url: http://localhost:8082
 *       billing-service-timeout: PT5S
 *     quota-enforcement:
 *       enabled: true
 *       excluded-paths:
 *         - /api/v1/auth/**
 *         - /actuator/**
 * </pre>
 */
@Component
public class QuotaEnforcementFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(QuotaEnforcementFilter.class);

  private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";

  private final IqScaffoldProperties properties;
  private final BillingServiceClient billingServiceClient;

  public QuotaEnforcementFilter(final IqScaffoldProperties properties,
                                final BillingServiceClient billingServiceClient) {
    this.properties = properties;
    this.billingServiceClient = billingServiceClient;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // Skip if billing service integration is disabled
    if (!properties.gateway().integration().billingServiceEnabled()) {
      return chain.filter(exchange);
    }

    var request = exchange.getRequest();
    var path = request.getPath().value();

    // Skip quota enforcement for excluded paths
    if (isExcludedPath(path)) {
      logger.debug("Skipping quota enforcement for excluded path: {}", path);
      return chain.filter(exchange);
    }

    // Get tenant context from exchange attributes
    var tenantContext = (TenantExtractionFilter.TenantContext)
        exchange.getAttributes().get(TENANT_CONTEXT_ATTRIBUTE);

    if (tenantContext == null || tenantContext.tenantId() == null) {
      logger.debug("No tenant context found, skipping quota enforcement for path: {}", path);
      return chain.filter(exchange);
    }

    var tenantId = tenantContext.tenantId();

    logger.debug("Checking API call quota for tenant: {}, path: {}", tenantId, path);

    // Check quota before processing request
    return billingServiceClient.checkQuota(tenantId, 1L)
        .flatMap(quotaResponse -> {
          if (!quotaResponse.allowed()) {
            logger.warn("Quota exceeded for tenant: {}, metric: {}, usage: {}/{}, resets: {}",
                tenantId, quotaResponse.metricType(), quotaResponse.currentUsage(),
                quotaResponse.limit(), quotaResponse.resetsAt());

            return Mono.error(new QuotaExceededException(
                tenantId,
                quotaResponse.metricType(),
                quotaResponse.currentUsage(),
                quotaResponse.limit(),
                quotaResponse.resetsAt()
            ));
          }

          logger.debug("Quota check passed for tenant: {}, remaining: {}/{}",
              tenantId, quotaResponse.remainingQuota(), quotaResponse.limit());

          // Process request and record usage asynchronously
          return chain.filter(exchange)
              .then(recordUsageAsync(tenantId));
        })
        .onErrorResume(error -> {
          // Propagate QuotaExceededException
          if (error instanceof QuotaExceededException) {
            return Mono.error(error);
          }

          // Log other errors but allow request to proceed (graceful degradation)
          logger.error("Error in quota enforcement for tenant: {}, path: {}",
              tenantId, path, error);
          return chain.filter(exchange);
        });
  }

  /**
   * Records API call usage asynchronously.
   *
   * <p>This method is called after the request is processed successfully.
   * It runs asynchronously and does not block the response.
   */
  private Mono<Void> recordUsageAsync(String tenantId) {
    return billingServiceClient.recordUsage(tenantId, 1L)
        .doOnSuccess(v -> logger.debug("Recorded API call usage for tenant: {}", tenantId))
        .doOnError(error -> logger.warn("Failed to record usage for tenant: {}", tenantId, error))
        .onErrorResume(error -> Mono.empty()); // Don't fail response if usage recording fails
  }

  /**
   * Checks if the path should be excluded from quota enforcement.
   *
   * <p>Excluded paths:
   * <ul>
   *   <li>Authentication endpoints (/api/v1/auth/**)</li>
   *   <li>Actuator endpoints (/actuator/**)</li>
   *   <li>API documentation (/swagger-ui/**, /api-docs/**)</li>
   *   <li>JWK endpoint (/.well-known/jwks.json)</li>
   * </ul>
   */
  private boolean isExcludedPath(String path) {
    return path.startsWith("/api/v1/auth/")
           || path.startsWith("/actuator/")
           || path.startsWith("/swagger-ui/")
           || path.startsWith("/api-docs/")
           || path.equals("/.well-known/jwks.json")
           || path.contains("/swagger-ui/")
           || path.contains("/api-docs/");
  }

  @Override
  public int getOrder() {
    return -40; // Execute after authentication (-50) but before routing
  }
}
