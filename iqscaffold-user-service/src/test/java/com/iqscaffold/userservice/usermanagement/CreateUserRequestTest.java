package com.iqscaffold.userservice.usermanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for CreateUserRequest class.
 * Tests record creation, validation properties, and default value handling.
 */
class CreateUserRequestTest {

  @Test
  void shouldCreateValidCreateUserRequest() {
    var request = new CreateUserRequest(
        "testuser",
        "test@example.com",
        "Password123!",
        "John",
        "Doe",
        true,
        true,
        Set.of("USER", "ADMIN")
    );

    assertEquals("testuser", request.username());
    assertEquals("test@example.com", request.email());
    assertEquals("Password123!", request.password());
    assertEquals("John", request.firstName());
    assertEquals("Doe", request.lastName());
    assertTrue(request.enabled());
    assertTrue(request.emailVerified());
    assertEquals(Set.of("USER", "ADMIN"), request.authorities());
  }

  @Test
  void shouldApplyDefaultValuesWhenNotProvided() {
    var request = new CreateUserRequest(
        "testuser",
        "test@example.com",
        "Password123!",
        "John",
        "Doe",
        null,
        null,
        null
    );

    assertEquals("testuser", request.username());
    assertEquals("test@example.com", request.email());
    assertEquals("Password123!", request.password());
    assertEquals("John", request.firstName());
    assertEquals("Doe", request.lastName());
    assertTrue(request.enabled()); // Default should be true
    assertFalse(request.emailVerified()); // Default should be false
    assertEquals(Set.of("USER"), request.authorities()); // Default should be USER
  }

  @Test
  void shouldApplyDefaultValuesWhenProvided() {
    var request = new CreateUserRequest(
        "testuser",
        "test@example.com",
        "Password123!",
        "John",
        "Doe",
        false,
        true,
        Set.of("ADMIN")
    );

    assertEquals("testuser", request.username());
    assertEquals("test@example.com", request.email());
    assertEquals("Password123!", request.password());
    assertEquals("John", request.firstName());
    assertEquals("Doe", request.lastName());
    assertFalse(request.enabled()); // Explicitly set to false
    assertTrue(request.emailVerified()); // Explicitly set to true
    assertEquals(Set.of("ADMIN"), request.authorities()); // Explicitly set to ADMIN
  }

  @Test
  void shouldHandleEmptyRolesSet() {
    var request = new CreateUserRequest(
        "testuser",
        "test@example.com",
        "Password123!",
        "John",
        "Doe",
        true,
        true,
        Set.of()
    );

    assertEquals(Set.of(), request.authorities());
  }

  @Test
  void shouldHandleSingleRole() {
    var request = new CreateUserRequest(
        "testuser",
        "test@example.com",
        "Password123!",
        "John",
        "Doe",
        true,
        true,
        Set.of("ADMIN")
    );

    assertEquals(Set.of("ADMIN"), request.authorities());
  }

  @Test
  void shouldHandleMultipleRoles() {
    var request = new CreateUserRequest(
        "testuser",
        "test@example.com",
        "Password123!",
        "John",
        "Doe",
        true,
        true,
        Set.of("USER", "ADMIN", "SUPER_ADMIN")
    );

    assertEquals(Set.of("USER", "ADMIN", "SUPER_ADMIN"), request.authorities());
  }

  @Test
  void shouldHandleBooleanValues() {
    var enabledTrueRequest = new CreateUserRequest(
        "testuser1",
        "test1@example.com",
        "Password123!",
        "John",
        "Doe",
        true,
        true,
        Set.of()
    );

    var enabledFalseRequest = new CreateUserRequest(
        "testuser2",
        "test2@example.com",
        "Password123!",
        "Jane",
        "Doe",
        false,
        false,
        Set.of()
    );

    assertTrue(enabledTrueRequest.enabled());
    assertTrue(enabledTrueRequest.emailVerified());
    assertFalse(enabledFalseRequest.enabled());
    assertFalse(enabledFalseRequest.emailVerified());
  }

  @Test
  void shouldHandleNullValues() {
    var request = new CreateUserRequest(
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
    assertTrue(request.enabled()); // Default should be applied
    assertFalse(request.emailVerified()); // Default should be applied
    assertEquals(Set.of("USER"), request.authorities()); // Default should be applied
  }

  @Test
  void shouldHandleEmptyStrings() {
    var request = new CreateUserRequest(
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
    assertTrue(request.enabled()); // Default should be applied
    assertFalse(request.emailVerified()); // Default should be applied
    assertEquals(Set.of("USER"), request.authorities()); // Default should be applied
  }
}

