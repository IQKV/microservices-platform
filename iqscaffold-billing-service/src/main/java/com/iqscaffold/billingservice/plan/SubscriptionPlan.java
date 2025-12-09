package com.iqscaffold.billingservice.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;

/**
 * SubscriptionPlan aggregate root entity representing a subscription plan in the public schema.
 *
 * <p>Subscription plans define the service tiers available to customers, including pricing,
 * features, quotas, and billing cycles. Plans are stored in the public schema and shared
 * across all tenants.
 *
 * <p>This aggregate enforces the following invariants:
 * <ul>
 *   <li>Plan code must be unique across all plans</li>
 *   <li>Base price must be non-negative</li>
 *   <li>Trial days must be non-negative</li>
 *   <li>FREE tier plans must have zero base price</li>
 *   <li>LIFETIME billing cycle plans cannot have trial periods</li>
 * </ul>
 *
 * <p>All modifications to the plan must go through this aggregate root to ensure
 * business rules and invariants are maintained.
 */
@Entity
@Table(name = "subscription_plans", schema = "public")
public class SubscriptionPlan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false, length = 50)
  private String planCode;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PlanTier tier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BillingCycle billingCycle;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal basePrice;

  @Column(nullable = false, length = 3)
  private String currency;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> features;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private PlanQuotas quotas;

  @Column(nullable = false)
  private Integer trialDays;

  @Column(nullable = false)
  private Boolean active;

  @Column(nullable = false)
  private Boolean publicPlan;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Default constructor for JPA.
   */
  protected SubscriptionPlan() {
    // JPA requires a no-arg constructor
  }

  /**
   * Creates a new subscription plan with the specified attributes.
   *
   * @param planCode     unique identifier for the plan
   * @param name         display name of the plan
   * @param description  detailed description of the plan
   * @param tier         plan tier (FREE, PRO, ENTERPRISE)
   * @param billingCycle billing frequency (MONTHLY, YEARLY, LIFETIME)
   * @param basePrice    price per billing cycle
   * @param currency     currency code (e.g., USD, EUR)
   * @param features     map of feature flags
   * @param quotas       quota limits for the plan
   * @param trialDays    number of trial days (0 for no trial)
   * @param publicPlan   whether the plan is publicly visible
   */
  private SubscriptionPlan(
      String planCode,
      String name,
      String description,
      PlanTier tier,
      BillingCycle billingCycle,
      BigDecimal basePrice,
      String currency,
      Map<String, Object> features,
      PlanQuotas quotas,
      Integer trialDays,
      Boolean publicPlan) {
    this.planCode = planCode;
    this.name = name;
    this.description = description;
    this.tier = tier;
    this.billingCycle = billingCycle;
    this.basePrice = basePrice;
    this.currency = currency;
    this.features = features != null ? new HashMap<>(features) : new HashMap<>();
    this.quotas = quotas;
    this.trialDays = trialDays;
    this.active = true;
    this.publicPlan = publicPlan;
  }

  /**
   * Factory method to create a new subscription plan.
   * Validates business rules and invariants before creation.
   *
   * @param planCode     unique identifier for the plan
   * @param name         display name of the plan
   * @param description  detailed description of the plan
   * @param tier         plan tier (FREE, PRO, ENTERPRISE)
   * @param billingCycle billing frequency (MONTHLY, YEARLY, LIFETIME)
   * @param basePrice    price per billing cycle
   * @param currency     currency code (e.g., USD, EUR)
   * @param features     map of feature flags
   * @param quotas       quota limits for the plan
   * @param trialDays    number of trial days (0 for no trial)
   * @param publicPlan   whether the plan is publicly visible
   * @return a new SubscriptionPlan instance
   * @throws IllegalArgumentException if any business rule is violated
   */
  public static SubscriptionPlan create(
      String planCode,
      String name,
      String description,
      PlanTier tier,
      BillingCycle billingCycle,
      BigDecimal basePrice,
      String currency,
      Map<String, Object> features,
      PlanQuotas quotas,
      Integer trialDays,
      Boolean publicPlan) {

    validatePlanCode(planCode);
    validateName(name);
    validateTier(tier);
    validateBillingCycle(billingCycle);
    validateBasePrice(basePrice);
    validateCurrency(currency);
    validateTrialDays(trialDays);
    validateFreeTierPrice(tier, basePrice);
    validateLifetimeTrial(billingCycle, trialDays);

    return new SubscriptionPlan(
        planCode,
        name,
        description,
        tier,
        billingCycle,
        basePrice,
        currency,
        features,
        quotas != null ? quotas : PlanQuotas.unlimited(),
        trialDays,
        publicPlan != null ? publicPlan : true);
  }

  /**
   * Updates the plan name and description.
   *
   * @param name        new display name
   * @param description new description
   */
  public void updateDetails(String name, String description) {
    validateName(name);
    this.name = name;
    this.description = description;
  }

  /**
   * Updates the plan features.
   *
   * @param features new feature map
   */
  public void updateFeatures(Map<String, Object> features) {
    this.features = features != null ? new HashMap<>(features) : new HashMap<>();
  }

  /**
   * Updates the plan quotas.
   *
   * @param quotas new quota limits
   */
  public void updateQuotas(PlanQuotas quotas) {
    this.quotas = quotas != null ? quotas : PlanQuotas.unlimited();
  }

  /**
   * Updates the plan pricing.
   * Note: Price changes only affect new subscriptions, not existing ones.
   *
   * @param basePrice new price per billing cycle
   * @param currency  currency code
   */
  public void updatePricing(BigDecimal basePrice, String currency) {
    validateBasePrice(basePrice);
    validateCurrency(currency);
    validateFreeTierPrice(this.tier, basePrice);
    this.basePrice = basePrice;
    this.currency = currency;
  }

  /**
   * Activates the plan, making it available for new subscriptions.
   */
  public void activate() {
    this.active = true;
  }

  /**
   * Deactivates the plan, preventing new subscriptions.
   * Existing subscriptions are not affected.
   */
  public void deactivate() {
    this.active = false;
  }

  /**
   * Makes the plan publicly visible.
   */
  public void makePublic() {
    this.publicPlan = true;
  }

  /**
   * Makes the plan private (only visible to admins).
   */
  public void makePrivate() {
    this.publicPlan = false;
  }

  /**
   * Checks if the plan offers a trial period.
   *
   * @return true if trial days > 0
   */
  public boolean hasTrial() {
    return trialDays != null && trialDays > 0;
  }

  /**
   * Checks if the plan is free.
   *
   * @return true if tier is FREE
   */
  public boolean isFree() {
    return tier == PlanTier.FREE;
  }

  /**
   * Checks if the plan requires payment.
   *
   * @return true if tier is not FREE
   */
  public boolean requiresPayment() {
    return tier != PlanTier.FREE;
  }

  /**
   * Checks if the plan is a lifetime plan.
   *
   * @return true if billing cycle is LIFETIME
   */
  public boolean isLifetime() {
    return billingCycle == BillingCycle.LIFETIME;
  }

  // Validation methods

  private static void validatePlanCode(String planCode) {
    if (planCode == null || planCode.isBlank()) {
      throw new IllegalArgumentException("Plan code cannot be null or blank");
    }
    if (planCode.length() > 50) {
      throw new IllegalArgumentException("Plan code cannot exceed 50 characters");
    }
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Plan name cannot be null or blank");
    }
    if (name.length() > 100) {
      throw new IllegalArgumentException("Plan name cannot exceed 100 characters");
    }
  }

  private static void validateTier(PlanTier tier) {
    if (tier == null) {
      throw new IllegalArgumentException("Plan tier cannot be null");
    }
  }

  private static void validateBillingCycle(BillingCycle billingCycle) {
    if (billingCycle == null) {
      throw new IllegalArgumentException("Billing cycle cannot be null");
    }
  }

  private static void validateBasePrice(BigDecimal basePrice) {
    if (basePrice == null) {
      throw new IllegalArgumentException("Base price cannot be null");
    }
    if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Base price cannot be negative");
    }
  }

  private static void validateCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency cannot be null or blank");
    }
    if (currency.length() != 3) {
      throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
    }
  }

  private static void validateTrialDays(Integer trialDays) {
    if (trialDays == null) {
      throw new IllegalArgumentException("Trial days cannot be null");
    }
    if (trialDays < 0) {
      throw new IllegalArgumentException("Trial days cannot be negative");
    }
  }

  private static void validateFreeTierPrice(PlanTier tier, BigDecimal basePrice) {
    if (tier == PlanTier.FREE && basePrice.compareTo(BigDecimal.ZERO) != 0) {
      throw new IllegalArgumentException("FREE tier plans must have zero base price");
    }
  }

  private static void validateLifetimeTrial(BillingCycle billingCycle, Integer trialDays) {
    if (billingCycle == BillingCycle.LIFETIME && trialDays > 0) {
      throw new IllegalArgumentException("LIFETIME billing cycle plans cannot have trial periods");
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

  public String getPlanCode() {
    return planCode;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public PlanTier getTier() {
    return tier;
  }

  public BillingCycle getBillingCycle() {
    return billingCycle;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public String getCurrency() {
    return currency;
  }

  public Map<String, Object> getFeatures() {
    return new HashMap<>(features);
  }

  public PlanQuotas getQuotas() {
    return quotas;
  }

  public Integer getTrialDays() {
    return trialDays;
  }

  public Boolean getActive() {
    return active;
  }

  public Boolean getPublicPlan() {
    return publicPlan;
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
    if (!(o instanceof SubscriptionPlan that)) {
      return false;
    }
    return Objects.equals(id, that.id) && Objects.equals(planCode, that.planCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, planCode);
  }

  @Override
  public String toString() {
    return "SubscriptionPlan{" +
           "id=" + id +
           ", planCode='" + planCode + '\'' +
           ", name='" + name + '\'' +
           ", tier=" + tier +
           ", billingCycle=" + billingCycle +
           ", basePrice=" + basePrice +
           ", currency='" + currency + '\'' +
           ", active=" + active +
           '}';
  }
}
