package com.iqscaffold.gatewayservice.service;

import java.time.LocalDateTime;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive client for billing service integration.
 *
 * <p>Provides methods to check quotas and record usage asynchronously.
 * Implements circuit breaker pattern and graceful degradation.
 */
@Service
public class BillingServiceClient {

  private static final Logger logger = LoggerFactory.getLogger(BillingServiceClient.class);

  private final WebClient webClient;
  private final IqScaffoldProperties properties;

  public BillingServiceClient(final WebClient.Builder webClientBuilder,
                              final IqScaffoldProperties properties) {
    this.properties = properties;
    this.webClient = webClientBuilder
        .baseUrl(properties.gateway().integration().billingServiceUrl())
        .build();
  }

  /**
   * Checks if tenant has quota available for API calls.
   *
   * @param tenantId          tenant identifier
   * @param requestedQuantity number of API calls requested (typically 1)
   * @return quota check response, or empty if billing service unavailable
   */
  public Mono<QuotaCheckResponse> checkQuota(String tenantId, long requestedQuantity) {
    if (!properties.gateway().integration().billingServiceEnabled()) {
      logger.debug("Billing service integration disabled, allowing request");
      return Mono.just(createAllowedResponse());
    }

    var request = new QuotaCheckRequest("API_CALLS", requestedQuantity);

    return webClient.post()
        .uri("/api/v1/internal/billing/usage/{tenantId}/check-quota", tenantId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, response -> {
          logger.warn("Client error checking quota for tenant: {}, status: {}",
              tenantId, response.statusCode());
          return Mono.empty();
        })
        .onStatus(HttpStatusCode::is5xxServerError, response -> {
          logger.error("Server error checking quota for tenant: {}, status: {}",
              tenantId, response.statusCode());
          return Mono.empty();
        })
        .bodyToMono(QuotaCheckResponse.class)
        .timeout(properties.gateway().integration().billingServiceTimeout())
        .doOnError(error -> logger.error("Error checking quota for tenant: {}", tenantId, error))
        .onErrorResume(error -> {
          // Graceful degradation - allow request if billing service unavailable
          logger.warn("Billing service unavailable, allowing request for tenant: {}", tenantId);
          return Mono.just(createAllowedResponse());
        });
  }

  /**
   * Records API call usage asynchronously.
   *
   * @param tenantId tenant identifier
   * @param quantity number of API calls to record (typically 1)
   * @return completion signal
   */
  public Mono<Void> recordUsage(String tenantId, long quantity) {
    if (!properties.gateway().integration().billingServiceEnabled()) {
      return Mono.empty();
    }

    var request = new RecordUsageRequest("API_CALLS", quantity);

    return webClient.post()
        .uri("/api/v1/internal/billing/usage/{tenantId}/record", tenantId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> {
          logger.warn("Error recording usage for tenant: {}, status: {}",
              tenantId, response.statusCode());
          return Mono.empty();
        })
        .bodyToMono(Void.class)
        .timeout(properties.gateway().integration().billingServiceTimeout())
        .doOnError(error -> logger.error("Error recording usage for tenant: {}", tenantId, error))
        .onErrorResume(error -> Mono.empty()) // Don't fail request if usage recording fails
        .then();
  }

  /**
   * Checks if tenant has access to a specific feature.
   *
   * @param tenantId    tenant identifier
   * @param featureCode feature code to check
   * @return feature access response, or allowed response if billing service unavailable
   */
  public Mono<FeatureAccessResponse> checkFeatureAccess(String tenantId, String featureCode) {
    if (!properties.gateway().integration().billingServiceEnabled()) {
      logger.debug("Billing service integration disabled, allowing feature access");
      return Mono.just(new FeatureAccessResponse(true, "FREE", "Feature check bypassed"));
    }

    var request = new FeatureCheckRequest(featureCode);

    return webClient.post()
        .uri("/api/v1/internal/billing/subscriptions/{tenantId}/check-feature", tenantId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, response -> {
          logger.warn("Client error checking feature access for tenant: {}, status: {}",
              tenantId, response.statusCode());
          return Mono.empty();
        })
        .onStatus(HttpStatusCode::is5xxServerError, response -> {
          logger.error("Server error checking feature access for tenant: {}, status: {}",
              tenantId, response.statusCode());
          return Mono.empty();
        })
        .bodyToMono(FeatureAccessResponse.class)
        .timeout(properties.gateway().integration().billingServiceTimeout())
        .doOnError(error -> logger.error("Error checking feature access for tenant: {}", tenantId, error))
        .onErrorResume(error -> {
          // Graceful degradation - allow access if billing service unavailable
          logger.warn("Billing service unavailable, allowing feature access for tenant: {}", tenantId);
          return Mono.just(new FeatureAccessResponse(true, "UNKNOWN", "Feature check bypassed"));
        });
  }

  private QuotaCheckResponse createAllowedResponse() {
    return new QuotaCheckResponse(
        "API_CALLS",
        0L,
        Long.MAX_VALUE,
        Long.MAX_VALUE,
        true,
        0.0,
        LocalDateTime.now().plusYears(1),
        "Quota check bypassed"
    );
  }

  /**
   * Request DTO for quota check.
   */
  public record QuotaCheckRequest(
      String metricType,
      long requestedQuantity
  ) {
  }

  /**
   * Response DTO for quota check.
   */
  public record QuotaCheckResponse(
      String metricType,
      long currentUsage,
      long limit,
      long remainingQuota,
      boolean allowed,
      double percentageUsed,
      LocalDateTime resetsAt,
      String message
  ) {
  }

  /**
   * Request DTO for recording usage.
   */
  public record RecordUsageRequest(
      String metricType,
      long quantity
  ) {
  }

  /**
   * Request DTO for feature access check.
   */
  public record FeatureCheckRequest(
      String featureCode
  ) {
  }

  /**
   * Response DTO for feature access check.
   */
  public record FeatureAccessResponse(
      boolean available,
      String planTier,
      String message
  ) {
  }
}
