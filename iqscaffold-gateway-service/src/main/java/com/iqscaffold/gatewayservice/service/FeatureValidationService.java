package com.iqscaffold.gatewayservice.service;

import java.time.Duration;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Service for validating feature access at the gateway level.
 *
 * <p>This service communicates with the billing service to check feature enablement
 * and builds feature contexts for downstream services. It includes caching and
 * resilience patterns for high performance and reliability.
 */
@Service
public class FeatureValidationService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureValidationService.class);

  private final WebClient billingServiceClient;
  private final FeatureUsageTrackingService usageTrackingService;

  public FeatureValidationService(
      final WebClient.Builder webClientBuilder,
      final FeatureUsageTrackingService usageTrackingService) {
    this.billingServiceClient = webClientBuilder
        .baseUrl("http://billing-service")
        .build();
    this.usageTrackingService = usageTrackingService;
  }

  /**
   * Validates that a tenant has access to all required features.
   *
   * @param tenantId         the tenant identifier
   * @param requiredFeatures set of features required for the operation
   * @param endpoint         the endpoint being accessed (for usage tracking)
   * @return validation result with access decision and feature context
   */
  @Cacheable(value = "featureValidation", key = "#tenantId + ':' + #requiredFeatures.hashCode()")
  public Mono<ValidationResult> validateFeatureAccess(String tenantId, Set<String> requiredFeatures, String endpoint) {
    logger.debug("Validating feature access for tenant {} with features {}", tenantId, requiredFeatures);

    return getFeatureContext(tenantId)
        .map(featureContext -> {
          // Check if all required features are available
          Set<String> missingFeatures = findMissingFeatures(requiredFeatures, featureContext);
          boolean isAllowed = missingFeatures.isEmpty();

          if (isAllowed) {
            // Track feature usage asynchronously
            requiredFeatures.forEach(feature ->
                usageTrackingService.recordFeatureUsage(tenantId, feature, endpoint));
          }

          return new ValidationResult(isAllowed, featureContext, missingFeatures);
        })
        .doOnError(error -> logger.error("Error validating feature access for tenant {}: {}",
            tenantId, error.getMessage(), error))
        .onErrorReturn(new ValidationResult(false, FeatureContext.empty(tenantId), requiredFeatures));
  }

  /**
   * Gets the complete feature context for a tenant from the billing service.
   */
  private Mono<FeatureContext> getFeatureContext(String tenantId) {
    return billingServiceClient
        .get()
        .uri("/api/v1/internal/features/context/{tenantId}", tenantId)
        .retrieve()
        .bodyToMono(FeatureContext.class)
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
            .maxBackoff(Duration.ofSeconds(2)))
        .timeout(Duration.ofSeconds(5))
        .doOnSuccess(context -> logger.debug("Retrieved feature context for tenant {}: {} features",
            tenantId, context.getEnabledFeatureCount()))
        .onErrorResume(error -> {
          logger.warn("Failed to retrieve feature context for tenant {}: {}", tenantId, error.getMessage());
          return Mono.just(FeatureContext.empty(tenantId));
        });
  }

  /**
   * Finds features that are required but not available in the tenant's context.
   */
  private Set<String> findMissingFeatures(Set<String> requiredFeatures, FeatureContext featureContext) {
    return requiredFeatures.stream()
        .filter(feature -> !isFeatureAvailable(feature, featureContext))
        .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Checks if a specific feature is available in the feature context.
   */
  private boolean isFeatureAvailable(String featureKey, FeatureContext featureContext) {
    // Check boolean features
    if (featureContext.isFeatureEnabled(featureKey)) {
      return true;
    }

    // Check quota features (available if quota > 0)
    Long quota = featureContext.getQuota(featureKey);
    if (quota != null && quota > 0) {
      return true;
    }

    // Check limit features (available if limit > 0)
    Long limit = featureContext.getLimit(featureKey);
    if (limit != null && limit > 0) {
      return true;
    }

    // Check tier features (available if tier is set)
    String tier = featureContext.getTier(featureKey);
    return tier != null && !tier.isEmpty();
  }

  /**
   * Result of feature validation containing access decision and context.
   */
  public static class ValidationResult {
    private final boolean allowed;
    private final FeatureContext featureContext;
    private final Set<String> missingFeatures;

    public ValidationResult(final boolean allowed, final FeatureContext featureContext, final Set<String> missingFeatures) {
      this.allowed = allowed;
      this.featureContext = featureContext;
      this.missingFeatures = missingFeatures;
    }

    public boolean isAllowed() {
      return allowed;
    }

    public FeatureContext getFeatureContext() {
      return featureContext;
    }

    public Set<String> getMissingFeatures() {
      return missingFeatures;
    }

    @Override
    public String toString() {
      return "ValidationResult{" +
             "allowed=" + allowed +
             ", missingFeatures=" + missingFeatures +
             ", planName=" + (featureContext != null ? featureContext.getPlanName() : "unknown") +
             '}';
    }
  }

  /**
   * Feature context DTO for gateway use.
   * This is a simplified version of the billing service's FeatureContext.
   */
  public static class FeatureContext {
    private String tenantId;
    private String planId;
    private String planName;
    private Set<String> enabledFeatures = Set.of();
    private java.util.Map<String, Long> quotas = java.util.Map.of();
    private java.util.Map<String, Long> limits = java.util.Map.of();
    private java.util.Map<String, String> tiers = java.util.Map.of();

    // Default constructor for JSON deserialization
    public FeatureContext() {
    }

    public FeatureContext(final String tenantId) {
      this.tenantId = tenantId;
    }

    public static FeatureContext empty(final String tenantId) {
      return new FeatureContext(tenantId);
    }

    public boolean isFeatureEnabled(String featureKey) {
      return enabledFeatures.contains(featureKey);
    }

    public Long getQuota(String featureKey) {
      return quotas.get(featureKey);
    }

    public Long getLimit(String featureKey) {
      return limits.get(featureKey);
    }

    public String getTier(String featureKey) {
      return tiers.get(featureKey);
    }

    public int getEnabledFeatureCount() {
      return enabledFeatures.size() + quotas.size() + limits.size() + tiers.size();
    }

    // Getters and setters for JSON serialization
    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getPlanId() {
      return planId;
    }

    public void setPlanId(String planId) {
      this.planId = planId;
    }

    public String getPlanName() {
      return planName;
    }

    public void setPlanName(String planName) {
      this.planName = planName;
    }

    public Set<String> getEnabledFeatures() {
      return enabledFeatures;
    }

    public void setEnabledFeatures(Set<String> enabledFeatures) {
      this.enabledFeatures = enabledFeatures != null ? enabledFeatures : Set.of();
    }

    public java.util.Map<String, Long> getQuotas() {
      return quotas;
    }

    public void setQuotas(java.util.Map<String, Long> quotas) {
      this.quotas = quotas != null ? quotas : java.util.Map.of();
    }

    public java.util.Map<String, Long> getLimits() {
      return limits;
    }

    public void setLimits(java.util.Map<String, Long> limits) {
      this.limits = limits != null ? limits : java.util.Map.of();
    }

    public java.util.Map<String, String> getTiers() {
      return tiers;
    }

    public void setTiers(java.util.Map<String, String> tiers) {
      this.tiers = tiers != null ? tiers : java.util.Map.of();
    }
  }
}
