package com.iqscaffold.leadservice.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator for phone number format.
 * Validates that the phone contains only digits, spaces, hyphens, and parentheses.
 */
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

  private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9\\s\\-()]+$");

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true; // Let @NotBlank handle null/empty validation if required
    }
    return PHONE_PATTERN.matcher(value).matches();
  }
}
