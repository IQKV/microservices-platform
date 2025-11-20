package org.gripday.userservice.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

  @Test
  @DisplayName("findByName and existsByName should work as expected")
  void findByNameAndExists() {
    org.gripday.userservice.tenancy.TenantContext.setCurrentTenantId("t1");
    var o = new Organization("Acme", "t1");
    organizationRepository.save(o);

    assertThat(organizationRepository.findByName("Acme")).isPresent();
    assertThat(organizationRepository.existsByName("Acme")).isTrue();
    assertThat(organizationRepository.existsByName("Other")).isFalse();
  }

  @Test
  @DisplayName("findAll in tenant context returns only tenant's organizations")
  void findByTenant() {
    org.gripday.userservice.tenancy.TenantContext.setCurrentTenantId("TEN");
    organizationRepository.saveAll(List.of(
        new Organization("A1", "TEN"),
        new Organization("A2", "TEN")
    ));

    org.gripday.userservice.tenancy.TenantContext.setCurrentTenantId("OTHER");
    organizationRepository.save(new Organization("B1", "OTHER"));

    org.gripday.userservice.tenancy.TenantContext.setCurrentTenantId("TEN");
    var listAll = organizationRepository.findAll();
    var list = listAll.stream().filter(o -> "TEN".equals(o.getTenantId())).toList();
    assertThat(list).hasSize(2);
    assertThat(list).extracting(Organization::getTenantId).containsOnly("TEN");
  }

  @Test
  @DisplayName("findByEnabledTrue orders by createdAt desc and filters enabled in tenant context")
  void findEnabledByTenantOrdered() throws InterruptedException {
    org.gripday.userservice.tenancy.TenantContext.setCurrentTenantId("T");
    var a1 = new Organization("E1", "T");
    var a2 = new Organization("E2", "T");
    a1.setEnabled(true);
    a2.setEnabled(true);
    organizationRepository.save(a1);
    Thread.sleep(5); // ensure createdAt differs
    organizationRepository.save(a2);

    var disabled = new Organization("DIS", "T");
    disabled.setEnabled(false);
    organizationRepository.save(disabled);

    var enabled = organizationRepository.findEnabledOrderByCreatedAtDesc();
    assertThat(enabled).extracting(Organization::getEnabled).containsOnly(true);
    assertThat(enabled.get(0).getName()).isEqualTo("E2");
  }

  @Test
  @DisplayName("count and countByEnabledTrue return expected counts in tenant context")
  void counts() {
    var t = "C";
    org.gripday.userservice.tenancy.TenantContext.setCurrentTenantId(t);
    organizationRepository.saveAll(List.of(
        new Organization("C1", t),
        new Organization("C2", t),
        new Organization("C3", t)
    ));
    var dis = new Organization("C4", t);
    dis.setEnabled(false);
    organizationRepository.save(dis);

    assertThat(organizationRepository.count()).isEqualTo(4);
    assertThat(organizationRepository.countByEnabledTrue()).isEqualTo(3);
  }
}
