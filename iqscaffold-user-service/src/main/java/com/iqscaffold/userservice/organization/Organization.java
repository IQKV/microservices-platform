package com.iqscaffold.userservice.organization;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

import com.iqscaffold.userservice.shared.PaymentGatewayProvider;
import com.iqscaffold.userservice.tenancy.Tenant;
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
 *   <li>Integration with billing service (payment gateway accounts)</li>
 * </ul>
 *
 * <h3>Key Relationships</h3>
 * <ul>
 *   <li><strong>Tenant</strong> - One-to-one relationship via tenant_id (unique)</li>
 *   <li><strong>Owner User</strong> - Reference to the primary user (stored in tenant schema)</li>
 *   <li><strong>Preferences</strong> - One-to-one relationship with organization preferences</li>
 * </ul>
 *
 * <h3>Payment Gateway Integration</h3>
 * <ul>
 *   <li><strong>paymentGatewayAccountId</strong> - Account ID in payment gateway (e.g., Stripe Connect, PayPal merchant)</li>
 *   <li><strong>paymentGatewayProvider</strong> - Gateway provider type (STRIPE, PAYPAL, SQUARE, BRAINTREE)</li>
 *   <li><strong>chargesEnabled</strong> - Whether organization can accept payments</li>
 *   <li><strong>payoutsEnabled</strong> - Whether organization can receive payouts</li>
 * </ul>
 *
 * <h3>Subscription Management</h3>
 * <ul>
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
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "organization-with-tenant",
        attributeNodes = {
            @NamedAttributeNode("tenant")
        }
    ),
    @NamedEntityGraph(
        name = "organization-with-preferences",
        attributeNodes = {
            @NamedAttributeNode("preference")
        }
    ),
    @NamedEntityGraph(
        name = "organization-complete",
        attributeNodes = {
            @NamedAttributeNode("tenant"),
            @NamedAttributeNode("preference")
        }
    )
})
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

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false)
  private Tenant tenant;

  @Column(name = "owner_user_id")
  private Long ownerUserId;

  @Column(name = "billing_email", length = 255)
  private String billingEmail;

  /**
   * Payment gateway account ID (e.g., Stripe Connect account, PayPal merchant ID).
   */
  @Column(name = "payment_gateway_account_id", length = 255)
  private String paymentGatewayAccountId;

  /**
   * Payment gateway provider type.
   */
  @Column(name = "payment_gateway_provider", length = 50)
  @Enumerated(EnumType.STRING)
  private PaymentGatewayProvider paymentGatewayProvider;

  /**
   * Whether the organization can accept charges/payments through the gateway.
   */
  @Column(name = "charges_enabled")
  private Boolean chargesEnabled = false;

  /**
   * Whether the organization can receive payouts through the gateway.
   */
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

  // Getters and Setters

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

  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
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

  public String getPaymentGatewayAccountId() {
    return paymentGatewayAccountId;
  }

  public void setPaymentGatewayAccountId(String paymentGatewayAccountId) {
    this.paymentGatewayAccountId = paymentGatewayAccountId;
  }

  public PaymentGatewayProvider getPaymentGatewayProvider() {
    return paymentGatewayProvider;
  }

  public void setPaymentGatewayProvider(PaymentGatewayProvider paymentGatewayProvider) {
    this.paymentGatewayProvider = paymentGatewayProvider;
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

  // Business Logic Methods

  public boolean isActive() {
    return enabled != null && enabled;
  }

  public String getLocation() {
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

  /**
   * Check if organization has a payment gateway account configured.
   */
  public boolean hasPaymentGatewayAccount() {
    return paymentGatewayAccountId != null && !paymentGatewayAccountId.isEmpty();
  }

  /**
   * Check if organization can accept payments through the configured gateway.
   */
  public boolean canAcceptPayments() {
    return hasPaymentGatewayAccount() && Boolean.TRUE.equals(chargesEnabled);
  }

  /**
   * Check if organization can receive payouts through the configured gateway.
   */
  public boolean canReceivePayouts() {
    return hasPaymentGatewayAccount() && Boolean.TRUE.equals(payoutsEnabled);
  }

  /**
   * Check if organization is using a specific payment gateway provider.
   */
  public boolean isUsingProvider(PaymentGatewayProvider provider) {
    return paymentGatewayProvider == provider;
  }

  /**
   * Equals based on business key (tenantId) which is unique and immutable.
   * This is JPA-safe as it doesn't rely on the id field which may be null before persistence.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Organization organization = (Organization) obj;
    return Objects.equals(tenantId, organization.tenantId);
  }

  /**
   * HashCode based on business key (tenantId) to ensure consistency with equals.
   * Using a constant hash for null tenantId to handle edge cases during construction.
   */
  @Override
  public int hashCode() {
    return tenantId != null ? tenantId.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Organization{" +
           "id=" + id +
           ", name='" + name + '\'' +
           ", tenantId='" + tenantId + '\'' +
           ", industry='" + industry + '\'' +
           ", enabled=" + enabled +
           ", paymentGatewayProvider=" + paymentGatewayProvider +
           ", subscriptionStatus='" + subscriptionStatus + '\'' +
           ", createdAt=" + createdAt +
           '}';
  }
}
