package com.iqscaffold.gatewayservice.service;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for integrating with the Billing Service to check feature access and quotas.
 */
@Service
public class BillingServiceClient {

  private static final Logger logger = LoggerFactory.getLogger(BillingServiceClient.class);

  private final WebClient webClient;
  private final IqScaffoldProperties properties;

  public BillingServiceClient(final WebClient.Builder webClientBuilder, final IqScaffoldProperties properties) {
    this.properties = properties;
    
    var billingServiceUrl = properties.gateway().integration() != null 
        ? properties.gateway().integration().billingServiceUrl() 
        : "http://localhost:8082";
    
    var timeout = properties.gateway().integration() != null 
        ? properties.gateway().integration().billingServiceTimeout() 
        : Duration.ofSeconds(5);
    
    this.webClient = webClientBuilder
        .baseUrl(billingServiceUrl)
        .defaultHeader("Content-Type", "application/json")
        .build();
    
    logger.info("BillingServiceClient initialized with URL: {}, timeout: {}", billingServiceUrl, timeout);
  }

  /**
   * Check if a tenant has access to a specific feature based on their subscription plan.
   *
   * @param tenantId the tenant ID
   * @param featureCode the feature code to check (e.g., "CRM.BULK_IMPORT", "EMAIL.CUSTOM_TEMPLATES")
   * @return Mono of FeatureCheckResponse indicating if the feature is available
   */
  public Mono<FeatureCheckResponse> checkFeatureAccess(String tenantId, String featureCode) {
    if (!isEnabled()) {
      logger.debug("Billing service integration disabled, allowing all features");
      return Mono.just(new FeatureCheckResponse(true, featureCode, "UNKNOWN", "Billing integration disabled"));
    }

    var request = new FeatureCheckRequest(featureCode);
    var timeout = properties.gateway().integration() != null 
        ? properties.gateway().integration().billingServiceTimeout() 
        : Duration.ofSeconds(5);

    logger.debug("Checking feature access for tenant: {}, feature: {}", tenantId, featureCode);

    return webClient.post()
        .uri("/api/v1/billing/subscriptions/{tenantId}/check-feature", tenantId)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(FeatureCheckResponse.class)
        .timeout(timeout)
        .doOnSuccess(response -> logger.debug("Feature check result for tenant {}, feature {}: available={}", 
            tenantId, featureCode, response.available()))
        .doOnError(error -> logger.error("Error checking feature access for tenant: {}, feature: {}", 
            tenantId, featureCode, error))
        .onErrorResume(error -> {
          // On error, fail open (allow access) to prevent blocking legitimate requests
          logger.warn("Billing service unavailable, allowing feature access for tenant: {}, feature: {}", 
              tenantId, featureCode);
          return Mono.just(new FeatureCheckResponse(true, featureCode, "UNKNOWN", 
              "Billing service unavailable - access granted"));
        });
  }

  private boolean isEnabled() {
    return properties.gateway().integration() != null 
        && properties.gateway().integration().billingServiceEnabled();
  }
}
