package com.iqscaffold.billingservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for UserContext record.
 */
@DisplayName("UserContext Tests")
class UserContextTest {

  @Test
  @DisplayName("Should create UserContext with all fields")
  void shouldCreateUserContextWithAllFields() {
    // Given
    Long userId = 123L;
    String username = "testuser";
    String email = "test@example.com";
    Set<String> authorities = Set.of("USER", "ADMIN");
    String tenantId = "tenant-123";
    String firstName = "John";
    String lastName = "Doe";

    // When
    UserContext userContext = new UserContext(
        userId, username, email, authorities, tenantId, firstName, lastName
    );

    // Then
    assertThat(userContext.userId()).isEqualTo(userId);
    assertThat(userContext.username()).isEqualTo(username);
    assertThat(userContext.email()).isEqualTo(email);
    assertThat(userContext.authorities()).containsExactlyInAnyOrder("USER", "ADMIN");
    assertThat(userContext.tenantId()).isEqualTo(tenantId);
    assertThat(userContext.firstName()).isEqualTo(firstName);
    assertThat(userContext.lastName()).isEqualTo(lastName);
  }

  @Test
  @DisplayName("Should check if user has specific authority")
  void shouldCheckIfUserHasAuthority() {
    // Given
    UserContext userContext = new UserContext(
        1L, "user", "user@example.com", Set.of("USER", "BILLING_ADMIN"),
        "tenant-1", "John", "Doe"
    );

    // When & Then
    assertThat(userContext.hasAuthority("USER")).isTrue();
    assertThat(userContext.hasAuthority("BILLING_ADMIN")).isTrue();
    assertThat(userContext.hasAuthority("ADMIN")).isFalse();
    assertThat(userContext.hasAuthority("SUPER_ADMIN")).isFalse();
  }

  @Test
  @DisplayName("Should identify admin users correctly")
  void shouldIdentifyAdminUsers() {
    // Given
    UserContext adminUser = new UserContext(
        1L, "admin", "admin@example.com", Set.of("ADMIN"),
        "tenant-1", "Admin", "User"
    );

    UserContext superAdminUser = new UserContext(
        2L, "superadmin", "superadmin@example.com", Set.of("SUPER_ADMIN"),
        "tenant-1", "Super", "Admin"
    );

    UserContext regularUser = new UserContext(
        3L, "user", "user@example.com", Set.of("USER"),
        "tenant-1", "Regular", "User"
    );

    // When & Then
    assertThat(adminUser.isAdmin()).isTrue();
    assertThat(superAdminUser.isAdmin()).isTrue();
    assertThat(regularUser.isAdmin()).isFalse();
  }

  @Test
  @DisplayName("Should return full name when first and last names are present")
  void shouldReturnFullName() {
    // Given
    UserContext userContext = new UserContext(
        1L, "user", "user@example.com", Set.of("USER"),
        "tenant-1", "John", "Doe"
    );

    // When
    String fullName = userContext.getFullName();

    // Then
    assertThat(fullName).isEqualTo("John Doe");
  }

  @Test
  @DisplayName("Should return first name only when last name is null")
  void shouldReturnFirstNameOnly() {
    // Given
    UserContext userContext = new UserContext(
        1L, "user", "user@example.com", Set.of("USER"),
        "tenant-1", "John", null
    );

    // When
    String fullName = userContext.getFullName();

    // Then
    assertThat(fullName).isEqualTo("John");
  }

  @Test
  @DisplayName("Should return last name only when first name is null")
  void shouldReturnLastNameOnly() {
    // Given
    UserContext userContext = new UserContext(
        1L, "user", "user@example.com", Set.of("USER"),
        "tenant-1", null, "Doe"
    );

    // When
    String fullName = userContext.getFullName();

    // Then
    assertThat(fullName).isEqualTo("Doe");
  }

  @Test
  @DisplayName("Should return username when both names are null")
  void shouldReturnUsernameWhenNamesAreNull() {
    // Given
    UserContext userContext = new UserContext(
        1L, "testuser", "user@example.com", Set.of("USER"),
        "tenant-1", null, null
    );

    // When
    String fullName = userContext.getFullName();

    // Then
    assertThat(fullName).isEqualTo("testuser");
  }

  @Test
  @DisplayName("Should handle empty authorities set")
  void shouldHandleEmptyAuthorities() {
    // Given
    UserContext userContext = new UserContext(
        1L, "user", "user@example.com", Set.of(),
        "tenant-1", "John", "Doe"
    );

    // When & Then
    assertThat(userContext.hasAuthority("USER")).isFalse();
    assertThat(userContext.isAdmin()).isFalse();
  }

  @Test
  @DisplayName("Should handle null authorities set")
  void shouldHandleNullAuthorities() {
    // Given
    UserContext userContext = new UserContext(
        1L, "user", "user@example.com", null,
        "tenant-1", "John", "Doe"
    );

    // When & Then
    assertThat(userContext.hasAuthority("USER")).isFalse();
    assertThat(userContext.isAdmin()).isFalse();
  }
}
