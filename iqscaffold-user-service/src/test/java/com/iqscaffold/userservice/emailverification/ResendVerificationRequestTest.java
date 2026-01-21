package com.iqscaffold.userservice.emailverification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class ResendVerificationRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidResendVerificationRequest() {
    var request = new ResendVerificationRequest("john.doe@example.com");

    assertEquals("john.doe@example.com", request.email());
  }

  @Test
  void shouldTrimAndLowercaseEmail() {
    var request = new ResendVerificationRequest("  John.Doe@Example.COM  ");

    assertEquals("john.doe@example.com", request.email());
  }

  @Test
  void shouldFailValidationWhenEmailIsBlank() {
    var request = new ResendVerificationRequest("");
    Set<ConstraintViolation<ResendVerificationRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Email is required")));
  }

  @Test
  void shouldFailValidationWhenEmailIsNull() {
    var request = new ResendVerificationRequest(null);
    Set<ConstraintViolation<ResendVerificationRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldFailValidationWhenEmailIsInvalid() {
    var request = new ResendVerificationRequest("invalid-email");
    Set<ConstraintViolation<ResendVerificationRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("Email must be valid")));
  }

  @Test
  void shouldFailValidationWhenEmailIsTooLong() {
    var longEmail = "a".repeat(250) + "@example.com";
    var request = new ResendVerificationRequest(longEmail);
    Set<ConstraintViolation<ResendVerificationRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("must not exceed 255 characters")));
  }

  @Test
  void shouldAcceptValidEmailFormats() {
    var emails = new String[] {
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.co.uk",
        "user_name@sub.example.com"
    };

    for (final var email : emails) {
      var request = new ResendVerificationRequest(email);
      Set<ConstraintViolation<ResendVerificationRequest>> violations = validator.validate(request);
      assertTrue(violations.isEmpty(), "Email should be valid: " + email);
    }
  }

  @Test
  void shouldHandleEmailWithWhitespace() {
    var request = new ResendVerificationRequest("  user@example.com  ");

    assertEquals("user@example.com", request.email());
    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldNormalizeEmailCase() {
    var request = new ResendVerificationRequest("User@EXAMPLE.COM");

    assertEquals("user@example.com", request.email());
  }
}

