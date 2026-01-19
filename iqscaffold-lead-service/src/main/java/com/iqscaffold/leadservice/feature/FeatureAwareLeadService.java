package com.iqscaffold.leadservice.feature;

import com.iqscaffold.leadservice.lead.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Feature-aware lead service that enforces subscription-based limits and features.
 * Demonstrates how to integrate feature context into business logic.
 */
@Service
public class FeatureAwareLeadService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureAwareLeadService.class);

  private final LeadRepository leadRepository;

  public FeatureAwareLeadService(final LeadRepository leadRepository) {
    this.leadRepository = leadRepository;
  }

  /**
   * Validates if lead creation is allowed based on subscription limits.
   */
  public void validateLeadCreation() {
    FeatureContext featureContext = FeatureContextHolder.getContext();

    // Check if lead management feature is enabled
    if (!featureContext.isFeatureEnabled("lead_management")) {
      throw new FeatureNotAvailableException("Lead management is not available in your current plan");
    }

    // Check lead quota limits
    Integer maxLeads = featureContext.getQuota("max_leads");
    if (maxLeads != null) {
      // Count all leads for current tenant (tenant filtering handled by JPA context)
      long currentLeadCount = leadRepository.count();

      if (currentLeadCount >= maxLeads) {
        throw new QuotaExceededException(
            String.format("Lead quota exceeded. Current: %d, Max: %d", currentLeadCount, maxLeads));
      }
    }

    logger.debug("Lead creation validation passed for plan: {}", featureContext.planName());
  }

  /**
   * Checks if advanced lead features are available.
   */
  public boolean isAdvancedLeadFeaturesAvailable() {
    return FeatureContextHolder.isFeatureEnabled("advanced_lead_features");
  }

  /**
   * Checks if lead export is available.
   */
  public boolean isLeadExportAvailable() {
    FeatureContext context = FeatureContextHolder.getContext();
    return context.isFeatureEnabled("data_export") && context.isFeatureEnabled("lead_management");
  }

  /**
   * Gets the maximum number of lead activities allowed.
   */
  public int getMaxLeadActivities() {
    Integer limit = FeatureContextHolder.getLimit("max_lead_activities");
    return limit != null ? limit : 10; // Default limit
  }

  /**
   * Gets the support tier for lead-related support.
   */
  public String getLeadSupportTier() {
    String tier = FeatureContextHolder.getTier("support_level");
    return tier != null ? tier : "basic";
  }

  /**
   * Exception thrown when a feature is not available in the current plan.
   */
  public static class FeatureNotAvailableException extends RuntimeException {
    public FeatureNotAvailableException(final String message) {
      super(message);
    }
  }

  /**
   * Exception thrown when a quota limit is exceeded.
   */
  public static class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(final String message) {
      super(message);
    }
  }
}
