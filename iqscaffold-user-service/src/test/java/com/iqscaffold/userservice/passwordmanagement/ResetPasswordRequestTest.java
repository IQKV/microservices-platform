package com.iqscaffold.userservice.passwordmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class ResetPasswordRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidResetPasswordRequest() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "newPassword123"
    );

    assertEquals("reset-token-123", request.token());
    assertEquals("newPassword123", request.newPassword());
  }

  @Test
  void shouldValidateTokenNotBlank() {
    var request = new ResetPasswordRequest(
        "",
        "newPassword123"
    );

    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateTokenNotNull() {
    var request = new ResetPasswordRequest(
        null,
        "newPassword123"
    );

    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateNewPasswordNotBlank() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        ""
    );

    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateNewPasswordNotNull() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        null
    );

    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateNewPasswordMinLength() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "short"
    );

    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAcceptMinimumLengthPassword() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "12345678"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptLongPassword() {
    var longPassword = "a".repeat(100);
    var request = new ResetPasswordRequest(
        "reset-token-123",
        longPassword
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptPasswordWithSpecialCharacters() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "P@ssw0rd!#$%"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptPasswordWithNumbers() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "Password123456"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptPasswordWithMixedCase() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "NewPassword123"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptVariousTokenFormats() {
    var tokens = new String[] {
        "simple-token",
        "token_with_underscore",
        "token.with.dots",
        "TOKEN123",
        "very-long-token-with-many-characters-123456789"
    };

    for (final var token : tokens) {
      var request = new ResetPasswordRequest(token, "newPassword123");
      assertTrue(validator.validate(request).isEmpty(), "Token should be valid: " + token);
    }
  }

  @Test
  void shouldAcceptJwtLikeToken() {
    var jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abc123";
    var request = new ResetPasswordRequest(jwtToken, "newPassword123");

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptUuidToken() {
    var uuidToken = "550e8400-e29b-41d4-a716-446655440000";
    var request = new ResetPasswordRequest(uuidToken, "newPassword123");

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldHandlePasswordWithSpaces() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "password with spaces"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldHandlePasswordWithUnicode() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "Пароль123456"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldValidateExactly8CharacterPassword() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "12345678"
    );

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldReject7CharacterPassword() {
    var request = new ResetPasswordRequest(
        "reset-token-123",
        "1234567"
    );

    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }
}

