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


class ValidateTokenRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidValidateTokenRequest() {
    var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0.abc123";
    var request = new ValidateTokenRequest(token);

    assertEquals(token, request.token());
  }

  @Test
  void shouldFailValidationWhenTokenIsBlank() {
    var request = new ValidateTokenRequest("");
    Set<ConstraintViolation<ValidateTokenRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldFailValidationWhenTokenIsNull() {
    var request = new ValidateTokenRequest(null);
    Set<ConstraintViolation<ValidateTokenRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAcceptValidJwtToken() {
    var jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    var request = new ValidateTokenRequest(jwtToken);

    assertEquals(jwtToken, request.token());
    assertTrue(validator.validate(request).isEmpty());
  }
}
