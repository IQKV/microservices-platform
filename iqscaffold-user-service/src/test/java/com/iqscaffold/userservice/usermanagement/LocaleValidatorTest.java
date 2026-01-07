package com.iqscaffold.userservice.usermanagement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import jakarta.validation.ConstraintValidatorContext;
import java.time.Duration;
import java.util.List;

import com.iqscaffold.userservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for LocaleValidator class.
 * Tests locale validation against configured supported locales.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Locale Validator Tests")
class LocaleValidatorTest {

  private LocaleValidator localeValidator;

  @Mock
  private IqScaffoldProperties properties;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

  private IqScaffoldProperties.I18n i18nConfig;

  @BeforeEach
  void setUp() {
    // Setup i18n configuration with supported locales
    i18nConfig = new IqScaffoldProperties.I18n(
        List.of("en", "es", "fr"),
        "en",
        "i18n/messages",
        Duration.ofHours(1),
        false,
        true
    );

    lenient().when(properties.i18n()).thenReturn(i18nConfig);
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);

    localeValidator = new LocaleValidator(properties);
    localeValidator.initialize(null);
  }

  @Test
  @DisplayName("Should return false for null locale")
  void shouldReturnFalseForNullLocale() {
    assertFalse(localeValidator.isValid(null, context));
  }

  @Test
  @DisplayName("Should return false for empty locale")
  void shouldReturnFalseForEmptyLocale() {
    assertFalse(localeValidator.isValid("", context));
  }

  @Test
  @DisplayName("Should return false for blank locale")
  void shouldReturnFalseForBlankLocale() {
    assertFalse(localeValidator.isValid("   ", context));
  }

  @Test
  @DisplayName("Should return true for supported locale 'en'")
  void shouldReturnTrueForSupportedLocaleEn() {
    assertTrue(localeValidator.isValid("en", context));
  }

  @Test
  @DisplayName("Should return true for supported locale 'es'")
  void shouldReturnTrueForSupportedLocaleEs() {
    assertTrue(localeValidator.isValid("es", context));
  }

  @Test
  @DisplayName("Should return true for supported locale 'fr'")
  void shouldReturnTrueForSupportedLocaleFr() {
    assertTrue(localeValidator.isValid("fr", context));
  }

  @Test
  @DisplayName("Should return false for unsupported locale")
  void shouldReturnFalseForUnsupportedLocale() {
    assertFalse(localeValidator.isValid("de", context));
  }

  @Test
  @DisplayName("Should return false for unsupported locale 'zh'")
  void shouldReturnFalseForUnsupportedLocaleZh() {
    assertFalse(localeValidator.isValid("zh", context));
  }

  @Test
  @DisplayName("Should disable default constraint violation for invalid locale")
  void shouldDisableDefaultConstraintViolationForInvalidLocale() {
    localeValidator.isValid("de", context);
    verify(context).disableDefaultConstraintViolation();
  }

  @Test
  @DisplayName("Should build custom constraint violation message for invalid locale")
  void shouldBuildCustomConstraintViolationMessageForInvalidLocale() {
    localeValidator.isValid("de", context);
    verify(context).buildConstraintViolationWithTemplate("Locale must be one of: en, es, fr");
  }

  @Test
  @DisplayName("Should add constraint violation for invalid locale")
  void shouldAddConstraintViolationForInvalidLocale() {
    localeValidator.isValid("de", context);
    verify(violationBuilder).addConstraintViolation();
  }

  @Test
  @DisplayName("Should handle locale with region code")
  void shouldHandleLocaleWithRegionCode() {
    // en-US is not in the supported list, so it should be invalid
    assertFalse(localeValidator.isValid("en-US", context));
  }

  @Test
  @DisplayName("Should be case sensitive for locale codes")
  void shouldBeCaseSensitiveForLocaleCodes() {
    // EN is not the same as en
    assertFalse(localeValidator.isValid("EN", context));
  }
}
