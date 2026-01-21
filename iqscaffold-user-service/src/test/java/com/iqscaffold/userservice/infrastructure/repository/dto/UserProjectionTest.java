package com.iqscaffold.userservice.infrastructure.repository.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;


class UserProjectionTest {

  @Test
  void shouldCreateUserProjection() {
    var createdAt = LocalDateTime.now();
    var projection = new UserProjection(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        createdAt,
        "tenant-1",
        Set.of("USER", "ADMIN")
    );

    assertEquals(1L, projection.id());
    assertEquals("john.doe", projection.username());
    assertEquals("john.doe@example.com", projection.email());
    assertEquals("John", projection.firstName());
    assertEquals("Doe", projection.lastName());
    assertTrue(projection.enabled());
    assertTrue(projection.emailVerified());
    assertEquals(createdAt, projection.createdAt());
    assertEquals("tenant-1", projection.tenantId());
    assertEquals(Set.of("USER", "ADMIN"), projection.authorityNames());
  }

  @Test
  void shouldCreateWithBasicInformation() {
    var createdAt = LocalDateTime.now();
    var projection = UserProjection.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        createdAt,
        "tenant-1"
    );

    assertTrue(projection.authorityNames().isEmpty());
  }

  @Test
  void shouldCreateWithAuthorities() {
    var createdAt = LocalDateTime.now();
    var projection = UserProjection.withAuthorities(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        createdAt,
        "tenant-1",
        Set.of("USER", "ADMIN")
    );

    assertEquals(2, projection.authorityNames().size());
    assertTrue(projection.authorityNames().contains("USER"));
    assertTrue(projection.authorityNames().contains("ADMIN"));
  }

  @Test
  void shouldGetFullName() {
    var projection = UserProjection.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1"
    );

    assertEquals("John Doe", projection.fullName());
  }

  @Test
  void shouldCheckIfUserIsActive() {
    var activeUser = UserProjection.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1"
    );

    assertTrue(activeUser.isActive());
  }

  @Test
  void shouldCheckIfUserIsInactiveWhenDisabled() {
    var inactiveUser = UserProjection.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        false,
        true,
        LocalDateTime.now(),
        "tenant-1"
    );

    assertFalse(inactiveUser.isActive());
  }

  @Test
  void shouldCheckIfUserIsInactiveWhenEmailNotVerified() {
    var inactiveUser = UserProjection.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        false,
        LocalDateTime.now(),
        "tenant-1"
    );

    assertFalse(inactiveUser.isActive());
  }

  @Test
  void shouldCheckIfUserHasAuthority() {
    var projection = UserProjection.withAuthorities(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1",
        Set.of("USER", "ADMIN")
    );

    assertTrue(projection.hasAuthority("USER"));
    assertTrue(projection.hasAuthority("ADMIN"));
    assertFalse(projection.hasAuthority("SUPER_ADMIN"));
  }

  @Test
  void shouldHandleNullAuthorities() {
    var projection = new UserProjection(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1",
        null
    );

    assertFalse(projection.hasAuthority("USER"));
  }

  @Test
  void shouldCheckIfUserIsAdmin() {
    var adminUser = UserProjection.withAuthorities(
        1L,
        "admin",
        "admin@example.com",
        "Admin",
        "User",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1",
        Set.of("ADMIN")
    );

    assertTrue(adminUser.isAdmin());
  }

  @Test
  void shouldCheckIfUserIsSuperAdmin() {
    var superAdminUser = UserProjection.withAuthorities(
        1L,
        "superadmin",
        "superadmin@example.com",
        "Super",
        "Admin",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1",
        Set.of("SUPER_ADMIN")
    );

    assertTrue(superAdminUser.isAdmin());
  }

  @Test
  void shouldCheckIfRegularUserIsNotAdmin() {
    var regularUser = UserProjection.withAuthorities(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1",
        Set.of("USER")
    );

    assertFalse(regularUser.isAdmin());
  }

  @Test
  void shouldGetDisplayNameFromFullName() {
    var projection = UserProjection.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1"
    );

    assertEquals("John Doe", projection.displayName());
  }

  @Test
  void shouldFallbackToUsernameWhenFullNameIsBlank() {
    var projection = UserProjection.of(
        1L,
        "john.doe",
        "john.doe@example.com",
        "",
        "",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1"
    );

    assertEquals("john.doe", projection.displayName());
  }

  @Test
  void shouldHandleNullEnabledStatus() {
    var projection = new UserProjection(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        null,
        true,
        LocalDateTime.now(),
        "tenant-1",
        Set.of()
    );

    assertFalse(projection.isActive());
  }

  @Test
  void shouldHandleNullEmailVerifiedStatus() {
    var projection = new UserProjection(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        null,
        LocalDateTime.now(),
        "tenant-1",
        Set.of()
    );

    assertFalse(projection.isActive());
  }

  @Test
  void shouldHandleEmptyAuthorities() {
    var projection = UserProjection.withAuthorities(
        1L,
        "john.doe",
        "john.doe@example.com",
        "John",
        "Doe",
        true,
        true,
        LocalDateTime.now(),
        "tenant-1",
        Set.of()
    );

    assertFalse(projection.hasAuthority("USER"));
    assertFalse(projection.isAdmin());
  }
}
