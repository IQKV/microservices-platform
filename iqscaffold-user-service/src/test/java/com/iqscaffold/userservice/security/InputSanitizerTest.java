package com.iqscaffold.userservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for InputSanitizer class.
 * Tests XSS prevention and SQL injection detection functionality.
 */
class InputSanitizerTest {

  private InputSanitizer inputSanitizer;

  @BeforeEach
  void setUp() {
    inputSanitizer = new InputSanitizer();
  }

  @Test
  void sanitizeInput_shouldReturnNullForNullInput() {
    assertNull(inputSanitizer.sanitizeInput(null));
  }

  @Test
  void sanitizeInput_shouldReturnEmptyForEmptyInput() {
    assertEquals("", inputSanitizer.sanitizeInput(""));
    assertEquals("", inputSanitizer.sanitizeInput("   "));
  }

  @Test
  void sanitizeInput_shouldEncodeHtmlEntities() {
    assertEquals("&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;",
        inputSanitizer.sanitizeInput("<script>alert(\"xss\")</script>"));
  }

  @Test
  void sanitizeInput_shouldRemoveScriptTags() {
    assertEquals("", inputSanitizer.sanitizeInput("<script>alert('xss')</script>"));
  }

  @Test
  void sanitizeInput_shouldRemoveJavaScriptProtocol() {
    assertEquals("alert('xss')", inputSanitizer.sanitizeInput("javascript:alert('xss')"));
  }

  @Test
  void sanitizeInput_shouldRemoveVBScriptProtocol() {
    assertEquals("msgbox('xss')", inputSanitizer.sanitizeInput("vbscript:msgbox('xss')"));
  }

  @Test
  void sanitizeInput_shouldRemoveEventHandlers() {
    assertEquals("=alert('xss')", inputSanitizer.sanitizeInput("onclick=alert('xss')"));
    assertEquals("=alert('xss')", inputSanitizer.sanitizeInput("onload=alert('xss')"));
    assertEquals("=alert('xss')", inputSanitizer.sanitizeInput("onerror=alert('xss')"));
  }

  @Test
  void sanitizeInput_shouldPreserveSafeText() {
    assertEquals("Hello World", inputSanitizer.sanitizeInput("Hello World"));
    assertEquals("user@example.com", inputSanitizer.sanitizeInput("user@example.com"));
  }

  @Test
  void sanitizeUsername_shouldRemoveNonAlphanumericChars() {
    assertEquals("john_doe", inputSanitizer.sanitizeUsername("john_doe"));
    assertEquals("john-doe", inputSanitizer.sanitizeUsername("john-doe"));
    assertEquals("johndoe", inputSanitizer.sanitizeUsername("john@doe"));
    assertEquals("johndoe", inputSanitizer.sanitizeUsername("john#doe"));
  }

  @Test
  void sanitizeUsername_shouldEncodeHtml() {
    assertEquals("john&amp;lt;script&amp;gt;doe",
        inputSanitizer.sanitizeUsername("john<script>doe"));
  }

  @Test
  void sanitizeEmail_shouldNormalizeAndEncode() {
    assertEquals("user@example.com", inputSanitizer.sanitizeEmail("USER@EXAMPLE.COM"));
    assertEquals("user&amp;lt;script&amp;gt;@example.com",
        inputSanitizer.sanitizeEmail("user<script>@example.com"));
  }

  @Test
  void sanitizeName_shouldPreserveSpacesButRemoveScripts() {
    assertEquals("John Doe", inputSanitizer.sanitizeName("John Doe"));
    assertEquals("", inputSanitizer.sanitizeName("<script>alert('xss')</script>"));
    assertEquals("alert('xss')", inputSanitizer.sanitizeName("javascript:alert('xss')"));
  }

  @Test
  void containsSqlInjection_shouldDetectSqlInjection() {
    assertTrue(inputSanitizer.containsSqlInjection("' OR '1'='1"));
    assertTrue(inputSanitizer.containsSqlInjection("'; DROP TABLE users; --"));
    assertTrue(inputSanitizer.containsSqlInjection("UNION SELECT * FROM users"));
    assertTrue(inputSanitizer.containsSqlInjection("1' OR '1'='1"));
  }

  @Test
  void containsSqlInjection_shouldAllowSafeInput() {
    assertFalse(inputSanitizer.containsSqlInjection("john.doe"));
    assertFalse(inputSanitizer.containsSqlInjection("user@example.com"));
    assertFalse(inputSanitizer.containsSqlInjection("John Doe"));
  }

  @Test
  void containsSqlInjection_shouldReturnFalseForNullOrEmpty() {
    assertFalse(inputSanitizer.containsSqlInjection(null));
    assertFalse(inputSanitizer.containsSqlInjection(""));
    assertFalse(inputSanitizer.containsSqlInjection("   "));
  }

  @Test
  void isInputSafe_shouldReturnTrueForSafeInput() {
    assertTrue(inputSanitizer.isInputSafe("Hello World"));
    assertTrue(inputSanitizer.isInputSafe("user@example.com"));
    assertTrue(inputSanitizer.isInputSafe("john_doe"));
  }

  @Test
  void isInputSafe_shouldReturnFalseForDangerousInput() {
    assertFalse(inputSanitizer.isInputSafe("<script>alert('xss')</script>"));
    assertFalse(inputSanitizer.isInputSafe("javascript:alert('xss')"));
    assertFalse(inputSanitizer.isInputSafe("' OR '1'='1"));
  }

  @Test
  void isInputSafe_shouldReturnTrueForNullOrEmpty() {
    assertTrue(inputSanitizer.isInputSafe(null));
    assertTrue(inputSanitizer.isInputSafe(""));
    assertTrue(inputSanitizer.isInputSafe("   "));
  }
}
