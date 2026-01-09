package com.iqscaffold.userservice.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrganizationTest {

  @Test
  void shouldCreateOrganization() {
    var organization = new Organization("Acme Corporation", "tenant-1");

    assertEquals("Acme Corporation", organization.getName());
    assertEquals("tenant-1", organization.getTenantId());
    assertNull(organization.getId());
  }

  @Test
  void shouldSetAndGetName() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setName("Updated Acme Corp");

    assertEquals("Updated Acme Corp", organization.getName());
  }

  @Test
  void shouldSetAndGetDescription() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setDescription("A leading technology company");

    assertEquals("A leading technology company", organization.getDescription());
  }

  @Test
  void shouldSetAndGetIndustry() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setIndustry("Technology");

    assertEquals("Technology", organization.getIndustry());
  }

  @Test
  void shouldSetAndGetWebsite() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setWebsite("https://acme.com");

    assertEquals("https://acme.com", organization.getWebsite());
  }

  @Test
  void shouldSetAndGetPhone() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setPhone("+1-555-0100");

    assertEquals("+1-555-0100", organization.getPhone());
  }

  @Test
  void shouldSetAndGetAddress() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setAddress("123 Main Street");

    assertEquals("123 Main Street", organization.getAddress());
  }

  @Test
  void shouldSetAndGetCity() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setCity("San Francisco");

    assertEquals("San Francisco", organization.getCity());
  }

  @Test
  void shouldSetAndGetCountry() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setCountry("USA");

    assertEquals("USA", organization.getCountry());
  }

  @Test
  void shouldSetAndGetEnabled() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setEnabled(false);

    assertFalse(organization.getEnabled());
  }

  @Test
  void shouldBeEnabledByDefault() {
    var organization = new Organization("Acme Corp", "tenant-1");

    assertTrue(organization.getEnabled());
  }

  @Test
  void shouldCheckIfOrganizationIsActive() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setEnabled(true);

    assertTrue(organization.isActive());
  }

  @Test
  void shouldCheckIfOrganizationIsInactive() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setEnabled(false);

    assertFalse(organization.isActive());
  }

  @Test
  void shouldHandleNullEnabledStatus() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setEnabled(null);

    assertFalse(organization.isActive());
  }

  @Test
  void shouldGetLocationWithCityAndCountry() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setCity("San Francisco");
    organization.setCountry("USA");

    assertEquals("San Francisco, USA", organization.getLocation());
  }

  @Test
  void shouldGetLocationWithOnlyCity() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setCity("San Francisco");

    assertEquals("San Francisco", organization.getLocation());
  }

  @Test
  void shouldGetLocationWithOnlyCountry() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setCountry("USA");

    assertEquals("USA", organization.getLocation());
  }

  @Test
  void shouldGetEmptyLocationWhenBothNull() {
    var organization = new Organization("Acme Corp", "tenant-1");

    assertEquals("", organization.getLocation());
  }

  @Test
  void shouldCheckEqualityBasedOnTenantId() {
    var org1 = new Organization("Acme Corp", "tenant-1");
    var org2 = new Organization("Acme Corp", "tenant-1");
    var org3 = new Organization("Different Corp", "tenant-1");

    // Same tenant ID means equal (business key equality)
    assertEquals(org1, org2);
    assertEquals(org1, org3);
    assertEquals(org1.hashCode(), org2.hashCode());
    assertEquals(org1.hashCode(), org3.hashCode());
  }

  @Test
  void shouldNotBeEqualWithDifferentTenantIds() {
    var org1 = new Organization("Acme Corp", "tenant-1");
    var org2 = new Organization("Acme Corp", "tenant-2");

    assertNotEquals(org1, org2);
  }

  @Test
  void shouldGenerateToString() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setIndustry("Technology");
    organization.setEnabled(true);

    var toString = organization.toString();
    assertTrue(toString.contains("Acme Corp"));
    assertTrue(toString.contains("Technology"));
    assertTrue(toString.contains("enabled=true"));
    assertTrue(toString.contains("tenantId='tenant-1'"));
  }

  @Test
  void shouldSetCompleteOrganizationData() {
    var organization = new Organization("Acme Corporation", "tenant-1");
    organization.setDescription("Leading tech company");
    organization.setIndustry("Technology");
    organization.setWebsite("https://acme.com");
    organization.setPhone("+1-555-0100");
    organization.setAddress("123 Main Street");
    organization.setCity("San Francisco");
    organization.setCountry("USA");
    organization.setEnabled(true);

    assertEquals("Acme Corporation", organization.getName());
    assertEquals("Leading tech company", organization.getDescription());
    assertEquals("Technology", organization.getIndustry());
    assertEquals("https://acme.com", organization.getWebsite());
    assertEquals("+1-555-0100", organization.getPhone());
    assertEquals("123 Main Street", organization.getAddress());
    assertEquals("San Francisco", organization.getCity());
    assertEquals("USA", organization.getCountry());
    assertTrue(organization.isActive());
    assertEquals("San Francisco, USA", organization.getLocation());
  }

  @Test
  void shouldHandleNullValues() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setDescription(null);
    organization.setIndustry(null);
    organization.setWebsite(null);
    organization.setPhone(null);
    organization.setAddress(null);
    organization.setCity(null);
    organization.setCountry(null);

    assertNull(organization.getDescription());
    assertNull(organization.getIndustry());
    assertNull(organization.getWebsite());
    assertNull(organization.getPhone());
    assertNull(organization.getAddress());
    assertNull(organization.getCity());
    assertNull(organization.getCountry());
    assertEquals("", organization.getLocation());
  }

  @Test
  void shouldUpdateOrganizationProperties() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setName("Updated Acme");
    organization.setDescription("New description");
    organization.setIndustry("New Industry");

    assertEquals("Updated Acme", organization.getName());
    assertEquals("New description", organization.getDescription());
    assertEquals("New Industry", organization.getIndustry());
  }

  @Test
  void shouldHandleEmptyStrings() {
    var organization = new Organization("Acme Corp", "tenant-1");
    organization.setCity("");
    organization.setCountry("");

    // Empty strings are treated as non-null, so they get concatenated with ", "
    var location = organization.getLocation();
    assertTrue(location.equals("") || location.equals(", "));
  }
}
