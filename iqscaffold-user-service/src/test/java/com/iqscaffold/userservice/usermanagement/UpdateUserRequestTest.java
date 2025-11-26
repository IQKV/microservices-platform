package com.iqscaffold.userservice.usermanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for UpdateUserRequest class.
 * Tests record creation and handling of nullable fields.
 */
class UpdateUserRequestTest {

  @Test
  void shouldCreateValidUpdateUserRequestWithAllFields() {
    var request = new UpdateUserRequest(
        "newusername",
        "newemail@example.com",
        "NewPassword123!",
        "NewJohn",
        "NewDoe",
        false,
        true,
        Set.of("ADMIN", "SUPER_ADMIN")
    );

    assertEquals("newusername", request.username());
    assertEquals("newemail@example.com", request.email());
    assertEquals("NewPassword123!", request.password());
    assertEquals("NewJohn", request.firstName());
    assertEquals("NewDoe", request.lastName());
    assertFalse(request.enabled());
    assertTrue(request.emailVerified());
    assertEquals(Set.of("ADMIN", "SUPER_ADMIN"), request.roles());
  }

  @Test
  void shouldCreateUpdateUserRequestWithNullFields() {
    var request = new UpdateUserRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    assertNull(request.username());
    assertNull(request.email());
    assertNull(request.password());
    assertNull(request.firstName());
    assertNull(request.lastName());
    assertNull(request.enabled());
    assertNull(request.emailVerified());
    assertNull(request.roles());
  }

  @Test
  void shouldCreateUpdateUserRequestWithPartialFields() {
    var request = new UpdateUserRequest(
        "updateduser",
        null,
        null,
        "Updated",
        null,
        false,
        null,
        Set.of("USER")
    );

    assertEquals("updateduser", request.username());
    assertNull(request.email());
    assertNull(request.password());
    assertEquals("Updated", request.firstName());
    assertNull(request.lastName());
    assertFalse(request.enabled());
    assertNull(request.emailVerified());
    assertEquals(Set.of("USER"), request.roles());
  }

  @Test
  void shouldHandleEmptyStrings() {
    var request = new UpdateUserRequest(
        "",
        "",
        "",
        "",
        "",
        null,
        null,
        null
    );

    assertEquals("", request.username());
    assertEquals("", request.email());
    assertEquals("", request.password());
    assertEquals("", request.firstName());
    assertEquals("", request.lastName());
    assertNull(request.enabled());
    assertNull(request.emailVerified());
    assertNull(request.roles());
  }

  @Test
  void shouldHandleSingleRole() {
    var request = new UpdateUserRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Set.of("ADMIN")
    );

    assertEquals(Set.of("ADMIN"), request.roles());
  }

  @Test
  void shouldHandleMultipleRoles() {
    var request = new UpdateUserRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Set.of("USER", "ADMIN", "SUPER_ADMIN")
    );

    assertEquals(Set.of("USER", "ADMIN", "SUPER_ADMIN"), request.roles());
  }

  @Test
  void shouldHandleEmptyRolesSet() {
    var request = new UpdateUserRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Set.of()
    );

    assertEquals(Set.of(), request.roles());
  }

  @Test
  void shouldHandleBooleanValues() {
    var enabledTrueRequest = new UpdateUserRequest(
        null,
        null,
        null,
        null,
        null,
        true,
        true,
        null
    );

    var enabledFalseRequest = new UpdateUserRequest(
        null,
        null,
        null,
        null,
        null,
        false,
        false,
        null
    );

    assertTrue(enabledTrueRequest.enabled());
    assertTrue(enabledTrueRequest.emailVerified());
    assertFalse(enabledFalseRequest.enabled());
    assertFalse(enabledFalseRequest.emailVerified());
  }

  @Test
  void shouldHandleMixedNullAndNonNullValues() {
    var request = new UpdateUserRequest(
        "username", // non-null
        null,       // null
        "Password123!", // non-null
        null,       // null
        "Doe",      // non-null
        null,       // null
        true,       // non-null
        null        // null
    );

    assertEquals("username", request.username());
    assertNull(request.email());
    assertEquals("Password123!", request.password());
    assertNull(request.firstName());
    assertEquals("Doe", request.lastName());
    assertNull(request.enabled());
    assertTrue(request.emailVerified());
    assertNull(request.roles());
  }

  @Test
  void shouldHandleLongValues() {
    var request = new UpdateUserRequest(
        "a".repeat(50), // max length
        "user@verylongdomainname.com",
        "VeryLongPassword123!WithSpecialChars",
        "a".repeat(100), // max length
        "b".repeat(100), // max length
        true,
        false,
        Set.of("USER", "ADMIN", "SUPER_ADMIN", "MANAGER", "VIEWER")
    );

    assertEquals(50, request.username().length());
    assertEquals("user@verylongdomainname.com", request.email());
    assertEquals("VeryLongPassword123!WithSpecialChars", request.password());
    assertEquals(100, request.firstName().length());
    assertEquals(100, request.lastName().length());
    assertTrue(request.enabled());
    assertFalse(request.emailVerified());
    assertEquals(5, request.roles().size());
  }
}
