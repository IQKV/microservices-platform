package org.gripday.userservice.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Entity tests for UserPreference to achieve branch coverage.
 */
class UserPreferenceEntityTest {

  @Test
  @DisplayName("Should create user preference with defaults")
  void shouldCreateUserPreferenceWithDefaults() {
    var user = new User("testuser", "test@example.com", "hash", "Test", "User", "tenant-123");
    var preference = new UserPreference(user, "tenant-123");

    assertThat(preference.getUser()).isEqualTo(user);
    assertThat(preference.getTenantId()).isEqualTo("tenant-123");
    assertThat(preference.getLocale()).isEqualTo("en");
    assertThat(preference.getTimezone()).isEqualTo("UTC");
    assertThat(preference.getCurrency()).isEqualTo("USD");
    assertThat(preference.getTheme()).isEqualTo("light");
    assertThat(preference.getNotificationEmail()).isTrue();
    assertThat(preference.getNotificationSms()).isFalse();
    assertThat(preference.getNotificationPush()).isTrue();
    assertThat(preference.getTwoFactorEnabled()).isFalse();
  }

  @Test
  @DisplayName("Should set and get all properties")
  void shouldSetAndGetAllProperties() {
    var user = new User("testuser", "test@example.com", "hash", "Test", "User", "tenant-123");
    var preference = new UserPreference(user, "tenant-123");

    preference.setLocale("fr");
    preference.setTimezone("Europe/Paris");
    preference.setCurrency("EUR");
    preference.setDateFormat("dd/MM/yyyy");
    preference.setTimeFormat("HH:mm");
    preference.setTheme("dark");
    preference.setProfilePhotoUrl("https://example.com/photo.jpg");
    preference.setPhoneNumber("+1234567890");
    preference.setBio("Test bio");
    preference.setNotificationEmail(false);
    preference.setNotificationSms(true);
    preference.setNotificationPush(false);
    preference.setTwoFactorEnabled(true);
    preference.setTwoFactorMethod("sms");
    preference.setCustomSettings("{\"key\":\"value\"}");

    assertThat(preference.getLocale()).isEqualTo("fr");
    assertThat(preference.getTimezone()).isEqualTo("Europe/Paris");
    assertThat(preference.getCurrency()).isEqualTo("EUR");
    assertThat(preference.getDateFormat()).isEqualTo("dd/MM/yyyy");
    assertThat(preference.getTimeFormat()).isEqualTo("HH:mm");
    assertThat(preference.getTheme()).isEqualTo("dark");
    assertThat(preference.getProfilePhotoUrl()).isEqualTo("https://example.com/photo.jpg");
    assertThat(preference.getPhoneNumber()).isEqualTo("+1234567890");
    assertThat(preference.getBio()).isEqualTo("Test bio");
    assertThat(preference.getNotificationEmail()).isFalse();
    assertThat(preference.getNotificationSms()).isTrue();
    assertThat(preference.getNotificationPush()).isFalse();
    assertThat(preference.getTwoFactorEnabled()).isTrue();
    assertThat(preference.getTwoFactorMethod()).isEqualTo("sms");
    assertThat(preference.getCustomSettings()).isEqualTo("{\"key\":\"value\"}");
  }

  @Test
  @DisplayName("Should handle equals and hashCode correctly")
  void shouldHandleEqualsAndHashCode() {
    var user1 = new User("user1", "user1@example.com", "hash", "User", "One", "tenant-123");
    var user2 = new User("user2", "user2@example.com", "hash", "User", "Two", "tenant-123");

    var pref1 = new UserPreference(user1, "tenant-123");
    var pref2 = new UserPreference(user1, "tenant-123");
    var pref3 = new UserPreference(user2, "tenant-123");

    assertThat(pref1).isEqualTo(pref1);
    assertThat(pref1).isEqualTo(pref2);
    assertThat(pref1).isNotEqualTo(pref3);
    assertThat(pref1).isNotEqualTo(null);
    assertThat(pref1).isNotEqualTo(new Object());

    assertThat(pref1.hashCode()).isEqualTo(pref2.hashCode());
  }

  @Test
  @DisplayName("Should generate toString with all fields")
  void shouldGenerateToString() {
    var user = new User("testuser", "test@example.com", "hash", "Test", "User", "tenant-123");
    var preference = new UserPreference(user, "tenant-123");

    var toString = preference.toString();

    assertThat(toString).contains("UserPreference");
    assertThat(toString).contains("locale='en'");
    assertThat(toString).contains("timezone='UTC'");
    assertThat(toString).contains("theme='light'");
    assertThat(toString).contains("tenantId='tenant-123'");
  }
}
