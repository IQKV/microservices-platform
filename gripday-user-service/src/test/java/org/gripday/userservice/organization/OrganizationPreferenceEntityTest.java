package org.gripday.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Entity tests for OrganizationPreference to achieve branch coverage.
 */
class OrganizationPreferenceEntityTest {

  @Test
  @DisplayName("Should create organization preference with defaults")
  void shouldCreateOrganizationPreferenceWithDefaults() {
    var org = new Organization("Test Org", "tenant-123");
    var preference = new OrganizationPreference(org, "tenant-123");

    assertThat(preference.getOrganization()).isEqualTo(org);
    assertThat(preference.getTenantId()).isEqualTo("tenant-123");
    assertThat(preference.getDefaultLocale()).isEqualTo("en");
    assertThat(preference.getDefaultTimezone()).isEqualTo("UTC");
    assertThat(preference.getDefaultCurrency()).isEqualTo("USD");
    assertThat(preference.getAllowUserRegistration()).isTrue();
    assertThat(preference.getRequireEmailVerification()).isTrue();
    assertThat(preference.getPasswordMinLength()).isEqualTo(8);
    assertThat(preference.getPasswordRequireUppercase()).isTrue();
    assertThat(preference.getPasswordRequireLowercase()).isTrue();
    assertThat(preference.getPasswordRequireNumbers()).isTrue();
    assertThat(preference.getPasswordRequireSpecialChars()).isTrue();
    assertThat(preference.getSessionTimeoutMinutes()).isEqualTo(30);
    assertThat(preference.getMaxLoginAttempts()).isEqualTo(5);
    assertThat(preference.getLockoutDurationMinutes()).isEqualTo(15);
    assertThat(preference.getEnableTwoFactorAuth()).isFalse();
    assertThat(preference.getRequireTwoFactorAuth()).isFalse();
  }

  @Test
  @DisplayName("Should set and get all properties")
  void shouldSetAndGetAllProperties() {
    var org = new Organization("Test Org", "tenant-123");
    var preference = new OrganizationPreference(org, "tenant-123");

    preference.setDefaultLocale("fr");
    preference.setDefaultTimezone("Europe/Paris");
    preference.setDefaultCurrency("EUR");
    preference.setDefaultDateFormat("dd/MM/yyyy");
    preference.setDefaultTimeFormat("HH:mm");
    preference.setAllowUserRegistration(false);
    preference.setRequireEmailVerification(false);
    preference.setPasswordMinLength(12);
    preference.setPasswordRequireUppercase(false);
    preference.setPasswordRequireLowercase(false);
    preference.setPasswordRequireNumbers(false);
    preference.setPasswordRequireSpecialChars(false);
    preference.setSessionTimeoutMinutes(60);
    preference.setMaxLoginAttempts(3);
    preference.setLockoutDurationMinutes(30);
    preference.setEnableTwoFactorAuth(true);
    preference.setRequireTwoFactorAuth(true);
    preference.setNotificationEmail("notify@example.com");
    preference.setSupportEmail("support@example.com");
    preference.setCustomSettings("{\"key\":\"value\"}");

    assertThat(preference.getDefaultLocale()).isEqualTo("fr");
    assertThat(preference.getDefaultTimezone()).isEqualTo("Europe/Paris");
    assertThat(preference.getDefaultCurrency()).isEqualTo("EUR");
    assertThat(preference.getDefaultDateFormat()).isEqualTo("dd/MM/yyyy");
    assertThat(preference.getDefaultTimeFormat()).isEqualTo("HH:mm");
    assertThat(preference.getAllowUserRegistration()).isFalse();
    assertThat(preference.getRequireEmailVerification()).isFalse();
    assertThat(preference.getPasswordMinLength()).isEqualTo(12);
    assertThat(preference.getPasswordRequireUppercase()).isFalse();
    assertThat(preference.getPasswordRequireLowercase()).isFalse();
    assertThat(preference.getPasswordRequireNumbers()).isFalse();
    assertThat(preference.getPasswordRequireSpecialChars()).isFalse();
    assertThat(preference.getSessionTimeoutMinutes()).isEqualTo(60);
    assertThat(preference.getMaxLoginAttempts()).isEqualTo(3);
    assertThat(preference.getLockoutDurationMinutes()).isEqualTo(30);
    assertThat(preference.getEnableTwoFactorAuth()).isTrue();
    assertThat(preference.getRequireTwoFactorAuth()).isTrue();
    assertThat(preference.getNotificationEmail()).isEqualTo("notify@example.com");
    assertThat(preference.getSupportEmail()).isEqualTo("support@example.com");
    assertThat(preference.getCustomSettings()).isEqualTo("{\"key\":\"value\"}");
  }

  @Test
  @DisplayName("Should handle equals and hashCode correctly")
  void shouldHandleEqualsAndHashCode() {
    var org1 = new Organization("Org 1", "tenant-123");
    var org2 = new Organization("Org 2", "tenant-123");
    
    var pref1 = new OrganizationPreference(org1, "tenant-123");
    var pref2 = new OrganizationPreference(org1, "tenant-123");
    var pref3 = new OrganizationPreference(org2, "tenant-123");

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
    var org = new Organization("Test Org", "tenant-123");
    var preference = new OrganizationPreference(org, "tenant-123");

    var toString = preference.toString();
    
    assertThat(toString).contains("OrganizationPreference");
    assertThat(toString).contains("defaultLocale='en'");
    assertThat(toString).contains("defaultTimezone='UTC'");
    assertThat(toString).contains("tenantId='tenant-123'");
  }
}
