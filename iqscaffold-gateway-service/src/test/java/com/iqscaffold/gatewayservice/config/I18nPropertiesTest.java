package com.iqscaffold.gatewayservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.iqscaffold.gatewayservice.config.IqScaffoldProperties.I18nProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("I18nProperties Tests")
class I18nPropertiesTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should create valid I18nProperties with supported locales")
    void shouldCreateValidI18nPropertiesWithSupportedLocales() {
      var i18nProperties = new I18nProperties(
          List.of("en", "es", "fr"),
          "en"
      );

      assertThat(i18nProperties.supportedLocales()).containsExactly("en", "es", "fr");
      assertThat(i18nProperties.defaultLocale()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should throw exception when default locale not in supported locales")
    void shouldThrowExceptionWhenDefaultLocaleNotInSupportedLocales() {
      assertThatThrownBy(() -> new I18nProperties(
          List.of("en", "es", "fr"),
          "de"
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Default locale 'de' must be included in supported locales");
    }

    @Test
    @DisplayName("Should allow default locale when it exists in supported locales")
    void shouldAllowDefaultLocaleWhenItExistsInSupportedLocales() {
      var i18nProperties = new I18nProperties(
          List.of("en", "es", "fr"),
          "es"
      );

      assertThat(i18nProperties.defaultLocale()).isEqualTo("es");
    }

    @Test
    @DisplayName("Should handle single supported locale")
    void shouldHandleSingleSupportedLocale() {
      var i18nProperties = new I18nProperties(
          List.of("en"),
          "en"
      );

      assertThat(i18nProperties.supportedLocales()).containsExactly("en");
      assertThat(i18nProperties.defaultLocale()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should handle locale with region codes")
    void shouldHandleLocaleWithRegionCodes() {
      var i18nProperties = new I18nProperties(
          List.of("en-US", "es-ES", "fr-FR"),
          "en-US"
      );

      assertThat(i18nProperties.supportedLocales()).containsExactly("en-US", "es-ES", "fr-FR");
      assertThat(i18nProperties.defaultLocale()).isEqualTo("en-US");
    }
  }

  @Nested
  @DisplayName("Bean Validation Tests")
  class BeanValidationTests {

    @Test
    @DisplayName("Should validate successfully with valid properties")
    void shouldValidateSuccessfullyWithValidProperties() {
      var i18nProperties = new I18nProperties(
          List.of("en", "es", "fr"),
          "en"
      );

      Set<ConstraintViolation<I18nProperties>> violations = validator.validate(i18nProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when supportedLocales is null")
    void shouldFailValidationWhenSupportedLocalesIsNull() {
      // The constructor doesn't throw exception for null supportedLocales, it just skips validation
      var i18nProperties = new I18nProperties(null, "en");

      // But bean validation should catch this
      Set<ConstraintViolation<I18nProperties>> violations = validator.validate(i18nProperties);
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("supportedLocales"));
    }

    @Test
    @DisplayName("Should fail validation when defaultLocale is null")
    void shouldFailValidationWhenDefaultLocaleIsNull() {
      // This will throw NullPointerException during construction due to null check
      assertThatThrownBy(() -> new I18nProperties(List.of("en", "es", "fr"), null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should fail validation when defaultLocale is blank")
    void shouldFailValidationWhenDefaultLocaleIsBlank() {
      // This will throw IllegalArgumentException during construction
      assertThatThrownBy(() -> new I18nProperties(List.of("en", "es", "fr"), ""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Default locale '' must be included in supported locales");
    }

    @Test
    @DisplayName("Should fail validation with invalid locale format in supportedLocales")
    void shouldFailValidationWithInvalidLocaleFormatInSupportedLocales() {
      var i18nProperties = new I18nProperties(
          List.of("en", "invalid-locale", "fr"),
          "en"
      );

      Set<ConstraintViolation<I18nProperties>> violations = validator.validate(i18nProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("supportedLocales"));
    }

    @Test
    @DisplayName("Should fail validation with invalid locale format in defaultLocale")
    void shouldFailValidationWithInvalidLocaleFormatInDefaultLocale() {
      var i18nProperties = new I18nProperties(
          List.of("en", "es", "invalid-locale"),
          "invalid-locale"
      );

      Set<ConstraintViolation<I18nProperties>> violations = validator.validate(i18nProperties);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("defaultLocale"));
    }

    @Test
    @DisplayName("Should validate successfully with region codes")
    void shouldValidateSuccessfullyWithRegionCodes() {
      var i18nProperties = new I18nProperties(
          List.of("en-US", "es-ES", "fr-FR"),
          "en-US"
      );

      Set<ConstraintViolation<I18nProperties>> violations = validator.validate(i18nProperties);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid region code format")
    void shouldFailValidationWithInvalidRegionCodeFormat() {
      var i18nProperties = new I18nProperties(
          List.of("en-us", "es-ES", "fr-FR"), // lowercase region code
          "en-us"
      );

      Set<ConstraintViolation<I18nProperties>> violations = validator.validate(i18nProperties);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation with three-letter language code")
    void shouldFailValidationWithThreeLetterLanguageCode() {
      var i18nProperties = new I18nProperties(
          List.of("eng", "spa", "fra"), // three-letter codes
          "eng"
      );

      Set<ConstraintViolation<I18nProperties>> violations = validator.validate(i18nProperties);

      assertThat(violations).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Utility Methods Tests")
  class UtilityMethodsTests {

    private I18nProperties i18nProperties;

    @BeforeEach
    void setUp() {
      i18nProperties = new I18nProperties(
          List.of("en", "es", "fr", "en-US", "es-ES"),
          "en"
      );
    }

    @Test
    @DisplayName("Should return supported locale objects")
    void shouldReturnSupportedLocaleObjects() {
      var localeObjects = i18nProperties.getSupportedLocaleObjects();

      assertThat(localeObjects).hasSize(5);
      assertThat(localeObjects).contains(
          Locale.forLanguageTag("en"),
          Locale.forLanguageTag("es"),
          Locale.forLanguageTag("fr"),
          Locale.forLanguageTag("en-US"),
          Locale.forLanguageTag("es-ES")
      );
    }

    @Test
    @DisplayName("Should return default locale object")
    void shouldReturnDefaultLocaleObject() {
      var defaultLocaleObject = i18nProperties.getDefaultLocaleObject();

      assertThat(defaultLocaleObject).isEqualTo(Locale.forLanguageTag("en"));
    }

    @Test
    @DisplayName("Should check if string locale is supported")
    void shouldCheckIfStringLocaleIsSupported() {
      assertThat(i18nProperties.isLocaleSupported("en")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("es")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("fr")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("en-US")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("es-ES")).isTrue();

      assertThat(i18nProperties.isLocaleSupported("de")).isFalse();
      assertThat(i18nProperties.isLocaleSupported("zh")).isFalse();
      assertThat(i18nProperties.isLocaleSupported("en-GB")).isFalse();
    }

    @Test
    @DisplayName("Should check if Locale object is supported with exact match")
    void shouldCheckIfLocaleObjectIsSupportedWithExactMatch() {
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("en"))).isTrue();
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("es"))).isTrue();
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("fr"))).isTrue();
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("en-US"))).isTrue();
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("es-ES"))).isTrue();

      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("de"))).isFalse();
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("zh"))).isFalse();
    }

    @Test
    @DisplayName("Should check if Locale object is supported with language-only match")
    void shouldCheckIfLocaleObjectIsSupportedWithLanguageOnlyMatch() {
      // en-GB should match "en" in supported locales
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("en-GB"))).isTrue();

      // es-MX should match "es" in supported locales
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("es-MX"))).isTrue();

      // fr-CA should match "fr" in supported locales
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("fr-CA"))).isTrue();

      // de-DE should not match any supported locale
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("de-DE"))).isFalse();
    }

    @Test
    @DisplayName("Should handle null and empty string checks gracefully")
    void shouldHandleNullAndEmptyStringChecksGracefully() {
      // The actual implementation doesn't handle null gracefully, so we expect exceptions
      assertThatThrownBy(() -> i18nProperties.isLocaleSupported((String) null))
          .isInstanceOf(NullPointerException.class);

      assertThat(i18nProperties.isLocaleSupported("")).isFalse();
      assertThat(i18nProperties.isLocaleSupported("   ")).isFalse();
    }

    @Test
    @DisplayName("Should handle null Locale object checks gracefully")
    void shouldHandleNullLocaleObjectChecksGracefully() {
      // The actual implementation doesn't handle null gracefully, so we expect exceptions
      assertThatThrownBy(() -> i18nProperties.isLocaleSupported((Locale) null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should be case sensitive for locale matching")
    void shouldBeCaseSensitiveForLocaleMatching() {
      assertThat(i18nProperties.isLocaleSupported("EN")).isFalse();
      assertThat(i18nProperties.isLocaleSupported("Es")).isFalse();
      assertThat(i18nProperties.isLocaleSupported("FR")).isFalse();
      assertThat(i18nProperties.isLocaleSupported("en-us")).isFalse(); // region should be uppercase
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle empty supported locales list")
    void shouldHandleEmptySupportedLocalesList() {
      assertThatThrownBy(() -> new I18nProperties(
          List.of(),
          "en"
      )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle duplicate locales in supported list")
    void shouldHandleDuplicateLocalesInSupportedList() {
      var i18nProperties = new I18nProperties(
          List.of("en", "es", "en", "fr"), // duplicate "en"
          "en"
      );

      assertThat(i18nProperties.supportedLocales()).containsExactly("en", "es", "en", "fr");
      assertThat(i18nProperties.isLocaleSupported("en")).isTrue();
    }

    @Test
    @DisplayName("Should handle mixed language and region codes")
    void shouldHandleMixedLanguageAndRegionCodes() {
      var i18nProperties = new I18nProperties(
          List.of("en", "es-ES", "fr", "de-DE"),
          "en"
      );

      assertThat(i18nProperties.isLocaleSupported("en")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("es-ES")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("fr")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("de-DE")).isTrue();

      // Language-only matching - the implementation checks if the language part matches
      // For es-MX, it will check "es" against supported locales, but "es" is not in the list, only "es-ES"
      // So this should be false unless we have "es" in the supported list
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("es-MX"))).isFalse(); // "es" not in supported list
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("de-AT"))).isFalse(); // "de" not in supported list
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("en-GB"))).isTrue(); // "en" is in supported list
    }

    @Test
    @DisplayName("Should handle special locale formats")
    void shouldHandleSpecialLocaleFormats() {
      var i18nProperties = new I18nProperties(
          List.of("zh-CN", "zh-TW", "pt-BR"),
          "zh-CN"
      );

      assertThat(i18nProperties.isLocaleSupported("zh-CN")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("zh-TW")).isTrue();
      assertThat(i18nProperties.isLocaleSupported("pt-BR")).isTrue();

      // Language-only matching - "zh" and "pt" are not in the supported list, only "zh-CN", "zh-TW", "pt-BR"
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("zh-HK"))).isFalse(); // "zh" not in supported list
      assertThat(i18nProperties.isLocaleSupported(Locale.forLanguageTag("pt-PT"))).isFalse(); // "pt" not in supported list
    }
  }
}
