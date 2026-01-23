package com.iqscaffold.gatewayservice.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Service for tracking feature usage at the gateway level.
 *
 * <p>This service asynchronously reports feature usage to the billing service
 * for analytics and billing purposes. It includes resilience patterns to ensure
 * that usage tracking failures don't impact request processing.
 */
@Service
public class FeatureUsageTrackingService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureUsageTrackingService.class);

  private final WebClient billingServiceClient;

  public FeatureUsageTrackingService(final WebClient.Builder webClientBuilder) {
    this.billingServiceClient = webClientBuilder
        .baseUrl("http://billing-service")
        .build();
  }

  /**
   * Records feature usage asynchronously.
   *
   * @param tenantId   the tenant identifier
   * @param featureKey the feature that was used
   * @param endpoint   the endpoint where the feature was used
   */
  @Async
  public void recordFeatureUsage(String tenantId, String featureKey, String endpoint) {
    try {
      var usageRequest = new FeatureUsageRequest(tenantId, featureKey, endpoint,
          MDC.get("userId"), MDC.get("correlationId"));

      billingServiceClient
          .post()
          .uri("/api/v1/internal/features/usage")
          .bodyValue(usageRequest)
          .retrieve()
          .bodyToMono(Void.class)
          .timeout(Duration.ofSeconds(5))
          .subscribe(
              result -> logger.debug("Recorded feature usage: tenant={}, feature={}, endpoint={}",
                  tenantId, featureKey, endpoint),
              error -> logger.warn("Failed to record feature usage for tenant {} and feature {}: {}",
                  tenantId, featureKey, error.getMessage())
          );

    } catch (final Exception e) {
      logger.error("Error recording feature usage for tenant {} and feature {}: {}",
          tenantId, featureKey, e.getMessage(), e);
      // Don't rethrow - usage tracking should not fail the main request
    }
  }

  /**
   * Records multiple feature usage events in batch.
   */
  @Async
  public void recordBatchFeatureUsage(String tenantId, java.util.Set<String> featureKeys, String endpoint) {
    featureKeys.forEach(featureKey -> recordFeatureUsage(tenantId, featureKey, endpoint));
  }

  /**
   * Request DTO for feature usage tracking.
   */
  public static class FeatureUsageRequest {
    private String tenantId;
    private String featureKey;
    private String endpoint;
    private String userId;
    private String correlationId;

    public FeatureUsageRequest() {
    }

    public FeatureUsageRequest(final String tenantId, final String featureKey, final String endpoint, final String userId, final String correlationId) {
      this.tenantId = tenantId;
      this.featureKey = featureKey;
      this.endpoint = endpoint;
      this.userId = userId;
      this.correlationId = correlationId;
    }

    // Getters and setters
    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getFeatureKey() {
      return featureKey;
    }

    public void setFeatureKey(String featureKey) {
      this.featureKey = featureKey;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getCorrelationId() {
      return correlationId;
    }

    public void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
    }
  }
}
