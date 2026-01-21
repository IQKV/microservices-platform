package com.iqscaffold.userservice.passwordmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class ChangePasswordRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidChangePasswordRequest() {
    var request = new ChangePasswordRequest(
        "currentPassword123",
        "newPassword456"
    );

    assertEquals("currentPassword123", request.currentPassword());
    assertEquals("newPassword456", request.newPassword());
  }

  @Test
  void shouldValidateCurrentPasswordNotBlank() {
    var request = new ChangePasswordRequest(
        "",
        "newPassword456"
    );

    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Current password is required")));
  }

  @Test
  void shouldValidateCurrentPasswordNotNull() {
    var request = new ChangePasswordRequest(
        null,
        "newPassword456"
    );

    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateNewPasswordNotBlank() {
    var request = new ChangePasswordRequest(
        "currentPassword123",
        ""
    );

    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("New password is required")));
  }

  @Test
  void shouldValidateNewPasswordNotNull() {
    var request = new ChangePasswordRequest(
        "currentPassword123",
        null
    );

    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateNewPasswordMinLength() {
    var request = new ChangePasswordRequest(
        "currentPassword123",
        "short"
    );

    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 8 and 128 characters")));
  }

  @Test
  void shouldValidateNewPasswordMaxLength() {
    var longPassword = "a".repeat(129);
    var request = new ChangePasswordRequest(
        "currentPassword123",
        longPassword
    );

    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("between 8 and 128 characters")));
  }

  @Test
  void shouldAcceptMinimumLengthPassword() {
    var request = new ChangePasswordRequest(
        "currentPassword123",
        "12345678"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptMaximumLengthPassword() {
    var maxPassword = "a".repeat(128);
    var request = new ChangePasswordRequest(
        "currentPassword123",
        maxPassword
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptPasswordsWithSpecialCharacters() {
    var request = new ChangePasswordRequest(
        "current!@#$%^&*()",
        "new!@#$%^&*()"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptPasswordsWithNumbers() {
    var request = new ChangePasswordRequest(
        "current123456",
        "newPass123456"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptPasswordsWithMixedCase() {
    var request = new ChangePasswordRequest(
        "CurrentPassword123",
        "NewPassword456"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAllowSameCurrentAndNewPassword() {
    // Validation doesn't prevent this - business logic should
    var request = new ChangePasswordRequest(
        "samePassword123",
        "samePassword123"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldHandlePasswordsWithSpaces() {
    var request = new ChangePasswordRequest(
        "current password",
        "new password"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldHandlePasswordsWithUnicode() {
    var request = new ChangePasswordRequest(
        "currentПароль123",
        "newПароль456"
    );

    assertTrue(validator.validate(request).isEmpty());
  }
}

