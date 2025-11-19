package org.gripday.userservice.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.gripday.userservice.usermanagement.User;
import org.gripday.userservice.usermanagement.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class TenantRepositoryTest {

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("finders and exists checks: by tenantId, domain, subdomain")
  void basicFindersAndExists() {
    var t = new Tenant("TEN-1", "Tenant One", "desc", "creator");
    t.setDomain("example.com");
    t.setSubdomain("acme");
    tenantRepository.save(t);

    assertThat(tenantRepository.findByTenantId("TEN-1")).isPresent();
    assertThat(tenantRepository.findByDomain("example.com")).isPresent();
    assertThat(tenantRepository.findBySubdomain("acme")).isPresent();

    assertThat(tenantRepository.existsByTenantId("TEN-1")).isTrue();
    assertThat(tenantRepository.existsByDomain("example.com")).isTrue();
    assertThat(tenantRepository.existsBySubdomain("acme")).isTrue();
  }

  @Test
  @DisplayName("enabled filters, createdBy, and name contains queries work")
  void enabledAndCreatedByAndNameSearch() {
    var a = new Tenant("A", "Acme");
    a.setCreatedBy("sys");
    var b = new Tenant("B", "Beta");
    b.setCreatedBy("sys");
    var c = new Tenant("C", "Gamma");
    c.setCreatedBy("ops");
    c.setEnabled(false);
    tenantRepository.saveAll(List.of(a, b, c));

    assertThat(tenantRepository.findByEnabledTrue()).extracting(Tenant::getTenantId).contains("A", "B");
    assertThat(tenantRepository.findByEnabledFalse()).extracting(Tenant::getTenantId).contains("C");

    assertThat(tenantRepository.countByEnabledTrue()).isEqualTo(2);
    assertThat(tenantRepository.countByEnabledFalse()).isEqualTo(1);

    assertThat(tenantRepository.findByCreatedBy("sys")).extracting(Tenant::getTenantId).containsExactlyInAnyOrder("A", "B");

    assertThat(tenantRepository.findByNameContainingIgnoreCase("a")).extracting(Tenant::getName).contains("Acme", "Gamma");
  }

  @Test
  @DisplayName("findTenantsWithUserCountBetween returns enabled tenants with user counts in range")
  void tenantsWithUserCountBetween() {
    tenantRepository.save(new Tenant("T1", "T1"));
    tenantRepository.save(new Tenant("T2", "T2"));

    // Users: T1 has 1 enabled, T2 has 2 enabled, and one disabled should not count
    userRepository.save(new User("u1", "u1@x.com", "h", "F", "L", "T1"));

    var u21 = new User("u21", "u21@x.com", "h", "F", "L", "T2");
    u21.setEnabled(true);
    userRepository.save(u21);
    var u22 = new User("u22", "u22@x.com", "h", "F", "L", "T2");
    u22.setEnabled(true);
    userRepository.save(u22);
    var u2d = new User("u2d", "u2d@x.com", "h", "F", "L", "T2");
    u2d.setEnabled(false);
    userRepository.save(u2d);

    var between1and2 = tenantRepository.findTenantsWithUserCountBetween(1, 2);
    assertThat(between1and2).extracting(Tenant::getTenantId).contains("T1", "T2");

    var between2and2 = tenantRepository.findTenantsWithUserCountBetween(2, 2);
    assertThat(between2and2).extracting(Tenant::getTenantId).containsExactly("T2");
  }

  @Test
  @DisplayName("findTenantsExceedingUserQuota returns tenants where enabled user count > maxUsers")
  void tenantsExceedingQuota() {
    var t = new Tenant("TQ", "Quota");
    t.setMaxUsers(1);
    tenantRepository.save(t);

    userRepository.save(new User("q1", "q1@x.com", "h", "F", "L", "TQ"));
    userRepository.save(new User("q2", "q2@x.com", "h", "F", "L", "TQ"));

    var exceeding = tenantRepository.findTenantsExceedingUserQuota();
    assertThat(exceeding).extracting(Tenant::getTenantId).contains("TQ");
  }

  @Test
  @DisplayName("getTenantStatistics returns aggregated rows per tenant")
  void tenantStatistics() {
    var t = new Tenant("TS", "Stats");
    t.setMaxUsers(10);
    tenantRepository.save(t);
    userRepository.save(new User("s1", "s1@x.com", "h", "F", "L", "TS"));

    var stats = tenantRepository.getTenantStatistics();
    assertThat(stats).isNotEmpty();
    var row = stats.get(0);
    assertThat(row).isInstanceOf(Object[].class);
    var arr = (Object[]) row;
    assertThat(arr).hasSize(6);
    assertThat(arr[0]).isInstanceOf(String.class); // tenantId
    assertThat(arr[1]).isInstanceOf(String.class); // name
  }
}
