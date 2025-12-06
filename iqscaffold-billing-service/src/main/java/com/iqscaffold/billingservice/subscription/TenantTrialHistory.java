package com.iqscaffold.billingservice.subscription;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Value object representing a tenant's trial history.
 * 
 * <p>Encapsulates information about whether a tenant has previously used a trial period.
 * This immutable value object is used by specifications and domain services to
 * determine trial eligibility.
 * 
 * <p>Business rules for trial eligibility:
 * <ul>
 *   <li>Each tenant is eligible for one trial period per lifetime</li>
 *   <li>Once a trial has been used, the tenant cannot start another trial</li>
 *   <li>Canceled trials still count as "used"</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * TenantTrialHistory history = new TenantTrialHistory(
 *   tenantId,
 *   false,  // has never used trial
 *   null,   // no previous trial date
 *   null    // no previous plan code
 * );
 * 
 * TrialEligibilitySpecification spec = new TrialEligibilitySpecification();
 * boolean eligible = spec.isSatisfiedBy(history); // true
 * }</pre>
 * 
 * @param tenantId the tenant identifier
 * @param hasUsedTrial whether the tenant has previously used a trial
 * @param lastTrialDate the date of the last trial (null if never used)
 * @param lastTrialPlanCode the plan code of the last trial (null if never used)
 */
public record TenantTrialHistory(
    UUID tenantId,
    boolean hasUsedTrial,
    LocalDateTime lastTrialDate,
    String lastTrialPlanCode
) {

  /**
   * Creates a new tenant trial history with validation.
   * 
   * @param tenantId the tenant identifier
   * @param hasUsedTrial whether the tenant has previously used a trial
   * @param lastTrialDate the date of the last trial (null if never used)
   * @param lastTrialPlanCode the plan code of the last trial (null if never used)
   * @throws IllegalArgumentException if tenantId is null or if hasUsedTrial is true but lastTrialDate is null
   */
  public TenantTrialHistory {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
    if (hasUsedTrial && lastTrialDate == null) {
      throw new IllegalArgumentException("Last trial date must be provided if trial has been used");
    }
  }

  /**
   * Factory method to create a trial history for a tenant who has never used a trial.
   * 
   * @param tenantId the tenant identifier
   * @return a new TenantTrialHistory indicating no previous trial usage
   */
  public static TenantTrialHistory noTrialUsed(final UUID tenantId) {
    return new TenantTrialHistory(tenantId, false, null, null);
  }

  /**
   * Factory method to create a trial history for a tenant who has used a trial.
   * 
   * @param tenantId the tenant identifier
   * @param lastTrialDate the date of the last trial
   * @param lastTrialPlanCode the plan code of the last trial
   * @return a new TenantTrialHistory indicating previous trial usage
   */
  public static TenantTrialHistory trialUsed(
      final UUID tenantId,
      final LocalDateTime lastTrialDate,
      final String lastTrialPlanCode) {
    return new TenantTrialHistory(tenantId, true, lastTrialDate, lastTrialPlanCode);
  }

  /**
   * Checks if the tenant is eligible for a trial.
   * 
   * @return true if the tenant has never used a trial
   */
  public boolean isEligibleForTrial() {
    return !hasUsedTrial;
  }

  /**
   * Gets the number of days since the last trial ended.
   * 
   * @return days since last trial, or null if no trial has been used
   */
  public Long daysSinceLastTrial() {
    if (lastTrialDate == null) {
      return null;
    }
    return java.time.Duration.between(lastTrialDate, LocalDateTime.now()).toDays();
  }
}
