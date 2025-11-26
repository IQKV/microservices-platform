package com.iqscaffold.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserContext Tests")
class UserContextTest {

  @Test
  @DisplayName("Should create UserContext with all fields")
  void shouldCreateUserContextWithAllFields() {
    // Arrange & Act
    var context = new UserContext(
        1L,
        "john.doe",
        "john@example.com",
        Set.of("USER", "ADMIN"),
        Set.of("READ", "WRITE"),
        "Engineering",
        "org-123",
        Map.of("custom", "value")
    );

    // Assert
    assertThat(context.userId()).isEqualTo(1L);
    assertThat(context.username()).isEqualTo("john.doe");
    assertThat(context.email()).isEqualTo("john@example.com");
    assertThat(context.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    assertThat(context.permissions()).containsExactlyInAnyOrder("READ", "WRITE");
    assertThat(context.department()).isEqualTo("Engineering");
    assertThat(context.organizationId()).isEqualTo("org-123");
    assertThat(context.customClaims()).containsEntry("custom", "value");
  }

  @Test
  @DisplayName("Should check if user has specific role")
  void shouldCheckIfUserHasSpecificRole() {
    // Arrange
    var context = new UserContext(
        1L, "john.doe", "john@example.com",
        Set.of("USER", "ADMIN"), null, null, null, null
    );

    // Act & Assert
    assertThat(context.hasRole("USER")).isTrue();
    assertThat(context.hasRole("ADMIN")).isTrue();
    assertThat(context.hasRole("SUPERADMIN")).isFalse();
  }

  @Test
  @DisplayName("Should return false when checking role with null roles")
  void shouldReturnFalseWhenCheckingRoleWithNullRoles() {
    // Arrange
    var context = new UserContext(
        1L, "john.doe", "john@example.com",
        null, null, null, null, null
    );

    // Act & Assert
    assertThat(context.hasRole("USER")).isFalse();
  }

  @Test
  @DisplayName("Should check if user has any of the specified roles")
  void shouldCheckIfUserHasAnyOfSpecifiedRoles() {
    // Arrange
    var context = new UserContext(
        1L, "john.doe", "john@example.com",
        Set.of("USER"), null, null, null, null
    );

    // Act & Assert
    assertThat(context.hasAnyRole("USER", "ADMIN")).isTrue();
    assertThat(context.hasAnyRole("ADMIN", "SUPERADMIN")).isFalse();
  }

  @Test
  @DisplayName("Should return false when checking any role with null roles")
  void shouldReturnFalseWhenCheckingAnyRoleWithNullRoles() {
    // Arrange
    var context = new UserContext(
        1L, "john.doe", "john@example.com",
        null, null, null, null, null
    );

    // Act & Assert
    assertThat(context.hasAnyRole("USER", "ADMIN")).isFalse();
  }

  @Test
  @DisplayName("Should identify admin user")
  void shouldIdentifyAdminUser() {
    // Arrange
    var adminContext = new UserContext(
        1L, "admin", "admin@example.com",
        Set.of("ADMIN"), null, null, null, null
    );
    var superAdminContext = new UserContext(
        2L, "superadmin", "superadmin@example.com",
        Set.of("SUPERADMIN"), null, null, null, null
    );
    var userContext = new UserContext(
        3L, "user", "user@example.com",
        Set.of("USER"), null, null, null, null
    );

    // Act & Assert
    assertThat(adminContext.isAdmin()).isTrue();
    assertThat(superAdminContext.isAdmin()).isTrue();
    assertThat(userContext.isAdmin()).isFalse();
  }

  @Test
  @DisplayName("Should identify super admin user")
  void shouldIdentifySuperAdminUser() {
    // Arrange
    var superAdminContext = new UserContext(
        1L, "superadmin", "superadmin@example.com",
        Set.of("SUPERADMIN"), null, null, null, null
    );
    var adminContext = new UserContext(
        2L, "admin", "admin@example.com",
        Set.of("ADMIN"), null, null, null, null
    );

    // Act & Assert
    assertThat(superAdminContext.isSuperAdmin()).isTrue();
    assertThat(adminContext.isSuperAdmin()).isFalse();
  }

  @Test
  @DisplayName("Should handle empty roles set")
  void shouldHandleEmptyRolesSet() {
    // Arrange
    var context = new UserContext(
        1L, "john.doe", "john@example.com",
        Set.of(), null, null, null, null
    );

    // Act & Assert
    assertThat(context.hasRole("USER")).isFalse();
    assertThat(context.hasAnyRole("USER", "ADMIN")).isFalse();
    assertThat(context.isAdmin()).isFalse();
    assertThat(context.isSuperAdmin()).isFalse();
  }
}
