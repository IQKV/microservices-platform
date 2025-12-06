package com.iqscaffold.billingservice.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Immutable value object representing quota limits for a subscription plan.
 * Defines resource limits for various metrics like users, storage, API calls, etc.
 * 
 * <p>This record is stored as JSONB in the database and provides type-safe access
 * to quota values. All quotas are optional and null values indicate unlimited access.
 */
public record PlanQuotas(
    @JsonProperty("maxUsers") Long maxUsers,
    @JsonProperty("storageGb") Long storageGb,
    @JsonProperty("apiCallsPerMonth") Long apiCallsPerMonth,
    @JsonProperty("emailSendsPerMonth") Long emailSendsPerMonth,
    @JsonProperty("campaignExecutionsPerMonth") Long campaignExecutionsPerMonth,
    @JsonProperty("scoringRequestsPerMonth") Long scoringRequestsPerMonth,
    @JsonProperty("customDomains") Long customDomains,
    @JsonProperty("dataExportsPerMonth") Long dataExportsPerMonth
) implements Serializable {

  /**
   * Creates a PlanQuotas instance with all quotas set to unlimited (null).
   * 
   * @return a PlanQuotas with no limits
   */
  public static PlanQuotas unlimited() {
    return new PlanQuotas(null, null, null, null, null, null, null, null);
  }

  /**
   * Creates a PlanQuotas instance for a free tier plan with basic limits.
   * 
   * @return a PlanQuotas with free tier limits
   */
  public static PlanQuotas freeTier() {
    return new PlanQuotas(
        5L,        // maxUsers
        1L,        // storageGb
        1000L,     // apiCallsPerMonth
        100L,      // emailSendsPerMonth
        10L,       // campaignExecutionsPerMonth
        50L,       // scoringRequestsPerMonth
        0L,        // customDomains
        5L         // dataExportsPerMonth
    );
  }

  /**
   * Creates a PlanQuotas instance for a pro tier plan with enhanced limits.
   * 
   * @return a PlanQuotas with pro tier limits
   */
  public static PlanQuotas proTier() {
    return new PlanQuotas(
        50L,       // maxUsers
        50L,       // storageGb
        100000L,   // apiCallsPerMonth
        10000L,    // emailSendsPerMonth
        1000L,     // campaignExecutionsPerMonth
        5000L,     // scoringRequestsPerMonth
        5L,        // customDomains
        100L       // dataExportsPerMonth
    );
  }

  /**
   * Checks if a specific quota has a limit defined.
   * 
   * @param quotaValue the quota value to check
   * @return true if the quota has a limit (not null), false if unlimited
   */
  public static boolean hasLimit(Long quotaValue) {
    return quotaValue != null;
  }

  /**
   * Checks if the max users quota is unlimited.
   * 
   * @return true if unlimited, false if limited
   */
  public boolean isMaxUsersUnlimited() {
    return maxUsers == null;
  }

  /**
   * Checks if the storage quota is unlimited.
   * 
   * @return true if unlimited, false if limited
   */
  public boolean isStorageUnlimited() {
    return storageGb == null;
  }

  /**
   * Checks if the API calls quota is unlimited.
   * 
   * @return true if unlimited, false if limited
   */
  public boolean isApiCallsUnlimited() {
    return apiCallsPerMonth == null;
  }
}
