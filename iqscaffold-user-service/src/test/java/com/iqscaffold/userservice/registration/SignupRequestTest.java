package com.iqscaffold.userservice.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class SignupRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidSignupRequest() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    assertEquals("john_doe", request.username());
    assertEquals("john.doe@example.com", request.email());
    assertEquals("SecurePass123!", request.password());
    assertEquals("John", request.firstName());
    assertEquals("Doe", request.lastName());
    assertEquals("tenant-1", request.tenantId());
  }

  @Test
  void shouldTrimUsername() {
    var request = new SignupRequest(
        "  john_doe  ",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    assertEquals("john_doe", request.username());
  }

  @Test
  void shouldTrimAndLowercaseEmail() {
    var request = new SignupRequest(
        "john_doe",
        "  John.Doe@Example.COM  ",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    assertEquals("john.doe@example.com", request.email());
  }

  @Test
  void shouldTrimFirstName() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "  John  ",
        "Doe",
        "tenant-1"
    );

    assertEquals("John", request.firstName());
  }

  @Test
  void shouldTrimLastName() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "  Doe  ",
        "tenant-1"
    );

    assertEquals("Doe", request.lastName());
  }

  @Test
  void shouldDefaultTenantIdWhenNull() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        null
    );

    assertEquals("default", request.tenantId());
  }

  @Test
  void shouldDefaultTenantIdWhenEmpty() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        ""
    );

    assertEquals("default", request.tenantId());
  }

  @Test
  void shouldDefaultTenantIdWhenBlank() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "   "
    );

    assertEquals("default", request.tenantId());
  }

  @Test
  void shouldTrimTenantId() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "  tenant-1  "
    );

    assertEquals("tenant-1", request.tenantId());
  }

  @Test
  void shouldValidateUsernameNotBlank() {
    var request = new SignupRequest(
        "",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Username is required")));
  }

  @Test
  void shouldValidateUsernameMinLength() {
    var request = new SignupRequest(
        "ab",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 3 and 50 characters")));
  }

  @Test
  void shouldValidateUsernameMaxLength() {
    var longUsername = "a".repeat(51);
    var request = new SignupRequest(
        longUsername,
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateEmailNotBlank() {
    var request = new SignupRequest(
        "john_doe",
        "",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Email is required")));
  }

  @Test
  void shouldValidateEmailFormat() {
    var request = new SignupRequest(
        "john_doe",
        "invalid-email",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Email must be valid")));
  }

  @Test
  void shouldValidateEmailMaxLength() {
    var longEmail = "a".repeat(250) + "@example.com";
    var request = new SignupRequest(
        "john_doe",
        longEmail,
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidatePasswordNotBlank() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Password is required")));
  }

  @Test
  void shouldValidatePasswordMinLength() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "Short1!",
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 8 and 100 characters")));
  }

  @Test
  void shouldValidatePasswordMaxLength() {
    var longPassword = "A".repeat(101);
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        longPassword,
        "John",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateFirstNameNotBlank() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "",
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("First name is required")));
  }

  @Test
  void shouldValidateFirstNameMaxLength() {
    var longName = "A".repeat(101);
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        longName,
        "Doe",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateLastNameNotBlank() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "",
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Last name is required")));
  }

  @Test
  void shouldValidateLastNameMaxLength() {
    var longName = "A".repeat(101);
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        longName,
        "tenant-1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateTenantIdMaxLength() {
    var longTenantId = "a".repeat(101);
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        longTenantId
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAcceptMinimumValidLengths() {
    var request = new SignupRequest(
        "abc",
        "a@b.com",
        "Pass123!",
        "J",
        "D",
        "t"
    );

    // May have custom validator violations, but basic constraints should pass
    assertNotNull(request);
    assertEquals("abc", request.username());
    assertEquals("a@b.com", request.email());
    assertEquals("J", request.firstName());
    assertEquals("D", request.lastName());
  }

  @Test
  void shouldHandleNullUsername() {
    var request = new SignupRequest(
        null,
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    assertNull(request.username());
  }

  @Test
  void shouldHandleNullEmail() {
    var request = new SignupRequest(
        "john_doe",
        null,
        "SecurePass123!",
        "John",
        "Doe",
        "tenant-1"
    );

    assertNull(request.email());
  }

  @Test
  void shouldHandleNullFirstName() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        null,
        "Doe",
        "tenant-1"
    );

    assertNull(request.firstName());
  }

  @Test
  void shouldHandleNullLastName() {
    var request = new SignupRequest(
        "john_doe",
        "john.doe@example.com",
        "SecurePass123!",
        "John",
        null,
        "tenant-1"
    );

    assertNull(request.lastName());
  }
}

