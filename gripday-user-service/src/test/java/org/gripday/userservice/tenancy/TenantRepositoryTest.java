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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import({TenantRepositoryTest.TestConfig.class, TenantManagementService.class})
class TenantRepositoryTest {

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TenantManagementService tenantManagementService;

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

    TenantContext.setCurrentTenantId("T1");
    userRepository.save(new User("u1", "u1@x.com", "h", "F", "L", "T1"));

    TenantContext.setCurrentTenantId("T2");
    var u21 = new User("u21", "u21@x.com", "h", "F", "L", "T2");
    u21.setEnabled(true);
    userRepository.save(u21);
    var u22 = new User("u22", "u22@x.com", "h", "F", "L", "T2");
    u22.setEnabled(true);
    userRepository.save(u22);
    var u2d = new User("u2d", "u2d@x.com", "h", "F", "L", "T2");
    u2d.setEnabled(false);
    userRepository.save(u2d);

    TenantContext.clear();
    var stats = tenantManagementService.getTenantStatistics();
    var s1 = stats.stream().filter(s -> s.tenantId().equals("T1")).findFirst().orElseThrow();
    var s2 = stats.stream().filter(s -> s.tenantId().equals("T2")).findFirst().orElseThrow();
    assertThat(s1.userCount()).isEqualTo(1L);
    assertThat(s2.userCount()).isEqualTo(2L);
  }

  @Test
  @DisplayName("findTenantsExceedingUserQuota returns tenants where enabled user count > maxUsers")
  void tenantsExceedingQuota() {
    var t = new Tenant("TQ", "Quota");
    t.setMaxUsers(1);
    tenantRepository.save(t);

    TenantContext.setCurrentTenantId("TQ");
    userRepository.save(new User("q1", "q1@x.com", "h", "F", "L", "TQ"));
    userRepository.save(new User("q2", "q2@x.com", "h", "F", "L", "TQ"));

    var exceeding = tenantManagementService.findTenantsExceedingUserQuota();
    assertThat(exceeding).extracting(Tenant::getTenantId).contains("TQ");
  }

  @Test
  @DisplayName("TenantManagementService.getTenantStatistics returns stats with per-tenant counts")
  void tenantStatistics() {
    var t = new Tenant("TS", "Stats");
    t.setMaxUsers(10);
    tenantRepository.save(t);
    TenantContext.setCurrentTenantId("TS");
    userRepository.save(new User("s1", "s1@x.com", "h", "F", "L", "TS"));

    var stats = tenantManagementService.getTenantStatistics();
    assertThat(stats).isNotEmpty();
    var s = stats.stream().filter(x -> x.tenantId().equals("TS")).findFirst().orElseThrow();
    assertThat(s.userCount()).isEqualTo(1L);
    assertThat(s.maxUsers()).isEqualTo(10);
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    org.gripday.userservice.tenancy.SchemaNameResolver schemaNameResolver() {
      return new org.gripday.userservice.tenancy.SchemaNameResolver("tenant_");
    }

    @Bean
    org.gripday.userservice.tenancy.TenantLiquibaseRunner liquibaseRunner() {
      return org.mockito.Mockito.mock(org.gripday.userservice.tenancy.TenantLiquibaseRunner.class);
    }

    @Bean
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate(javax.sql.DataSource dataSource) {
      return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    }
  }
}
