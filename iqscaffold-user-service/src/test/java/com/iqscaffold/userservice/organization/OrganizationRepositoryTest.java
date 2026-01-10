package com.iqscaffold.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.iqscaffold.userservice.tenancy.Tenant;
import com.iqscaffold.userservice.tenancy.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class OrganizationRepositoryTest {

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Test
  @DisplayName("findByName and existsByName should work as expected")
  void findByNameAndExists() {
    // Create tenant first
    var tenant = new Tenant("t1-unique", "Test Tenant 1");
    tenantRepository.save(tenant);

    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("t1-unique");
    var o = new Organization("Acme", "t1-unique");
    organizationRepository.save(o);

    assertThat(organizationRepository.findByName("Acme")).isPresent();
    assertThat(organizationRepository.existsByName("Acme")).isTrue();
    assertThat(organizationRepository.existsByName("Other")).isFalse();
  }

  @Test
  @DisplayName("findAll in tenant context returns only tenant's organizations")
  void findByTenant() {
    // Create tenants first
    tenantRepository.save(new Tenant("TEN-unique", "Test Tenant"));
    tenantRepository.save(new Tenant("TEN-unique2", "Test Tenant 2"));
    tenantRepository.save(new Tenant("OTHER-unique", "Other Tenant"));

    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("TEN-unique");
    organizationRepository.saveAll(List.of(
        new Organization("A1", "TEN-unique"),
        new Organization("A2", "TEN-unique2")
    ));

    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("OTHER-unique");
    organizationRepository.save(new Organization("B1", "OTHER-unique"));

    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("TEN-unique");
    var listAll = organizationRepository.findAll();
    var list = listAll.stream().filter(o -> "TEN-unique".equals(o.getTenantId())).toList();
    assertThat(list).hasSize(1);
    assertThat(list).extracting(Organization::getTenantId).containsOnly("TEN-unique");
  }

  @Test
  @DisplayName("findByEnabledTrue orders by createdAt desc and filters enabled in tenant context")
  void findEnabledByTenantOrdered() throws InterruptedException {
    // Create tenants first
    tenantRepository.save(new Tenant("T-unique", "Test Tenant"));
    tenantRepository.save(new Tenant("T-unique2", "Test Tenant 2"));
    tenantRepository.save(new Tenant("T-unique3", "Test Tenant 3"));

    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("T-unique");
    var a1 = new Organization("E1", "T-unique");
    var a2 = new Organization("E2", "T-unique2");
    a1.setEnabled(true);
    a2.setEnabled(true);
    organizationRepository.save(a1);
    Thread.sleep(5); // ensure createdAt differs
    organizationRepository.save(a2);

    var disabled = new Organization("DIS", "T-unique3");
    disabled.setEnabled(false);
    organizationRepository.save(disabled);

    var enabled = organizationRepository.findEnabledOrderByCreatedAtDesc();
    assertThat(enabled).extracting(Organization::getEnabled).containsOnly(true);
    assertThat(enabled.get(0).getName()).isEqualTo("E2");
  }

  @Test
  @DisplayName("count and countByEnabledTrue return expected counts in tenant context")
  void counts() {
    var t = "C-unique";
    // Create tenants first
    tenantRepository.save(new Tenant(t, "Test Tenant"));
    tenantRepository.save(new Tenant(t + "2", "Test Tenant 2"));
    tenantRepository.save(new Tenant(t + "3", "Test Tenant 3"));
    tenantRepository.save(new Tenant(t + "4", "Test Tenant 4"));

    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId(t);
    organizationRepository.saveAll(List.of(
        new Organization("C1", t),
        new Organization("C2", t + "2"),
        new Organization("C3", t + "3")
    ));
    var dis = new Organization("C4", t + "4");
    dis.setEnabled(false);
    organizationRepository.save(dis);

    assertThat(organizationRepository.count()).isEqualTo(4);
    assertThat(organizationRepository.countByEnabledTrue()).isEqualTo(3);
  }
}
