package org.gripday.userservice.presentation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation for password complexity validation. Uses Java 21 features for improved readability and performance.
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

  // Compiled patterns for better performance
  private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
  private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
  private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
  private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

  @Override
  public void initialize(ValidPassword constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null || password.trim().isEmpty()) {
      return false;
    }

    // Use var for improved readability
    var hasUppercase = UPPERCASE_PATTERN.matcher(password).matches();
    var hasLowercase = LOWERCASE_PATTERN.matcher(password).matches();
    var hasDigit = DIGIT_PATTERN.matcher(password).matches();
    var hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).matches();

    // Validate all requirements
    if (!hasUppercase) {
      addConstraintViolation(context, "Password must contain at least one uppercase letter");
      return false;
    }

    if (!hasLowercase) {
      addConstraintViolation(context, "Password must contain at least one lowercase letter");
      return false;
    }

    if (!hasDigit) {
      addConstraintViolation(context, "Password must contain at least one number");
      return false;
    }

    if (!hasSpecialChar) {
      addConstraintViolation(context, "Password must contain at least one special character");
      return false;
    }

    return true;
  }

  /**
   * Add custom constraint violation message using modern Java syntax.
   */
  private void addConstraintViolation(ConstraintValidatorContext context, String message) {
    if (context != null) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(message)
          .addConstraintViolation();
    }
  }
}