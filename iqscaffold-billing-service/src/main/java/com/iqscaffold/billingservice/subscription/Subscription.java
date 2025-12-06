package com.iqscaffold.billingservice.subscription;

import com.iqscaffold.billingservice.plan.SubscriptionPlan;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Type;

/**
 * Subscription aggregate root entity representing a tenant's subscription to a plan.
 * 
 * <p>Subscriptions manage the lifecycle of a tenant's access to platform features,
 * including trial periods, billing cycles, cancellations, and status transitions.
 * Subscriptions are stored in tenant-scoped schemas for data isolation.
 * 
 * <p>This aggregate enforces the following invariants:
 * <ul>
 *   <li>Only one active subscription per tenant</li>
 *   <li>Status transitions must follow valid state machine rules</li>
 *   <li>Trial period cannot be negative</li>
 *   <li>Current period end must be after start</li>
 *   <li>Trial end must be after trial start</li>
 *   <li>Canceled subscriptions maintain access until period end</li>
 * </ul>
 * 
 * <p>All modifications to the subscription must go through this aggregate root
 * to ensure business rules and invariants are maintained.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private SubscriptionPlan plan;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SubscriptionStatus status;

  @Column
  private LocalDateTime currentPeriodStart;

  @Column
  private LocalDateTime currentPeriodEnd;

  @Column
  private LocalDateTime trialStart;

  @Column
  private LocalDateTime trialEnd;

  @Column
  private LocalDateTime canceledAt;

  @Column(nullable = false)
  private Boolean cancelAtPeriodEnd;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Default constructor for JPA.
   */
  protected Subscription() {
    // JPA requires a no-arg constructor
  }

  /**
   * Creates a new subscription with the specified attributes.
   * 
   * @param tenantId tenant identifier
   * @param userId user who created the subscription
   * @param plan subscription plan
   * @param status initial status
   */
  private Subscription(UUID tenantId, UUID userId, SubscriptionPlan plan, SubscriptionStatus status) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.plan = plan;
    this.status = status;
    this.cancelAtPeriodEnd = false;
    this.metadata = new HashMap<>();
  }

  /**
   * Factory method to create a new trial subscription.
   * 
   * @param tenantId tenant identifier
   * @param userId user who created the subscription
   * @param plan subscription plan with trial period
   * @return a new Subscription in TRIAL status
   * @throws IllegalArgumentException if plan doesn't offer trial
   */
  public static Subscription createTrial(UUID tenantId, UUID userId, SubscriptionPlan plan) {
    validateTenantId(tenantId);
    validateUserId(userId);
    validatePlan(plan);

    if (!plan.hasTrial()) {
      throw new IllegalArgumentException("Plan does not offer a trial period");
    }

    var subscription = new Subscription(tenantId, userId, plan, SubscriptionStatus.TRIAL);
    var now = LocalDateTime.now();
    subscription.trialStart = now;
    subscription.trialEnd = now.plusDays(plan.getTrialDays());
    subscription.currentPeriodStart = now;
    subscription.currentPeriodEnd = subscription.trialEnd;

    return subscription;
  }

  /**
   * Factory method to create a new active subscription without trial.
   * 
   * @param tenantId tenant identifier
   * @param userId user who created the subscription
   * @param plan subscription plan
   * @return a new Subscription in ACTIVE status
   */
  public static Subscription createActive(UUID tenantId, UUID userId, SubscriptionPlan plan) {
    validateTenantId(tenantId);
    validateUserId(userId);
    validatePlan(plan);

    var subscription = new Subscription(tenantId, userId, plan, SubscriptionStatus.ACTIVE);
    var now = LocalDateTime.now();
    subscription.currentPeriodStart = now;
    subscription.currentPeriodEnd = calculatePeriodEnd(now, plan);

    return subscription;
  }

  /**
   * Factory method to create a new incomplete subscription.
   * Used when subscription creation requires additional steps (e.g., payment method).
   * 
   * @param tenantId tenant identifier
   * @param userId user who created the subscription
   * @param plan subscription plan
   * @return a new Subscription in INCOMPLETE status
   */
  public static Subscription createIncomplete(UUID tenantId, UUID userId, SubscriptionPlan plan) {
    validateTenantId(tenantId);
    validateUserId(userId);
    validatePlan(plan);

    return new Subscription(tenantId, userId, plan, SubscriptionStatus.INCOMPLETE);
  }

  /**
   * Converts a trial subscription to active status.
   * Typically called when trial ends and payment method is on file.
   * 
   * @throws IllegalStateException if subscription is not in TRIAL status
   */
  public void convertTrialToActive() {
    if (status != SubscriptionStatus.TRIAL) {
      throw new IllegalStateException("Can only convert TRIAL subscriptions to ACTIVE");
    }

    transitionTo(SubscriptionStatus.ACTIVE);
    var now = LocalDateTime.now();
    currentPeriodStart = now;
    currentPeriodEnd = calculatePeriodEnd(now, plan);
  }

  /**
   * Marks the subscription as past due when payment fails.
   * 
   * @throws IllegalStateException if subscription is not in ACTIVE status
   */
  public void markPastDue() {
    if (status != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException("Can only mark ACTIVE subscriptions as PAST_DUE");
    }

    transitionTo(SubscriptionStatus.PAST_DUE);
  }

  /**
   * Reactivates a past due subscription after successful payment.
   * 
   * @throws IllegalStateException if subscription is not in PAST_DUE status
   */
  public void reactivateFromPastDue() {
    if (status != SubscriptionStatus.PAST_DUE) {
      throw new IllegalStateException("Can only reactivate PAST_DUE subscriptions");
    }

    transitionTo(SubscriptionStatus.ACTIVE);
  }

  /**
   * Cancels the subscription at the end of the current billing period.
   * Access is maintained until period end.
   */
  public void cancelAtPeriodEnd() {
    if (!status.canCancel()) {
      throw new IllegalStateException("Cannot cancel subscription in " + status + " status");
    }

    this.canceledAt = LocalDateTime.now();
    this.cancelAtPeriodEnd = true;
    transitionTo(SubscriptionStatus.CANCELED);
  }

  /**
   * Cancels the subscription immediately.
   * Access is revoked immediately.
   */
  public void cancelImmediately() {
    if (!status.canCancel()) {
      throw new IllegalStateException("Cannot cancel subscription in " + status + " status");
    }

    this.canceledAt = LocalDateTime.now();
    this.cancelAtPeriodEnd = false;
    transitionTo(SubscriptionStatus.EXPIRED);
  }

  /**
   * Reactivates a canceled subscription before period end.
   * 
   * @throws IllegalStateException if subscription cannot be reactivated
   */
  public void reactivate() {
    if (!status.canReactivate()) {
      throw new IllegalStateException("Cannot reactivate subscription in " + status + " status");
    }

    this.canceledAt = null;
    this.cancelAtPeriodEnd = false;
    transitionTo(SubscriptionStatus.ACTIVE);
  }

  /**
   * Expires the subscription when period ends or payment fails permanently.
   */
  public void expire() {
    transitionTo(SubscriptionStatus.EXPIRED);
  }

  /**
   * Suspends the subscription (admin action).
   */
  public void suspend() {
    transitionTo(SubscriptionStatus.SUSPENDED);
  }

  /**
   * Unsuspends the subscription (admin action).
   * 
   * @throws IllegalStateException if subscription is not suspended
   */
  public void unsuspend() {
    if (status != SubscriptionStatus.SUSPENDED) {
      throw new IllegalStateException("Can only unsuspend SUSPENDED subscriptions");
    }

    transitionTo(SubscriptionStatus.ACTIVE);
  }

  /**
   * Changes the subscription plan (upgrade or downgrade).
   * 
   * @param newPlan the new subscription plan
   * @throws IllegalStateException if plan change is not allowed
   */
  public void changePlan(SubscriptionPlan newPlan) {
    if (!status.canChangePlan()) {
      throw new IllegalStateException("Cannot change plan in " + status + " status");
    }

    validatePlan(newPlan);
    this.plan = newPlan;

    // Reset period for new plan
    var now = LocalDateTime.now();
    this.currentPeriodStart = now;
    this.currentPeriodEnd = calculatePeriodEnd(now, newPlan);
  }

  /**
   * Extends the trial period by the specified number of days.
   * 
   * @param additionalDays number of days to extend
   * @throws IllegalStateException if subscription is not in TRIAL status
   * @throws IllegalArgumentException if additionalDays is negative
   */
  public void extendTrial(int additionalDays) {
    if (status != SubscriptionStatus.TRIAL) {
      throw new IllegalStateException("Can only extend trial for TRIAL subscriptions");
    }

    if (additionalDays < 0) {
      throw new IllegalArgumentException("Additional days cannot be negative");
    }

    this.trialEnd = this.trialEnd.plusDays(additionalDays);
    this.currentPeriodEnd = this.trialEnd;
  }

  /**
   * Renews the subscription for the next billing period.
   * Updates period start and end dates.
   */
  public void renew() {
    if (status != SubscriptionStatus.ACTIVE) {
      throw new IllegalStateException("Can only renew ACTIVE subscriptions");
    }

    this.currentPeriodStart = this.currentPeriodEnd;
    this.currentPeriodEnd = calculatePeriodEnd(this.currentPeriodStart, plan);
  }

  /**
   * Adds or updates metadata for the subscription.
   * 
   * @param key metadata key
   * @param value metadata value
   */
  public void addMetadata(String key, Object value) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, value);
  }

  /**
   * Removes metadata from the subscription.
   * 
   * @param key metadata key to remove
   */
  public void removeMetadata(String key) {
    if (this.metadata != null) {
      this.metadata.remove(key);
    }
  }

  /**
   * Checks if the subscription is currently in trial period.
   * 
   * @return true if in trial and trial hasn't ended
   */
  public boolean isInTrial() {
    return status == SubscriptionStatus.TRIAL && 
           trialEnd != null && 
           LocalDateTime.now().isBefore(trialEnd);
  }

  /**
   * Checks if the subscription allows feature access.
   * 
   * @return true if status allows access
   */
  public boolean hasAccess() {
    return status.allowsAccess();
  }

  /**
   * Checks if the subscription is in an active billing state.
   * 
   * @return true if actively billing
   */
  public boolean isActiveBilling() {
    return status.isActiveBilling();
  }

  /**
   * Checks if the subscription period has ended.
   * 
   * @return true if current period end is in the past
   */
  public boolean isPeriodEnded() {
    return currentPeriodEnd != null && LocalDateTime.now().isAfter(currentPeriodEnd);
  }

  /**
   * Gets the number of days remaining in the current period.
   * 
   * @return days remaining, or 0 if period has ended
   */
  public long getDaysRemainingInPeriod() {
    if (currentPeriodEnd == null) {
      return 0;
    }

    var now = LocalDateTime.now();
    if (now.isAfter(currentPeriodEnd)) {
      return 0;
    }

    return java.time.Duration.between(now, currentPeriodEnd).toDays();
  }

  // Private helper methods

  private void transitionTo(SubscriptionStatus newStatus) {
    validateStatusTransition(this.status, newStatus);
    this.status = newStatus;
  }

  private static void validateStatusTransition(SubscriptionStatus from, SubscriptionStatus to) {
    // Use switch expression to validate allowed transitions
    var isValid = switch (from) {
      case TRIAL -> to == SubscriptionStatus.ACTIVE || 
                    to == SubscriptionStatus.EXPIRED || 
                    to == SubscriptionStatus.CANCELED;
      case ACTIVE -> to == SubscriptionStatus.PAST_DUE || 
                     to == SubscriptionStatus.CANCELED || 
                     to == SubscriptionStatus.SUSPENDED || 
                     to == SubscriptionStatus.EXPIRED;
      case PAST_DUE -> to == SubscriptionStatus.ACTIVE || 
                       to == SubscriptionStatus.EXPIRED || 
                       to == SubscriptionStatus.SUSPENDED;
      case CANCELED -> to == SubscriptionStatus.ACTIVE || 
                       to == SubscriptionStatus.EXPIRED;
      case SUSPENDED -> to == SubscriptionStatus.ACTIVE || 
                        to == SubscriptionStatus.EXPIRED;
      case INCOMPLETE -> to == SubscriptionStatus.TRIAL || 
                         to == SubscriptionStatus.ACTIVE || 
                         to == SubscriptionStatus.EXPIRED;
      case EXPIRED -> false; // Cannot transition from EXPIRED
    };

    if (!isValid) {
      throw new IllegalStateException(
          "Invalid status transition from " + from + " to " + to
      );
    }
  }

  private static LocalDateTime calculatePeriodEnd(LocalDateTime start, SubscriptionPlan plan) {
    return switch (plan.getBillingCycle()) {
      case MONTHLY -> start.plusMonths(1);
      case YEARLY -> start.plusYears(1);
      case LIFETIME -> start.plusYears(100); // Effectively never expires
    };
  }

  private static void validateTenantId(UUID tenantId) {
    if (tenantId == null) {
      throw new IllegalArgumentException("Tenant ID cannot be null");
    }
  }

  private static void validateUserId(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
  }

  private static void validatePlan(SubscriptionPlan plan) {
    if (plan == null) {
      throw new IllegalArgumentException("Subscription plan cannot be null");
    }
  }

  @PrePersist
  protected void onCreate() {
    var now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Getters

  public Long getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getUserId() {
    return userId;
  }

  public SubscriptionPlan getPlan() {
    return plan;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public LocalDateTime getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public LocalDateTime getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public LocalDateTime getTrialStart() {
    return trialStart;
  }

  public LocalDateTime getTrialEnd() {
    return trialEnd;
  }

  public LocalDateTime getCanceledAt() {
    return canceledAt;
  }

  public Boolean getCancelAtPeriodEnd() {
    return cancelAtPeriodEnd;
  }

  public Map<String, Object> getMetadata() {
    return metadata != null ? new HashMap<>(metadata) : new HashMap<>();
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Subscription that)) {
      return false;
    }
    return Objects.equals(id, that.id) && Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tenantId);
  }

  @Override
  public String toString() {
    return "Subscription{" +
        "id=" + id +
        ", tenantId=" + tenantId +
        ", status=" + status +
        ", plan=" + (plan != null ? plan.getPlanCode() : null) +
        ", currentPeriodEnd=" + currentPeriodEnd +
        '}';
  }
}
