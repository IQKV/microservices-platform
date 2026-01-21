package com.iqscaffold.billingservice.subscription;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.iqscaffold.billingservice.feature.PlanFeature;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Platform-wide subscription plan entity.
 * <p>
 * Represents the pricing plans available to all tenants (e.g., Starter, Pro, Enterprise).
 * Stored in public schema as plans are shared across all tenants.
 *
 * <h3>Entity Graphs</h3>
 * <ul>
 *   <li><strong>plan-with-features</strong> - Eagerly loads plan features for feature checking</li>
 *   <li><strong>plan-with-features-and-definitions</strong> - Loads features with their definitions for complete feature context</li>
 * </ul>
 */
@Entity
@Table(name = "subscription_plan", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.billingservice.subscription.SubscriptionPlan")
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "plan-with-features",
        attributeNodes = {
            @NamedAttributeNode("planFeatures")
        }
    ),
    @NamedEntityGraph(
        name = "plan-with-features-and-definitions",
        attributeNodes = {
            @NamedAttributeNode(value = "planFeatures", subgraph = "planFeature-with-definition")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "planFeature-with-definition",
                attributeNodes = {
                    @NamedAttributeNode("feature")
                }
            )
        }
    )
})
public class SubscriptionPlan {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SubscriptionInterval interval;

  @Column(name = "interval_count", nullable = false)
  private Integer intervalCount = 1;

  @Column(name = "trial_period_days")
  private Integer trialPeriodDays = 0;

  @Column(name = "stripe_product_id")
  private String stripeProductId;

  @Column(name = "stripe_price_id")
  private String stripePriceId;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;



  /**
   * Structured features enabled for this plan.
   * Replaces the legacy JSON features field with proper entity relationships.
   */
  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<PlanFeature> planFeatures = new HashSet<>();

  @Column(name = "max_users")
  private Integer maxUsers;

  @Column(name = "max_storage_gb")
  private Integer maxStorageGb;

  @Column(name = "max_api_calls")
  private Long maxApiCalls;

  /**
   * Additional metadata (JSON format).
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SubscriptionPlan() {
  }

  // Getters and setters

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public SubscriptionInterval getInterval() {
    return interval;
  }

  public void setInterval(SubscriptionInterval interval) {
    this.interval = interval;
  }

  public Integer getIntervalCount() {
    return intervalCount;
  }

  public void setIntervalCount(Integer intervalCount) {
    this.intervalCount = intervalCount;
  }

  public Integer getTrialPeriodDays() {
    return trialPeriodDays;
  }

  public void setTrialPeriodDays(Integer trialPeriodDays) {
    this.trialPeriodDays = trialPeriodDays;
  }

  public String getStripeProductId() {
    return stripeProductId;
  }

  public void setStripeProductId(String stripeProductId) {
    this.stripeProductId = stripeProductId;
  }

  public String getStripePriceId() {
    return stripePriceId;
  }

  public void setStripePriceId(String stripePriceId) {
    this.stripePriceId = stripePriceId;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }



  public Set<PlanFeature> getPlanFeatures() {
    return planFeatures;
  }

  public void setPlanFeatures(Set<PlanFeature> planFeatures) {
    this.planFeatures = planFeatures;
  }

  /**
   * Adds a feature to this plan.
   */
  public void addFeature(PlanFeature planFeature) {
    planFeatures.add(planFeature);
    planFeature.setPlan(this);
  }

  /**
   * Removes a feature from this plan.
   */
  public void removeFeature(PlanFeature planFeature) {
    planFeatures.remove(planFeature);
    planFeature.setPlan(null);
  }

  public Integer getMaxUsers() {
    return maxUsers;
  }

  public void setMaxUsers(Integer maxUsers) {
    this.maxUsers = maxUsers;
  }

  public Integer getMaxStorageGb() {
    return maxStorageGb;
  }

  public void setMaxStorageGb(Integer maxStorageGb) {
    this.maxStorageGb = maxStorageGb;
  }

  public Long getMaxApiCalls() {
    return maxApiCalls;
  }

  public void setMaxApiCalls(Long maxApiCalls) {
    this.maxApiCalls = maxApiCalls;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  // Business methods

  public boolean isActive() {
    return Boolean.TRUE.equals(isActive);
  }

  public boolean hasTrial() {
    return trialPeriodDays != null && trialPeriodDays > 0;
  }
}
