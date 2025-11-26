package com.iqscaffold.userservice.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LoginRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidLoginRequest() {
    var request = new LoginRequest("john.doe", "password123", false);

    assertEquals("john.doe", request.username());
    assertEquals("password123", request.password());
    assertFalse(request.rememberMe());
  }

  @Test
  void shouldTrimAndLowercaseUsername() {
    var request = new LoginRequest("  John.Doe  ", "password123", false);

    assertEquals("john.doe", request.username());
  }

  @Test
  void shouldNotTrimPassword() {
    var request = new LoginRequest("john.doe", "  password123  ", false);

    assertEquals("  password123  ", request.password());
  }

  @Test
  void shouldFailValidationWhenUsernameIsBlank() {
    var request = new LoginRequest("", "password123", false);
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Username or email is required")));
  }

  @Test
  void shouldFailValidationWhenPasswordIsBlank() {
    var request = new LoginRequest("john.doe", "", false);
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Password is required")));
  }

  @Test
  void shouldFailValidationWhenUsernameIsTooShort() {
    var request = new LoginRequest("ab", "password123", false);
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 3 and 255 characters")));
  }

  @Test
  void shouldFailValidationWhenUsernameIsTooLong() {
    var username = "a".repeat(256);
    var request = new LoginRequest(username, "password123", false);
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 3 and 255 characters")));
  }

  @Test
  void shouldFailValidationWhenPasswordIsTooLong() {
    var password = "a".repeat(101);
    var request = new LoginRequest("john.doe", password, false);
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("must not exceed 100 characters")));
  }

  @Test
  void shouldAcceptEmailAsUsername() {
    var request = new LoginRequest("john.doe@example.com", "password123", true);

    assertEquals("john.doe@example.com", request.username());
    assertTrue(request.rememberMe());
  }

  @Test
  void shouldHandleNullUsernameGracefully() {
    var request = new LoginRequest(null, "password123", false);

    assertNull(request.username());
  }
}
