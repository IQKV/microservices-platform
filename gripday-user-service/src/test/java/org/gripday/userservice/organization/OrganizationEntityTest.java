package org.gripday.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrganizationEntityTest {

  @Test
  @DisplayName("default enabled is true and tenantId is set via base class")
  void defaultsAndTenant() {
    var org = new Organization("Acme Corp", "tenant-1");
    assertThat(org.getEnabled()).isTrue();
    assertThat(org.getTenantId()).isEqualTo("tenant-1");

    org.setEnabled(false);
    assertThat(org.getEnabled()).isFalse();
  }

  @Test
  @DisplayName("setters update basic fields")
  void setters() {
    var org = new Organization("Name", "t1");
    org.setDescription("Desc");
    org.setIndustry("IT");
    org.setWebsite("https://example.com");
    org.setPhone("+100");
    org.setAddress("Street 1");
    org.setCity("City");
    org.setCountry("Country");

    assertThat(org.getDescription()).isEqualTo("Desc");
    assertThat(org.getIndustry()).isEqualTo("IT");
    assertThat(org.getWebsite()).isEqualTo("https://example.com");
    assertThat(org.getPhone()).isEqualTo("+100");
    assertThat(org.getAddress()).isEqualTo("Street 1");
    assertThat(org.getCity()).isEqualTo("City");
    assertThat(org.getCountry()).isEqualTo("Country");
  }
}
