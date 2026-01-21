package com.iqscaffold.userservice.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.iqscaffold.userservice.shared.Authority;

class UserEntityTest {

  @Test
  @DisplayName("addAuthority/removeAuthority maintain bidirectional relationship and hasAuthority reflects state")
  void authoritiesManagement() {
    var user = new User("john", "john@example.com", "hash", "John", "Doe", "TEN");
    var admin = new Authority("ADMIN");

    assertThat(user.hasAuthority("ADMIN")).isFalse();

    user.addAuthority(admin);
    assertThat(user.hasAuthority("ADMIN")).isTrue();
    assertThat(admin.getUsers()).contains(user);

    user.removeAuthority(admin);
    assertThat(user.hasAuthority("ADMIN")).isFalse();
    assertThat(admin.getUsers()).doesNotContain(user);
  }

  @Test
  @DisplayName("getFullName concatenates first and last name; isActive requires enabled and emailVerified true")
  void fullNameAndActive() {
    var user = new User("alice", "alice@example.com", "hash", "Alice", "Smith", "TEN");

    assertThat(user.getFullName()).isEqualTo("Alice Smith");

    // default enabled true, emailVerified false per entity defaults => not active
    assertThat(user.isActive()).isFalse();

    user.setEmailVerified(true);
    assertThat(user.isActive()).isTrue();

    user.setEnabled(false);
    assertThat(user.isActive()).isFalse();
  }
}
