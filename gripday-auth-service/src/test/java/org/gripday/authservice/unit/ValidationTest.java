package org.gripday.authservice.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import org.gripday.authservice.presentation.dto.LoginRequest;
import org.gripday.authservice.presentation.dto.SignupRequest;
import org.gripday.authservice.presentation.validation.InputSanitizer;
import org.gripday.authservice.presentation.validation.PasswordValidator;
import org.gripday.authservice.presentation.validation.UsernameValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for input validation and security measures. Tests custom validation annotations and input sanitization without Spring context.
 */
class ValidationTest {

  private Validator validator;
  private InputSanitizer inputSanitizer;
  private PasswordValidator passwordValidator;
  private UsernameValidator usernameValidator;

  @BeforeEach
  void setUp() {
    // Create validator factory without Spring context
    var factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();

    // Create instances directly
    inputSanitizer = new InputSanitizer();
    passwordValidator = new PasswordValidator();
    usernameValidator = new UsernameValidator();
  }

  @Test
  void testValidSignupRequest() {
    var request = new SignupRequest(
        "validuser",
        "user@example.com",
        "ValidPass123!",
        "John",
        "Doe",
        "tenant1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty(), "Valid signup request should have no violations");
  }

  @Test
  void testInvalidPasswordComplexity() {
    var request = new SignupRequest(
        "validuser",
        "user@example.com",
        "weakpass", // Missing uppercase, number, and special character
        "John",
        "Doe",
        "tenant1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty(), "Weak password should have violations");

    var passwordViolations = violations.stream()
        .filter(v -> v.getPropertyPath().toString().equals("password"))
        .toList();
    assertFalse(passwordViolations.isEmpty(), "Should have password complexity violations");
  }

  @Test
  void testInvalidUsername() {
    var request = new SignupRequest(
        "admin", // Reserved username
        "user@example.com",
        "ValidPass123!",
        "John",
        "Doe",
        "tenant1"
    );

    Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty(), "Reserved username should have violations");
  }

  @Test
  void testValidLoginRequest() {
    var request = new LoginRequest(
        "validuser",
        "ValidPass123!",
        false
    );

    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty(), "Valid login request should have no violations");
  }

  @Test
  void testPasswordValidatorDirectly() {
    // Test valid password
    assertTrue(passwordValidator.isValid("ValidPass123!", null));

    // Test invalid passwords
    assertFalse(passwordValidator.isValid("lowercase", null)); // No uppercase, number, special
    assertFalse(passwordValidator.isValid("UPPERCASE", null)); // No lowercase, number, special
    assertFalse(passwordValidator.isValid("NoNumbers!", null)); // No numbers
    assertFalse(passwordValidator.isValid("NoSpecial123", null)); // No special characters
    assertFalse(passwordValidator.isValid("", null)); // Empty
    assertFalse(passwordValidator.isValid(null, null)); // Null
  }

  @Test
  void testUsernameValidatorDirectly() {
    // Test valid usernames
    assertTrue(usernameValidator.isValid("validuser", null));
    assertTrue(usernameValidator.isValid("user123", null));
    assertTrue(usernameValidator.isValid("user_name", null));
    assertTrue(usernameValidator.isValid("user-name", null));

    // Test invalid usernames
    assertFalse(usernameValidator.isValid("admin", null)); // Reserved
    assertFalse(usernameValidator.isValid("root", null)); // Reserved
    assertFalse(usernameValidator.isValid("_username", null)); // Starts with special char
    assertFalse(usernameValidator.isValid("username_", null)); // Ends with special char
    assertFalse(usernameValidator.isValid("user__name", null)); // Consecutive special chars
    assertFalse(usernameValidator.isValid("", null)); // Empty
    assertFalse(usernameValidator.isValid(null, null)); // Null
  }

  @Test
  void testInputSanitizer() {
    // Test XSS prevention
    var maliciousInput = "<script>alert('xss')</script>";
    var sanitized = inputSanitizer.sanitizeInput(maliciousInput);
    assertFalse(sanitized.contains("<script>"), "Script tags should be removed");

    // Test SQL injection detection
    var sqlInjection = "'; DROP TABLE users; --";
    assertTrue(inputSanitizer.containsSqlInjection(sqlInjection), "Should detect SQL injection");

    // Test safe input
    var safeInput = "John Doe";
    assertTrue(inputSanitizer.isInputSafe(safeInput), "Safe input should be allowed");

    // Test username sanitization
    var username = "user<script>";
    var sanitizedUsername = inputSanitizer.sanitizeUsername(username);
    assertEquals("userltscriptgt", sanitizedUsername, "Username should be sanitized");

    // Test email sanitization
    var email = "USER@EXAMPLE.COM";
    var sanitizedEmail = inputSanitizer.sanitizeEmail(email);
    assertEquals("user@example.com", sanitizedEmail, "Email should be lowercase");
  }
}