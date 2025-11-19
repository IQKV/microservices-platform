package org.gripday.userservice.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for PasswordValidator class.
 * Tests password complexity validation requirements.
 */
class PasswordValidatorTest {

  private PasswordValidator passwordValidator;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    passwordValidator = new PasswordValidator();
    passwordValidator.initialize(null);
    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    when(violationBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Test
  void isValid_shouldReturnFalseForNullPassword() {
    assertFalse(passwordValidator.isValid(null, context));
  }

  @Test
  void isValid_shouldReturnFalseForEmptyPassword() {
    assertFalse(passwordValidator.isValid("", context));
    assertFalse(passwordValidator.isValid("   ", context));
  }

  @Test
  void isValid_shouldReturnFalseForPasswordWithoutUppercase() {
    assertFalse(passwordValidator.isValid("password123!", context));
    assertFalse(passwordValidator.isValid("lowercase!", context));
  }

  @Test
  void isValid_shouldReturnFalseForPasswordWithoutLowercase() {
    assertFalse(passwordValidator.isValid("PASSWORD123!", context));
    assertFalse(passwordValidator.isValid("UPPERCASE!", context));
  }

  @Test
  void isValid_shouldReturnFalseForPasswordWithoutDigit() {
    assertFalse(passwordValidator.isValid("Password!", context));
    assertFalse(passwordValidator.isValid("NoDigitsHere!", context));
  }

  @Test
  void isValid_shouldReturnFalseForPasswordWithoutSpecialChar() {
    assertFalse(passwordValidator.isValid("Password123", context));
    assertFalse(passwordValidator.isValid("NoSpecialChars", context));
  }

  @Test
  void isValid_shouldReturnTrueForValidPassword() {
    assertTrue(passwordValidator.isValid("Password123!", context));
    assertTrue(passwordValidator.isValid("MySecureP@ssw0rd", context));
    assertTrue(passwordValidator.isValid("ValidPass123$", context));
  }

  @Test
  void isValid_shouldHandleVariousSpecialCharacters() {
    assertTrue(passwordValidator.isValid("Password123@", context));
    assertTrue(passwordValidator.isValid("Password123#", context));
    assertTrue(passwordValidator.isValid("Password123$", context));
    assertTrue(passwordValidator.isValid("Password123%", context));
    assertTrue(passwordValidator.isValid("Password123^", context));
    assertTrue(passwordValidator.isValid("Password123&", context));
    assertTrue(passwordValidator.isValid("Password123*", context));
    assertTrue(passwordValidator.isValid("Password123(", context));
    assertTrue(passwordValidator.isValid("Password123)", context));
    assertTrue(passwordValidator.isValid("Password123-", context));
    assertTrue(passwordValidator.isValid("Password123+", context));
    assertTrue(passwordValidator.isValid("Password123=", context));
  }

  @Test
  void isValid_shouldHandleEdgeCases() {
    assertTrue(passwordValidator.isValid("A1!aaaaa", context)); // Minimum valid length
    assertTrue(passwordValidator.isValid("P@ssW0rd", context));
    assertFalse(passwordValidator.isValid("A1!", context)); // Too short
  }

  @Test
  void isValid_shouldHandleMixedCasePasswords() {
    assertTrue(passwordValidator.isValid("PaSsWoRd123!", context));
    assertTrue(passwordValidator.isValid("MIXEDcase123@", context));
  }

  @Test
  void isValid_shouldHandlePasswordsWithMultipleSpecialChars() {
    assertTrue(passwordValidator.isValid("Password123!@#", context));
    assertTrue(passwordValidator.isValid("Secure$P@ssw0rd", context));
  }
}
