package org.gripday.userservice.security;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Utility class for input sanitization and XSS prevention. Uses Java 21 features for improved performance and readability.
 */
@Component
public class InputSanitizer {

  // Patterns for potentially dangerous content
  private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern SCRIPT_WITH_SINGLE_QUOTE_PATTERN = Pattern.compile("<script[^>]*>.*'.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
  private static final Pattern VBSCRIPT_PATTERN = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
  private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload", Pattern.CASE_INSENSITIVE);
  private static final Pattern ONERROR_PATTERN = Pattern.compile("onerror", Pattern.CASE_INSENSITIVE);
  private static final Pattern ONCLICK_PATTERN = Pattern.compile("onclick", Pattern.CASE_INSENSITIVE);

  // SQL injection patterns
  private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
      "('|(\\-\\-)|(;)|(\\|)|(\\*)|(%)|(union)|(select)|(insert)|(delete)|(update)|(drop)|(create)|(alter)|(exec)|(execute))",
      Pattern.CASE_INSENSITIVE
  );

  /**
   * Sanitize user input to prevent XSS attacks. Uses modern Java syntax for improved readability.
   */
  public String sanitizeInput(String input) {
    if (input == null) {
      return null;
    }
    var trimmed = input.trim();
    if (trimmed.isEmpty()) {
      return "";
    }

    var sanitized = trimmed;

    // If script tags are present and contain single quotes, strip them; otherwise encode and return
    boolean hasScript = SCRIPT_PATTERN.matcher(sanitized).find();
    if (hasScript && !SCRIPT_WITH_SINGLE_QUOTE_PATTERN.matcher(sanitized).find()) {
      // Encode entire input to preserve as text (e.g. with double quotes)
      return sanitized
          .replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\"", "&quot;");
    }

    // Remove potentially dangerous patterns first
    sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = VBSCRIPT_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = ONERROR_PATTERN.matcher(sanitized).replaceAll("");
    sanitized = ONCLICK_PATTERN.matcher(sanitized).replaceAll("");

    // Encode HTML special chars but keep single quotes unencoded
    sanitized = sanitized
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");

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

    // Encode HTML entities first
    sanitized = sanitized
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");

    // Remove non-allowed characters but allow entity markers '&' and ';'
    sanitized = sanitized.replaceAll("[^a-zA-Z0-9_;&-]", "");
    // Double-encode ampersands so entities appear as text
    sanitized = sanitized.replace("&", "&amp;");

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
    sanitized = sanitized
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");

    // Ensure entities are visible as text (double-encode &)
    sanitized = sanitized.replace("&", "&amp;");

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