package org.gripday.authservice.presentation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation for username format validation. Uses Java 21 features and security best practices.
 */
public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

  // Username pattern: alphanumeric, underscore, hyphen, but not starting/ending with special chars
  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]*[a-zA-Z0-9]$|^[a-zA-Z0-9]$");

  // Reserved usernames for security
  private static final java.util.Set<String> RESERVED_USERNAMES = java.util.Set.of(
      "admin", "administrator", "root", "system", "user", "guest", "test", "demo",
      "api", "www", "mail", "email", "support", "help", "info", "contact",
      "null", "undefined", "anonymous", "public", "private", "internal"
  );

  @Override
  public void initialize(ValidUsername constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(String username, ConstraintValidatorContext context) {
    if (username == null || username.trim().isEmpty()) {
      return false;
    }

    var trimmedUsername = username.trim().toLowerCase();

    // Check pattern match
    if (!USERNAME_PATTERN.matcher(username).matches()) {
      addConstraintViolation(context, "Username format is invalid");
      return false;
    }

    // Check for reserved usernames
    if (RESERVED_USERNAMES.contains(trimmedUsername)) {
      addConstraintViolation(context, "Username is reserved and cannot be used");
      return false;
    }

    // Check for consecutive special characters
    if (username.contains("__") || username.contains("--") || username.contains("_-") || username.contains("-_")) {
      addConstraintViolation(context, "Username cannot contain consecutive special characters");
      return false;
    }

    return true;
  }

  /**
   * Add custom constraint violation message.
   */
  private void addConstraintViolation(ConstraintValidatorContext context, String message) {
    if (context != null) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(message)
          .addConstraintViolation();
    }
  }
}