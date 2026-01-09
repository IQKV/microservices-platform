package com.iqscaffold.userservice.organization;

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
    var preference = new OrganizationPreference(org);

    assertThat(preference.getOrganization()).isEqualTo(org);
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
    assertThat(preference.getTwoFactorAuthRequired()).isFalse();
  }

  @Test
  @DisplayName("Should set and get all properties")
  void shouldSetAndGetAllProperties() {
    var org = new Organization("Test Org", "tenant-123");
    var preference = new OrganizationPreference(org);

    preference.setDefaultLocale("fr");
    preference.setDefaultTimezone("Europe/Paris");
    preference.setDefaultCurrency("EUR");
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
    preference.setTwoFactorAuthRequired(true);
    preference.setNotificationEmail("notify@example.com");

    assertThat(preference.getDefaultLocale()).isEqualTo("fr");
    assertThat(preference.getDefaultTimezone()).isEqualTo("Europe/Paris");
    assertThat(preference.getDefaultCurrency()).isEqualTo("EUR");
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
    assertThat(preference.getTwoFactorAuthRequired()).isTrue();
    assertThat(preference.getNotificationEmail()).isEqualTo("notify@example.com");
  }

  @Test
  @DisplayName("Should handle equals and hashCode correctly")
  void shouldHandleEqualsAndHashCode() {
    var org1 = new Organization("Org 1", "tenant-123");
    var org2 = new Organization("Org 2", "tenant-456");

    var pref1 = new OrganizationPreference(org1);
    var pref2 = new OrganizationPreference(org1);
    var pref3 = new OrganizationPreference(org2);

    // Same reference
    assertThat(pref1).isEqualTo(pref1);
    
    // Different instances with null ids are not equal (transient entities)
    assertThat(pref1).isNotEqualTo(pref2);
    assertThat(pref1).isNotEqualTo(pref3);
    assertThat(pref1).isNotEqualTo(null);
    assertThat(pref1).isNotEqualTo(new Object());

    // HashCode should be consistent (class-based)
    assertThat(pref1.hashCode()).isEqualTo(pref2.hashCode());
    assertThat(pref1.hashCode()).isEqualTo(pref3.hashCode());
  }

  @Test
  @DisplayName("Should generate toString with all fields")
  void shouldGenerateToString() {
    var org = new Organization("Test Org", "tenant-123");
    var preference = new OrganizationPreference(org);

    var toString = preference.toString();

    assertThat(toString).contains("OrganizationPreference");
    assertThat(toString).contains("defaultLocale='en'");
    assertThat(toString).contains("defaultTimezone='UTC'");
  }
}
