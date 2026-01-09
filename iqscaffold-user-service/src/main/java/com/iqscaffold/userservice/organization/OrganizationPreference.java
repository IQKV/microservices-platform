package com.iqscaffold.userservice.organization;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * OrganizationPreference entity representing organization-specific settings and user defaults.
 * Stored in PUBLIC schema alongside Organization entity. Maintains a one-to-one relationship with Organization.
 */
@Entity
@Table(name = "organization_preferences", schema = "public")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.userservice.organization.OrganizationPreference")
public class OrganizationPreference {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false, unique = true)
  private Organization organization;

  @Column(name = "default_locale", length = 10)
  private String defaultLocale = "en";

  @Column(name = "default_timezone", length = 50)
  private String defaultTimezone = "UTC";

  @Column(name = "default_currency", length = 3)
  private String defaultCurrency = "USD";

  @Column(name = "password_min_length")
  private Integer passwordMinLength = 8;

  @Column(name = "password_require_uppercase")
  private Boolean passwordRequireUppercase = true;

  @Column(name = "password_require_lowercase")
  private Boolean passwordRequireLowercase = true;

  @Column(name = "password_require_numbers")
  private Boolean passwordRequireNumbers = true;

  @Column(name = "password_require_special_chars")
  private Boolean passwordRequireSpecialChars = true;

  @Column(name = "session_timeout_minutes")
  private Integer sessionTimeoutMinutes = 30;

  @Column(name = "max_login_attempts")
  private Integer maxLoginAttempts = 5;

  @Column(name = "lockout_duration_minutes")
  private Integer lockoutDurationMinutes = 15;

  @Column(name = "two_factor_auth_required")
  private Boolean twoFactorAuthRequired = false;

  @Column(name = "allow_user_registration")
  private Boolean allowUserRegistration = true;

  @Column(name = "require_email_verification")
  private Boolean requireEmailVerification = true;

  @Column(name = "notification_email", length = 255)
  private String notificationEmail;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected OrganizationPreference() {
  }

  public OrganizationPreference(final Organization organization) {
    this.organization = organization;
  }

  public Long getId() {
    return id;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public String getDefaultLocale() {
    return defaultLocale;
  }

  public void setDefaultLocale(String defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  public String getDefaultTimezone() {
    return defaultTimezone;
  }

  public void setDefaultTimezone(String defaultTimezone) {
    this.defaultTimezone = defaultTimezone;
  }

  public String getDefaultCurrency() {
    return defaultCurrency;
  }

  public void setDefaultCurrency(String defaultCurrency) {
    this.defaultCurrency = defaultCurrency;
  }

  public Boolean getAllowUserRegistration() {
    return allowUserRegistration;
  }

  public void setAllowUserRegistration(Boolean allowUserRegistration) {
    this.allowUserRegistration = allowUserRegistration;
  }

  public Boolean getRequireEmailVerification() {
    return requireEmailVerification;
  }

  public void setRequireEmailVerification(Boolean requireEmailVerification) {
    this.requireEmailVerification = requireEmailVerification;
  }

  public Integer getPasswordMinLength() {
    return passwordMinLength;
  }

  public void setPasswordMinLength(Integer passwordMinLength) {
    this.passwordMinLength = passwordMinLength;
  }

  public Boolean getPasswordRequireUppercase() {
    return passwordRequireUppercase;
  }

  public void setPasswordRequireUppercase(Boolean passwordRequireUppercase) {
    this.passwordRequireUppercase = passwordRequireUppercase;
  }

  public Boolean getPasswordRequireLowercase() {
    return passwordRequireLowercase;
  }

  public void setPasswordRequireLowercase(Boolean passwordRequireLowercase) {
    this.passwordRequireLowercase = passwordRequireLowercase;
  }

  public Boolean getPasswordRequireNumbers() {
    return passwordRequireNumbers;
  }

  public void setPasswordRequireNumbers(Boolean passwordRequireNumbers) {
    this.passwordRequireNumbers = passwordRequireNumbers;
  }

  public Boolean getPasswordRequireSpecialChars() {
    return passwordRequireSpecialChars;
  }

  public void setPasswordRequireSpecialChars(Boolean passwordRequireSpecialChars) {
    this.passwordRequireSpecialChars = passwordRequireSpecialChars;
  }

  public Integer getSessionTimeoutMinutes() {
    return sessionTimeoutMinutes;
  }

  public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
    this.sessionTimeoutMinutes = sessionTimeoutMinutes;
  }

  public Integer getMaxLoginAttempts() {
    return maxLoginAttempts;
  }

  public void setMaxLoginAttempts(Integer maxLoginAttempts) {
    this.maxLoginAttempts = maxLoginAttempts;
  }

  public Integer getLockoutDurationMinutes() {
    return lockoutDurationMinutes;
  }

  public void setLockoutDurationMinutes(Integer lockoutDurationMinutes) {
    this.lockoutDurationMinutes = lockoutDurationMinutes;
  }

  public Boolean getTwoFactorAuthRequired() {
    return twoFactorAuthRequired;
  }

  public void setTwoFactorAuthRequired(Boolean twoFactorAuthRequired) {
    this.twoFactorAuthRequired = twoFactorAuthRequired;
  }

  public String getNotificationEmail() {
    return notificationEmail;
  }

  public void setNotificationEmail(String notificationEmail) {
    this.notificationEmail = notificationEmail;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Equals based on id only to avoid circular references with Organization.
   * For transient entities (id == null), only reference equality is used.
   * This is JPA-safe and avoids lazy loading issues.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    var preference = (OrganizationPreference) obj;
    // For transient entities, only reference equality
    if (id == null || preference.id == null) {
      return false;
    }
    return Objects.equals(id, preference.id);
  }

  /**
   * HashCode based on a constant to ensure consistency across persistence lifecycle.
   * This prevents issues when entities are added to collections before persistence.
   */
  @Override
  public int hashCode() {
    // Use a constant hash to ensure consistency before and after persistence
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("OrganizationPreference{")
        .append("id=").append(id)
        .append(", organizationId=").append(organization != null ? organization.getId() : null)
        .append(", defaultLocale='").append(defaultLocale).append('\'')
        .append(", defaultTimezone='").append(defaultTimezone).append('\'')
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
