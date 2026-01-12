package com.iqscaffold.userservice.tenancy;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

import com.iqscaffold.userservice.organization.Organization;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Tenant entity representing isolated customer environments in the multi-tenant architecture. Each tenant has its own isolated data and configuration settings.
 * This entity is stored in the public schema as it contains system-wide tenant metadata.
 */
@Entity
@Table(name = "tenants", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.userservice.tenancy.Tenant")
public class Tenant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, unique = true, length = 100)
  private String tenantId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TenantStatus status = TenantStatus.ACTIVE;

  @Column(name = "domain", length = 255)
  private String domain;

  @Column(name = "max_users")
  private Integer maxUsers;

  @Column(name = "storage_quota_gb")
  private Integer storageQuotaGb;

  @Column(name = "api_rate_limit_per_minute")
  private Integer apiRateLimitPerMinute;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Column(name = "suspended_at")
  private LocalDateTime suspendedAt;

  @Column(name = "archived_at")
  private LocalDateTime archivedAt;

  @Column(name = "suspension_reason", length = 500)
  private String suspensionReason;

  @Column(name = "archived_reason", length = 500)
  private String archivedReason;

  @OneToOne(mappedBy = "tenant", fetch = FetchType.LAZY)
  private Organization organization;

  // Default constructor for JPA
  protected Tenant() {
  }

  // Constructor with required fields
  public Tenant(final String tenantId, final String name) {
    this.tenantId = tenantId;
    this.name = name;
  }

  // Constructor with common fields
  public Tenant(final String tenantId, final String name, final String description, final String createdBy) {
    this.tenantId = tenantId;
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
  }

  // Getters and setters using modern Java syntax
  public Long getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
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

  public TenantStatus getStatus() {
    return status;
  }

  public void setStatus(TenantStatus status) {
    this.status = status;
  }

  public LocalDateTime getSuspendedAt() {
    return suspendedAt;
  }

  public void setSuspendedAt(LocalDateTime suspendedAt) {
    this.suspendedAt = suspendedAt;
  }

  public LocalDateTime getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(LocalDateTime archivedAt) {
    this.archivedAt = archivedAt;
  }

  public String getSuspensionReason() {
    return suspensionReason;
  }

  public void setSuspensionReason(String suspensionReason) {
    this.suspensionReason = suspensionReason;
  }

  public String getArchivedReason() {
    return archivedReason;
  }

  public void setArchivedReason(String archivedReason) {
    this.archivedReason = archivedReason;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public Integer getMaxUsers() {
    return maxUsers;
  }

  public void setMaxUsers(Integer maxUsers) {
    this.maxUsers = maxUsers;
  }

  public Integer getStorageQuotaGb() {
    return storageQuotaGb;
  }

  public void setStorageQuotaGb(Integer storageQuotaGb) {
    this.storageQuotaGb = storageQuotaGb;
  }

  public Integer getApiRateLimitPerMinute() {
    return apiRateLimitPerMinute;
  }

  public void setApiRateLimitPerMinute(Integer apiRateLimitPerMinute) {
    this.apiRateLimitPerMinute = apiRateLimitPerMinute;
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

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  // Utility methods
  public boolean isActive() {
    return status == TenantStatus.ACTIVE;
  }

  public boolean isSuspended() {
    return status == TenantStatus.SUSPENDED;
  }

  public boolean isArchived() {
    return status == TenantStatus.ARCHIVED;
  }

  public boolean hasUserQuota() {
    var maxUsers = this.maxUsers;
    return maxUsers != null && maxUsers > 0;
  }

  public boolean hasStorageQuota() {
    var storageQuota = this.storageQuotaGb;
    return storageQuota != null && storageQuota > 0;
  }

  public boolean hasRateLimit() {
    var rateLimit = this.apiRateLimitPerMinute;
    return rateLimit != null && rateLimit > 0;
  }

  public String getDisplayName() {
    var name = this.name;
    var tenantId = this.tenantId;
    return name != null && !name.trim().isEmpty() ? name : tenantId;
  }

  // Standard equals, hashCode, and toString methods
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    var tenant = (Tenant) obj;
    return Objects.equals(id, tenant.id)
           && Objects.equals(tenantId, tenant.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tenantId);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("Tenant{")
        .append("id=").append(id)
        .append(", tenantId='").append(tenantId).append("'")
        .append(", name='").append(name).append("'")
        .append(", status=").append(status)
        .append(", domain='").append(domain).append("'")
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
