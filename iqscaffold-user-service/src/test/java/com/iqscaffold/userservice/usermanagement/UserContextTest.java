package com.iqscaffold.userservice.usermanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.junit.jupiter.api.Test;


class UserContextTest {

  @Test
  void shouldCreateValidUserContext() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of("USER", "ADMIN"),
        Set.of("READ_PROFILE", "WRITE_PROFILE"),
        "John",
        "Doe",
        "tenant-1",
        123L,
        Map.of("department", "Engineering")
    );

    assertEquals(1L, context.userId());
    assertEquals("john.doe", context.username());
    assertEquals("john.doe@example.com", context.email());
    assertEquals(Set.of("USER", "ADMIN"), context.authorities());
    assertEquals(Set.of("READ_PROFILE", "WRITE_PROFILE"), context.permissions());
    assertEquals("John", context.firstName());
    assertEquals("Doe", context.lastName());
    assertEquals("tenant-1", context.tenantId());
    assertEquals(123L, context.organizationId());
    assertEquals(Map.of("department", "Engineering"), context.customClaims());
  }

  @Test
  void shouldThrowExceptionWhenUserIdIsNull() {
    assertThrows(NullPointerException.class, () ->
        constructUserContextThrowCause(
            null,
            "john.doe",
            "john.doe@example.com",
            Set.of(),
            Set.of(),
            "John",
            "Doe",
            "tenant-1",
            null,
            Map.of()
        )
    );
  }

  @Test
  void shouldThrowExceptionWhenUsernameIsNull() {
    assertThrows(NullPointerException.class, () ->
        constructUserContextThrowCause(
            1L,
            null,
            "john.doe@example.com",
            Set.of(),
            Set.of(),
            "John",
            "Doe",
            "tenant-1",
            null,
            Map.of()
        )
    );
  }

  @Test
  void shouldThrowExceptionWhenEmailIsNull() {
    assertThrows(NullPointerException.class, () ->
        constructUserContextThrowCause(
            1L,
            "john.doe",
            null,
            Set.of(),
            Set.of(),
            "John",
            "Doe",
            "tenant-1",
            null,
            Map.of()
        )
    );
  }

  @Test
  void shouldThrowExceptionWhenTenantIdIsNull() {
    assertThrows(NullPointerException.class, () ->
        constructUserContextThrowCause(
            1L,
            "john.doe",
            "john.doe@example.com",
            Set.of(),
            Set.of(),
            "John",
            "Doe",
            null,
            null,
            Map.of()
        )
    );
  }

  @Test
  void shouldCreateImmutableCollections() {
    var roles = Set.of("USER");
    var permissions = Set.of("READ");
    Map<String, Object> claims = Map.of("key", "value");

    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        roles,
        permissions,
        "John",
        "Doe",
        "tenant-1",
        null,
        claims
    );

    assertThrows(UnsupportedOperationException.class, () ->
        context.authorities().add("ADMIN")
    );

    assertThrows(UnsupportedOperationException.class, () ->
        context.permissions().add("WRITE")
    );

    assertThrows(UnsupportedOperationException.class, () ->
        context.customClaims().put("new", "value")
    );
  }

  private static void constructUserContextThrowCause(
      Long userId,
      String username,
      String email,
      @Nullable Set<String> roles,
      @Nullable Set<String> permissions,
      @Nullable String firstName,
      @Nullable String lastName,
      String tenantId,
      @Nullable Long organizationId,
      @Nullable Map<String, Object> customClaims
  ) {
    new UserContext(
        userId,
        username,
        email,
        roles,
        permissions,
        firstName,
        lastName,
        tenantId,
        organizationId,
        customClaims
    );
  }

  @Test
  void shouldHandleNullCollections() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        null,
        null,
        "John",
        "Doe",
        "tenant-1",
        null,
        null
    );

    assertNotNull(context.authorities());
    assertNotNull(context.permissions());
    assertNotNull(context.customClaims());
    assertTrue(context.authorities().isEmpty());
    assertTrue(context.permissions().isEmpty());
    assertTrue(context.customClaims().isEmpty());
  }

  @Test
  void shouldCheckIfUserHasRole() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of("USER", "ADMIN"),
        Set.of(),
        "John",
        "Doe",
        "tenant-1",
        null,
        Map.of()
    );

    assertTrue(context.hasAuthority("USER"));
    assertTrue(context.hasAuthority("ADMIN"));
    assertFalse(context.hasAuthority("SUPER_ADMIN"));
  }

  @Test
  void shouldCheckIfUserHasPermission() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of(),
        Set.of("READ_PROFILE", "WRITE_PROFILE"),
        "John",
        "Doe",
        "tenant-1",
        null,
        Map.of()
    );

    assertTrue(context.hasPermission("READ_PROFILE"));
    assertTrue(context.hasPermission("WRITE_PROFILE"));
    assertFalse(context.hasPermission("DELETE_PROFILE"));
  }

  @Test
  void shouldGetFullName() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of(),
        Set.of(),
        "John",
        "Doe",
        "tenant-1",
        null,
        Map.of()
    );

    assertEquals("John Doe", context.getFullName());
  }

  @Test
  void shouldGetFullNameWithNullFirstName() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of(),
        Set.of(),
        null,
        "Doe",
        "tenant-1",
        null,
        Map.of()
    );

    assertEquals("Doe", context.getFullName());
  }

  @Test
  void shouldGetFullNameWithNullLastName() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of(),
        Set.of(),
        "John",
        null,
        "tenant-1",
        null,
        Map.of()
    );

    assertEquals("John", context.getFullName());
  }

  @Test
  void shouldGetFullNameWithBothNamesNull() {
    var context = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of(),
        Set.of(),
        null,
        null,
        "tenant-1",
        null,
        Map.of()
    );

    assertEquals("", context.getFullName());
  }

  @Test
  void shouldCheckIfUserIsAdmin() {
    var adminContext = new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ADMIN"),
        Set.of(),
        "Admin",
        "User",
        "tenant-1",
        null,
        Map.of()
    );

    assertTrue(adminContext.isAdmin());
  }

  @Test
  void shouldCheckIfUserIsSuperAdmin() {
    var superAdminContext = new UserContext(
        1L,
        "superadmin",
        "superadmin@example.com",
        Set.of("SUPER_ADMIN"),
        Set.of(),
        "Super",
        "Admin",
        "tenant-1",
        null,
        Map.of()
    );

    assertTrue(superAdminContext.isAdmin());
  }

  @Test
  void shouldReturnFalseForNonAdminUser() {
    var userContext = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of("USER"),
        Set.of(),
        "John",
        "Doe",
        "tenant-1",
        null,
        Map.of()
    );

    assertFalse(userContext.isAdmin());
  }

  @Test
  void shouldCompareUserContexts() {
    var context1 = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of("USER"),
        Set.of("READ"),
        "John",
        "Doe",
        "tenant-1",
        null,
        Map.of()
    );

    var context2 = new UserContext(
        1L,
        "john.doe",
        "john.doe@example.com",
        Set.of("USER"),
        Set.of("READ"),
        "John",
        "Doe",
        "tenant-1",
        null,
        Map.of()
    );

    assertEquals(context1, context2);
    assertEquals(context1.hashCode(), context2.hashCode());
  }
}
