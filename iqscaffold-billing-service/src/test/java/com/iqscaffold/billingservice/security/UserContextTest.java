package com.iqscaffold.billingservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class UserContextTest {

  @Test
  void hasAuthority_shouldReturnTrueWhenAuthorityExists() {
    // Given
    UserContext context = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("READ", "WRITE"),
        "tenant1",
        null,
        "John",
        "Doe"
    );

    // When & Then
    assertTrue(context.hasAuthority("READ"));
    assertTrue(context.hasAuthority("WRITE"));
  }

  @Test
  void hasAuthority_shouldReturnFalseWhenAuthorityDoesNotExist() {
    // Given
    UserContext context = new UserContext(
        1L,
        "testuser",
        "test@example.com",
        Set.of("READ"),
        "tenant1",
        null,
        "John",
        "Doe"
    );

    // When & Then
    assertFalse(context.hasAuthority("DELETE"));
    assertFalse(context.hasAuthority("ADMIN"));
  }

  @Test
  void isAdmin_shouldReturnTrueWhenUserHasRoleAdmin() {
    // Given
    UserContext context = new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ROLE_ADMIN", "READ", "WRITE"),
        "tenant1",
        null,
        "Admin",
        "User"
    );

    // When & Then
    assertTrue(context.isAdmin());
  }

  @Test
  void isAdmin_shouldReturnTrueWhenUserHasAdminAuthority() {
    // Given
    UserContext context = new UserContext(
        1L,
        "admin",
        "admin@example.com",
        Set.of("ADMIN", "READ"),
        "tenant1",
        null,
        "Admin",
        "User"
    );

    // When & Then
    assertTrue(context.isAdmin());
  }

  @Test
  void isAdmin_shouldReturnFalseWhenUserIsNotAdmin() {
    // Given
    UserContext context = new UserContext(
        1L,
        "user",
        "user@example.com",
        Set.of("READ", "WRITE"),
        "tenant1",
        null,
        "Regular",
        "User"
    );

    // When & Then
    assertFalse(context.isAdmin());
  }

  @Test
  void userContext_shouldStoreAllFields() {
    // Given
    Long userId = 123L;
    String username = "testuser";
    String email = "test@example.com";
    Set<String> authorities = Set.of("READ", "WRITE");
    String tenantId = "tenant123";
    Long organizationId = 456L;
    String firstName = "John";
    String lastName = "Doe";

    // When
    UserContext context = new UserContext(
        userId, username, email, authorities, tenantId, organizationId, firstName, lastName
    );

    // Then
    assertEquals(userId, context.userId());
    assertEquals(username, context.username());
    assertEquals(email, context.email());
    assertEquals(authorities, context.authorities());
    assertEquals(tenantId, context.tenantId());
    assertEquals(organizationId, context.organizationId());
    assertEquals(firstName, context.firstName());
    assertEquals(lastName, context.lastName());
  }

  @Test
  void userContext_shouldHandleEmptyAuthorities() {
    // Given
    UserContext context = new UserContext(
        1L,
        "user",
        "user@example.com",
        Set.of(),
        "tenant1",
        null,
        "John",
        "Doe"
    );

    // When & Then
    assertFalse(context.hasAuthority("READ"));
    assertFalse(context.isAdmin());
  }

  @Test
  void userContext_shouldHandleNullValues() {
    // Given
    UserContext context = new UserContext(
        null,
        null,
        null,
        Set.of("READ"),
        null,
        null,
        null,
        null
    );

    // When & Then
    assertNull(context.userId());
    assertNull(context.username());
    assertNull(context.email());
    assertNull(context.tenantId());
    assertNull(context.organizationId());
    assertNull(context.firstName());
    assertNull(context.lastName());
    assertTrue(context.hasAuthority("READ"));
  }
}
