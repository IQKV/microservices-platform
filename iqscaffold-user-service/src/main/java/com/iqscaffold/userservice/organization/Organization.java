package com.iqscaffold.userservice.organization;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Organization entity representing billing entities in the multi-tenant SaaS architecture.
 * 
 * <p>Organizations are stored in the PUBLIC schema (system-wide) and have a 1:1 relationship
 * with tenants. Each organization represents a billing entity with its own tenant schema for
 * data isolation. This design enables:
 * <ul>
 *   <li>Centralized billing and subscription management</li>
 *   <li>Cross-tenant reporting and analytics</li>
 *   <li>Clear ownership: Organization owns Tenant owns Users</li>
 *   <li>Integration with billing service (Stripe Connect accounts)</li>
 * </ul>
 *
 * <h3>Key Relationships</h3>
 * <ul>
 *   <li><strong>Tenant</strong> - One-to-one relationship via tenant_id (unique)</li>
 *   <li><strong>Owner User</strong> - Reference to the primary user (stored in tenant schema)</li>
 *   <li><strong>Preferences</strong> - One-to-one relationship with organization preferences</li>
 * </ul>
 *
 * <h3>Billing Integration</h3>
 * <ul>
 *   <li><strong>stripeAccountId</strong> - Stripe Connect account for payment processing</li>
 *   <li><strong>subscriptionStatus</strong> - Current subscription state (active, canceled, etc.)</li>
 *   <li><strong>subscriptionPlan</strong> - Plan identifier (basic, pro, enterprise)</li>
 *   <li><strong>billingEmail</strong> - Email for billing notifications</li>
 *   <li><strong>maxUsers</strong> - User limit based on subscription plan</li>
 * </ul>
 */
@Entity
@Table(name = "organizations", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.userservice.organization.Organization")
public class Organization {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "industry", length = 100)
  private String industry;

  @Column(name = "website", length = 255)
  private String website;

  @Column(name = "phone", length = 50)
  private String phone;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "city", length = 100)
  private String city;

  @Column(name = "country", length = 100)
  private String country;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "tenant_id", nullable = false, unique = true, length = 100)
  private String tenantId;

  @Column(name = "owner_user_id")
  private Long ownerUserId;

  @Column(name = "billing_email", length = 255)
  private String billingEmail;

  @Column(name = "stripe_account_id", length = 255)
  private String stripeAccountId;

  @Column(name = "charges_enabled")
  private Boolean chargesEnabled = false;

  @Column(name = "payouts_enabled")
  private Boolean payoutsEnabled = false;

  @Column(name = "subscription_status", length = 50)
  private String subscriptionStatus;

  @Column(name = "subscription_plan", length = 100)
  private String subscriptionPlan;

  @Column(name = "max_users")
  private Integer maxUsers;

  @OneToOne(mappedBy = "organization")
  private OrganizationPreference preference;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  protected Organization() {
  }

  public Organization(final String name, final String tenantId) {
    this.name = name;
    this.tenantId = tenantId;
  }

  public Long getId() {
    return id;
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

  public String getIndustry() {
    return industry;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public String getBillingEmail() {
    return billingEmail;
  }

  public void setBillingEmail(String billingEmail) {
    this.billingEmail = billingEmail;
  }

  public String getStripeAccountId() {
    return stripeAccountId;
  }

  public void setStripeAccountId(String stripeAccountId) {
    this.stripeAccountId = stripeAccountId;
  }

  public Boolean getChargesEnabled() {
    return chargesEnabled;
  }

  public void setChargesEnabled(Boolean chargesEnabled) {
    this.chargesEnabled = chargesEnabled;
  }

  public Boolean getPayoutsEnabled() {
    return payoutsEnabled;
  }

  public void setPayoutsEnabled(Boolean payoutsEnabled) {
    this.payoutsEnabled = payoutsEnabled;
  }

  public String getSubscriptionStatus() {
    return subscriptionStatus;
  }

  public void setSubscriptionStatus(String subscriptionStatus) {
    this.subscriptionStatus = subscriptionStatus;
  }

  public String getSubscriptionPlan() {
    return subscriptionPlan;
  }

  public void setSubscriptionPlan(String subscriptionPlan) {
    this.subscriptionPlan = subscriptionPlan;
  }

  public Integer getMaxUsers() {
    return maxUsers;
  }

  public void setMaxUsers(Integer maxUsers) {
    this.maxUsers = maxUsers;
  }

  public OrganizationPreference getPreference() {
    return preference;
  }

  public void setPreference(OrganizationPreference preference) {
    this.preference = preference;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public boolean isActive() {
    var enabled = this.enabled;
    return enabled != null && enabled;
  }

  public String getLocation() {
    var city = this.city;
    var country = this.country;
    if (city != null && country != null) {
      return city + ", " + country;
    }
    return city != null ? city : (country != null ? country : "");
  }

  public boolean hasSubscription() {
    return subscriptionStatus != null && !subscriptionStatus.isEmpty();
  }

  public boolean isSubscriptionActive() {
    return "active".equalsIgnoreCase(subscriptionStatus);
  }

  public boolean hasStripeAccount() {
    return stripeAccountId != null && !stripeAccountId.isEmpty();
  }

  public boolean canAcceptPayments() {
    return hasStripeAccount() && Boolean.TRUE.equals(chargesEnabled);
  }

  public boolean canReceivePayouts() {
    return hasStripeAccount() && Boolean.TRUE.equals(payoutsEnabled);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    var organization = (Organization) obj;
    return Objects.equals(id, organization.id)
           && Objects.equals(tenantId, organization.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tenantId);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("Organization{")
        .append("id=").append(id)
        .append(", name='").append(name).append('\'')
        .append(", tenantId='").append(tenantId).append('\'')
        .append(", industry='").append(industry).append('\'')
        .append(", enabled=").append(enabled)
        .append(", subscriptionStatus='").append(subscriptionStatus).append('\'')
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
