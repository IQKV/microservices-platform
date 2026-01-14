package com.iqscaffold.leadservice.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/**
 * Validator for lead source values.
 * Validates that the source is one of the predefined valid sources.
 */
public class LeadSourceValidator implements ConstraintValidator<ValidLeadSource, String> {

  private static final Set<String> VALID_SOURCES = Set.of(
      "WEBSITE",
      "REFERRAL",
      "COLD_CALL",
      "EMAIL_CAMPAIGN",
      "SOCIAL_MEDIA",
      "TRADE_SHOW",
      "PARTNER",
      "OTHER"
  );

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    if (value == null) {
      return true; // Let @NotBlank handle null validation
    }
    return VALID_SOURCES.contains(value.toUpperCase());
  }
}
