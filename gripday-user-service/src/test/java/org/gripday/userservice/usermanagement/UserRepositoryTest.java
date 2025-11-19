package org.gripday.userservice.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.gripday.userservice.shared.Authority;
import org.gripday.userservice.shared.AuthorityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AuthorityRepository authorityRepository;

  @Test
  @DisplayName("findByUsernameOrEmail should find by either username or email")
  void findByUsernameOrEmail() {
    persistUser("tenantA", "john", "john@example.com", true, true, Set.of("ADMIN", "USER"));

    assertThat(userRepository.findByUsernameOrEmail("john", "nope@example.com")).isPresent();
    assertThat(userRepository.findByUsernameOrEmail("nouser", "john@example.com")).isPresent();
    assertThat(userRepository.findByUsernameOrEmail("nouser", "nope@example.com")).isEmpty();
  }

  @Test
  @DisplayName("existsByUsername and existsByEmail should reflect persistence state")
  void existsChecks() {
    persistUser("tenantA", "alice", "alice@example.com", true, true, Set.of("USER"));

    assertThat(userRepository.existsByUsername("alice")).isTrue();
    assertThat(userRepository.existsByUsername("bob")).isFalse();

    assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
    assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
  }

  @Test
  @DisplayName("findEnabledUsersByTenantId should return only enabled users in the tenant ordered by createdAt desc")
  void findEnabledUsersByTenant() {
    persistUser("tenantT", "u1", "u1@x.com", true, true, Set.of("USER"));
    persistUser("tenantT", "u2", "u2@x.com", false, true, Set.of("USER"));
    var u3 = persistUser("tenantT", "u3", "u3@x.com", true, true, Set.of("USER"));
    persistUser("tenantOther", "u4", "u4@x.com", true, true, Set.of("USER"));

    var enabled = userRepository.findEnabledUsersByTenantId("tenantT");

    assertThat(enabled).extracting(User::getEnabled).containsOnly(true);
    assertThat(enabled).extracting(User::getTenantId).containsOnly("tenantT");
    assertThat(enabled).hasSize(2);
    // createdAt is generated, but order should be desc, so last persisted should come first
    assertThat(enabled.get(0).getUsername()).isEqualTo(u3.getUsername());
  }

  @Test
  @DisplayName("countByTenantId and countByTenantIdAndEnabledTrue should count correctly")
  void countsByTenant() {
    persistUser("t1", "a1", "a1@x.com", true, true, Set.of("USER"));
    persistUser("t1", "a2", "a2@x.com", false, true, Set.of("USER"));
    persistUser("t1", "a3", "a3@x.com", true, true, Set.of("USER"));

    assertThat(userRepository.countByTenantId("t1")).isEqualTo(3);
    assertThat(userRepository.countByTenantIdAndEnabledTrue("t1")).isEqualTo(2);
  }

  @Test
  @DisplayName("findByTenantIdAndAuthorityName should filter by tenant and authority")
  void findByTenantAndAuthority() {
    var admin = getOrCreateAuthorities(Set.of("ADMIN")).get(0);
    var userRole = getOrCreateAuthorities(Set.of("USER")).get(0);

    var u1 = baseUser("tA", "x1", "x1@x.com", true, true);
    u1.getAuthorities().add(admin);
    admin.getUsers().add(u1);

    var u2 = baseUser("tA", "x2", "x2@x.com", true, true);
    u2.getAuthorities().add(userRole);
    userRole.getUsers().add(u2);

    var u3 = baseUser("tB", "x3", "x3@x.com", true, true);
    u3.getAuthorities().add(admin);
    admin.getUsers().add(u3);

    authorityRepository.saveAll(List.of(admin, userRole));
    userRepository.saveAll(List.of(u1, u2, u3));

    var result = userRepository.findByTenantIdAndAuthorityName("tA", "ADMIN");
    assertThat(result).extracting(User::getUsername).containsExactly("x1");
  }

  @Test
  @DisplayName("findUnverifiedUsersByTenantId should return enabled but unverified users")
  void findUnverifiedUsers() {
    persistUser("tZ", "v1", "v1@x.com", true, false, Set.of("USER"));
    persistUser("tZ", "v2", "v2@x.com", true, true, Set.of("USER"));

    var res = userRepository.findUnverifiedUsersByTenantId("tZ");
    assertThat(res).extracting(User::getEmailVerified).containsOnly(false);
  }

  private User persistUser(String tenant, String username, String email, boolean enabled, boolean emailVerified, Set<String> authorities) {
    var u = baseUser(tenant, username, email, enabled, emailVerified);
    var auths = getOrCreateAuthorities(authorities);
    u.getAuthorities().addAll(auths);
    auths.forEach(a -> a.getUsers().add(u));
    authorityRepository.saveAll(auths);
    return userRepository.save(u);
  }

  private User baseUser(String tenant, String username, String email, boolean enabled, boolean emailVerified) {
    var u = new User(username, email, "hash", "First", "Last", tenant);
    u.setEnabled(enabled);
    u.setEmailVerified(emailVerified);
    return u;
  }

  private List<Authority> getOrCreateAuthorities(Set<String> names) {
    return names.stream()
        .map(n -> authorityRepository.findByName(n).orElseGet(() -> new Authority(n)))
        .toList();
  }
}
