package com.iqscaffold.userservice.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RefreshTokenRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidRefreshTokenRequest() {
    var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0.abc123";
    var request = new RefreshTokenRequest(token);

    assertEquals(token, request.refreshToken());
  }

  @Test
  void shouldFailValidationWhenTokenIsBlank() {
    var request = new RefreshTokenRequest("");
    Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Refresh token is required")));
  }

  @Test
  void shouldFailValidationWhenTokenIsNull() {
    var request = new RefreshTokenRequest(null);
    Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAcceptLongTokenStrings() {
    var longToken = "a".repeat(500);
    var request = new RefreshTokenRequest(longToken);

    assertEquals(longToken, request.refreshToken());
    assertTrue(validator.validate(request).isEmpty());
  }
}
