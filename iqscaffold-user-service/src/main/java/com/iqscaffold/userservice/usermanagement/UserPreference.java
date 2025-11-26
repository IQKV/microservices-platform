package com.iqscaffold.userservice.usermanagement;

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

import com.iqscaffold.userservice.shared.TenantAware;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * UserPreference entity representing user-specific settings and preferences.
 * Maintains a one-to-one relationship with User.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreference extends TenantAware {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "locale", length = 10)
  private String locale = "en";

  @Column(name = "timezone", length = 50)
  private String timezone = "UTC";

  @Column(name = "currency", length = 3)
  private String currency = "USD";

  @Column(name = "date_format", length = 50)
  private String dateFormat = "yyyy-MM-dd";

  @Column(name = "time_format", length = 50)
  private String timeFormat = "HH:mm:ss";

  @Column(name = "theme", length = 20)
  private String theme = "light";

  @Column(name = "profile_photo_url", length = 500)
  private String profilePhotoUrl;

  @Column(name = "phone_number", length = 50)
  private String phoneNumber;

  @Column(name = "bio", columnDefinition = "TEXT")
  private String bio;

  @Column(name = "notification_email", nullable = false)
  private Boolean notificationEmail = true;

  @Column(name = "notification_sms", nullable = false)
  private Boolean notificationSms = false;

  @Column(name = "notification_push", nullable = false)
  private Boolean notificationPush = true;

  @Column(name = "two_factor_enabled", nullable = false)
  private Boolean twoFactorEnabled = false;

  @Column(name = "two_factor_method", length = 20)
  private String twoFactorMethod;

  @Column(name = "custom_settings", columnDefinition = "TEXT")
  private String customSettings;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected UserPreference() {
  }

  public UserPreference(final User user, final String tenantId) {
    super(tenantId);
    this.user = user;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getTimeFormat() {
    return timeFormat;
  }

  public void setTimeFormat(String timeFormat) {
    this.timeFormat = timeFormat;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public String getProfilePhotoUrl() {
    return profilePhotoUrl;
  }

  public void setProfilePhotoUrl(String profilePhotoUrl) {
    this.profilePhotoUrl = profilePhotoUrl;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public Boolean getNotificationEmail() {
    return notificationEmail;
  }

  public void setNotificationEmail(Boolean notificationEmail) {
    this.notificationEmail = notificationEmail;
  }

  public Boolean getNotificationSms() {
    return notificationSms;
  }

  public void setNotificationSms(Boolean notificationSms) {
    this.notificationSms = notificationSms;
  }

  public Boolean getNotificationPush() {
    return notificationPush;
  }

  public void setNotificationPush(Boolean notificationPush) {
    this.notificationPush = notificationPush;
  }

  public Boolean getTwoFactorEnabled() {
    return twoFactorEnabled;
  }

  public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
    this.twoFactorEnabled = twoFactorEnabled;
  }

  public String getTwoFactorMethod() {
    return twoFactorMethod;
  }

  public void setTwoFactorMethod(String twoFactorMethod) {
    this.twoFactorMethod = twoFactorMethod;
  }

  public String getCustomSettings() {
    return customSettings;
  }

  public void setCustomSettings(String customSettings) {
    this.customSettings = customSettings;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    var preference = (UserPreference) obj;
    return Objects.equals(id, preference.id)
           && Objects.equals(user, preference.user);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, user);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("UserPreference{")
        .append("id=").append(id)
        .append(", userId=").append(user != null ? user.getId() : null)
        .append(", locale='").append(locale).append('\'')
        .append(", timezone='").append(timezone).append('\'')
        .append(", theme='").append(theme).append('\'')
        .append(", tenantId='").append(getTenantId()).append('\'')
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
