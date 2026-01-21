package com.iqscaffold.userservice.passwordmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class ForgotPasswordRequestTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldCreateValidForgotPasswordRequest() {
    var request = new ForgotPasswordRequest("user@example.com");

    assertEquals("user@example.com", request.email());
  }

  @Test
  void shouldValidateEmailNotBlank() {
    var request = new ForgotPasswordRequest("");

    Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateEmailNotNull() {
    var request = new ForgotPasswordRequest(null);

    Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldValidateEmailFormat() {
    var request = new ForgotPasswordRequest("invalid-email");

    Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void shouldAcceptValidEmailFormats() {
    var validEmails = new String[] {
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.co.uk",
        "user_name@sub.example.com",
        "123@example.com"
    };

    for (final var email : validEmails) {
      var request = new ForgotPasswordRequest(email);
      Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
      assertTrue(violations.isEmpty(), "Email should be valid: " + email);
    }
  }

  @Test
  void shouldRejectInvalidEmailFormats() {
    var invalidEmails = new String[] {
        "invalid",
        "@example.com",
        "user@",
        "user @example.com"
    };

    for (final var email : invalidEmails) {
      var request = new ForgotPasswordRequest(email);
      Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
      assertFalse(violations.isEmpty(), "Email should be invalid: " + email);
    }
  }

  @Test
  void shouldAcceptEmailWithNumbers() {
    var request = new ForgotPasswordRequest("user123@example.com");

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptEmailWithHyphens() {
    var request = new ForgotPasswordRequest("user-name@example.com");

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptEmailWithSubdomain() {
    var request = new ForgotPasswordRequest("user@mail.example.com");

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptEmailWithPlusSign() {
    var request = new ForgotPasswordRequest("user+tag@example.com");

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldAcceptLongEmail() {
    var longEmail = "verylongemailaddress@verylongdomainname.com";
    var request = new ForgotPasswordRequest(longEmail);

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void shouldHandleEmailWithUpperCase() {
    var request = new ForgotPasswordRequest("User@Example.COM");

    assertTrue(validator.validate(request).isEmpty());
  }
}

