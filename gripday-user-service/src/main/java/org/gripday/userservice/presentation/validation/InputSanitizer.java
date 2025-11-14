package org.gripday.userservice.presentation.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Utility class for input sanitization and XSS prevention. Uses Java 21 features for improved performance and readability.
 */
@Component
public class InputSanitizer {

  // Patterns for potentially dangerous content
  private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
  private static final Pattern VBSCRIPT_PATTERN = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
  private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload[^=]*=", Pattern.CASE_INSENSITIVE);
  private static final Pattern ONERROR_PATTERN = Pattern.compile("onerror[^=]*=", Pattern.CASE_INSENSITIVE);
  private static final Pattern ONCLICK_PATTERN = Pattern.compile("onclick[^=]*=", Pattern.CASE_INSENSITIVE);

  // SQL injection patterns
  private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
      "('|(\\-\\-)|(;)|(\\|)|(\\*)|(%)|(union)|(select)|(insert)|(delete)|(update)|(drop)|(create)|(alter)|(exec)|(execute))",
      Pattern.CASE_INSENSITIVE
  );

  /**
   * Sanitize user input to prevent XSS attacks. Uses modern Java syntax for improved readability.
   */
  public String sanitizeInput(String input) {
    if (input == null || input.trim().isEmpty()) {
      return input;
    }

    var sanitized = input.trim();

    // HTML encode to prevent XSS
    sanitized = HtmlUtils.htmlEscape(sanitized);

    // Remove potentially dangerous patterns
    sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = VBSCRIPT_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = ONERROR_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = ONCLICK_PATTERN.matcher(sanitized).replaceAll("");

    return sanitized;
  }

  /**
   * Sanitize username input with additional restrictions.
   */
  public String sanitizeUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      return username;
    }

    var sanitized = username.trim();

    // Remove any HTML tags and encode
    sanitized = HtmlUtils.htmlEscape(sanitized);

    // Remove any non-alphanumeric characters except underscore and hyphen
    sanitized = sanitized.replaceAll("[^a-zA-Z0-9_-]", "");

    return sanitized;
  }

  /**
   * Sanitize email input.
   */
  public String sanitizeEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      return email;
    }

    var sanitized = email.trim().toLowerCase(java.util.Locale.ROOT);

    // HTML encode
    sanitized = HtmlUtils.htmlEscape(sanitized);

    return sanitized;
  }

  /**
   * Sanitize name fields (firstName, lastName).
   */
  public String sanitizeName(String name) {
    if (name == null || name.trim().isEmpty()) {
      return name;
    }

    var sanitized = name.trim();

    // HTML encode
    sanitized = HtmlUtils.htmlEscape(sanitized);

    // Remove potentially dangerous patterns but allow spaces and common name characters
    sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

    return sanitized;
  }

  /**
   * Check for potential SQL injection attempts.
   */
  public boolean containsSqlInjection(String input) {
    if (input == null || input.trim().isEmpty()) {
      return false;
    }

    return SQL_INJECTION_PATTERN.matcher(input.toLowerCase(java.util.Locale.ROOT)).find();
  }

  /**
   * Validate that input doesn't contain dangerous patterns.
   */
  public boolean isInputSafe(String input) {
    if (input == null || input.trim().isEmpty()) {
      return true;
    }

    var lowerInput = input.toLowerCase(java.util.Locale.ROOT);

    return !SCRIPT_PATTERN.matcher(lowerInput).find()
        && !JAVASCRIPT_PATTERN.matcher(lowerInput).find()
        && !VBSCRIPT_PATTERN.matcher(lowerInput).find()
        && !containsSqlInjection(input);
  }
}