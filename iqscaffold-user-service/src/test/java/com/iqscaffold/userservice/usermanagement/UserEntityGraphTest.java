package com.iqscaffold.userservice.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.iqscaffold.userservice.shared.Authority;

/**
 * Test class demonstrating entity graph functionality for User entity.
 *
 * <p>These tests verify that entity graphs properly load associations
 * without causing lazy loading exceptions or N+1 query problems.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserEntityGraphTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @Test
  void shouldLoadUserWithAuthoritiesUsingEntityGraph() {
    // Given: A user with authorities
    var authority = new Authority("TEST_ROLE", "Test Role");
    entityManager.persist(authority);

    var user = new User("testuser", "test@example.com", "hashedpassword",
        "Test", "User", "test-tenant");
    user.setAuthorities(Set.of(authority));
    entityManager.persistAndFlush(user);
    entityManager.clear(); // Clear persistence context

    // When: Finding user with authorities entity graph
    var foundUser = userRepository.findByUsernameWithAuthorities("testuser");

    // Then: User and authorities should be loaded
    assertThat(foundUser).isPresent();

    // Verify authorities are loaded without lazy loading exception
    assertDoesNotThrow(() -> {
      var authorities = foundUser.get().getAuthorities();
      assertThat(authorities).hasSize(1);
      assertThat(authorities.iterator().next().getName()).isEqualTo("TEST_ROLE");
    });
  }

  @Test
  void shouldLoadUserWithPreferencesUsingEntityGraph() {
    // Given: A user with preferences
    var user = new User("testuser2", "test2@example.com", "hashedpassword",
        "Test", "User2", "test-tenant");
    entityManager.persist(user);

    var preference = new UserPreference();
    preference.setUser(user);
    preference.setLocale("es");
    preference.setTimezone("Europe/Madrid");
    entityManager.persistAndFlush(preference);
    entityManager.clear(); // Clear persistence context

    // When: Finding user with preferences entity graph
    var foundUser = userRepository.findByIdWithPreferences(user.getId());

    // Then: User and preferences should be loaded
    assertThat(foundUser).isPresent();

    // Verify preferences are loaded without lazy loading exception
    assertDoesNotThrow(() -> {
      var userPreference = foundUser.get().getPreference();
      assertThat(userPreference).isNotNull();
      assertThat(userPreference.getLocale()).isEqualTo("es");
      assertThat(userPreference.getTimezone()).isEqualTo("Europe/Madrid");
    });
  }

  @Test
  void shouldLoadUserWithCompleteProfileUsingEntityGraph() {
    // Given: A user with both authorities and preferences
    var authority = new Authority("ADMIN", "Administrator");
    entityManager.persist(authority);

    var user = new User("adminuser", "admin@example.com", "hashedpassword",
        "Admin", "User", "test-tenant");
    user.setAuthorities(Set.of(authority));
    entityManager.persist(user);

    var preference = new UserPreference();
    preference.setUser(user);
    preference.setLocale("fr");
    preference.setCurrency("EUR");
    entityManager.persistAndFlush(preference);
    entityManager.clear(); // Clear persistence context

    // When: Finding user with complete profile entity graph
    var foundUser = userRepository.findByUsernameOrEmailWithComplete("adminuser", "adminuser");

    // Then: User, authorities, and preferences should all be loaded
    assertThat(foundUser).isPresent();

    // Verify all associations are loaded without lazy loading exceptions
    assertDoesNotThrow(() -> {
      var loadedUser = foundUser.get();

      // Check authorities
      var authorities = loadedUser.getAuthorities();
      assertThat(authorities).hasSize(1);
      assertThat(authorities.iterator().next().getName()).isEqualTo("ADMIN");

      // Check preferences
      var userPreference = loadedUser.getPreference();
      assertThat(userPreference).isNotNull();
      assertThat(userPreference.getLocale()).isEqualTo("fr");
      assertThat(userPreference.getCurrency()).isEqualTo("EUR");
    });
  }

  @Test
  void shouldFindUsersByRoleWithAuthoritiesLoaded() {
    // Given: Multiple users with the same role
    var userRole = new Authority("USER", "Regular User");
    entityManager.persist(userRole);

    var user1 = new User("user1", "user1@example.com", "hashedpassword",
        "User", "One", "test-tenant");
    user1.setAuthorities(Set.of(userRole));
    entityManager.persist(user1);

    var user2 = new User("user2", "user2@example.com", "hashedpassword",
        "User", "Two", "test-tenant");
    user2.setAuthorities(Set.of(userRole));
    entityManager.persistAndFlush(user2);
    entityManager.clear(); // Clear persistence context

    // When: Finding users by role with authorities loaded
    var users = userRepository.findByAuthorityNameWithAuthorities("USER");

    // Then: All users should be loaded with their authorities
    assertThat(users).hasSize(2);

    // Verify authorities are loaded for all users
    assertDoesNotThrow(() -> {
      users.forEach(user -> {
        var authorities = user.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getName()).isEqualTo("USER");
      });
    });
  }
}

