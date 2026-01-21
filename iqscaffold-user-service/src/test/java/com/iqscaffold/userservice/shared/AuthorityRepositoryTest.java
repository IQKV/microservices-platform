package com.iqscaffold.userservice.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.iqscaffold.userservice.usermanagement.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class AuthorityRepositoryTest {

  @Autowired
  private AuthorityRepository authorityRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("findByName, existsByName and findByNameIn should work")
  void basicFinders() {
    var a1 = new Authority("ADMIN");
    var a2 = new Authority("USER");
    authorityRepository.saveAll(List.of(a1, a2));

    assertThat(authorityRepository.findByName("ADMIN")).isPresent();
    assertThat(authorityRepository.existsByName("USER")).isTrue();

    var list = authorityRepository.findByNameIn(Set.of("ADMIN", "USER", "MISSING"));
    assertThat(list).extracting(Authority::getName).containsExactly("ADMIN", "USER");
  }

  @Test
  @DisplayName("findAllOrderByName returns all ordered by name")
  void orderedByName() {
    authorityRepository.saveAll(List.of(new Authority("ZETA"), new Authority("ALPHA"), new Authority("OMEGA")));
    var ordered = authorityRepository.findAllOrderByName();
    assertThat(ordered).extracting(Authority::getName).containsExactly("ALPHA", "OMEGA", "ZETA");
  }

  @Test
  @DisplayName("countUsersByAuthorityName and findByUserId should reflect assignments")
  void byUserRelations() {
    var admin = new Authority("ADMIN");
    var user = new Authority("USER");
    authorityRepository.saveAll(List.of(admin, user));

    var u1 = new User("u1", "u1@x.com", "hash", "F", "L", "t1");
    var u2 = new User("u2", "u2@x.com", "hash", "F", "L", "t1");

    // manage both sides
    u1.getAuthorities().add(admin);
    admin.getUsers().add(u1);

    u2.getAuthorities().add(user);
    user.getUsers().add(u2);

    // persist users so IDs are generated and join table rows are created
    entityManager.persist(u1);
    entityManager.persist(u2);
    entityManager.flush();

    var countAdmin = authorityRepository.countUsersByAuthorityName("ADMIN");
    assertThat(countAdmin).isEqualTo(1);

    var forU2 = authorityRepository.findByUserId(u2.getId());
    assertThat(forU2).extracting(Authority::getName).containsExactly("USER");

    // Add another authority with no users
    var orphan = new Authority("ORPHAN");
    authorityRepository.save(orphan);

    var empty = authorityRepository.findAuthoritiesWithNoUsers();
    assertThat(empty).extracting(Authority::getName).contains("ORPHAN");
  }
}
