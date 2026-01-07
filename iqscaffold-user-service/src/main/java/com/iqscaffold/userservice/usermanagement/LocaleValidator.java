package com.iqscaffold.userservice.usermanagement;

import com.iqscaffold.userservice.config.IqScaffoldProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for supported locales based on configuration.
 */
public class LocaleValidator implements ConstraintValidator<ValidLocale, String> {

    private IqScaffoldProperties properties;

    public LocaleValidator(IqScaffoldProperties properties) {
        this.properties = properties;
    }

    @Override
    public void initialize(ValidLocale constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String locale, ConstraintValidatorContext context) {
        if (locale == null || locale.isBlank()) {
            return false;
        }

        var i18nConfig = properties.i18n();
        boolean isValid = i18nConfig.isLocaleSupported(locale);

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Locale must be one of: " + String.join(", ", i18nConfig.supportedLocales())
            ).addConstraintViolation();
        }

        return isValid;
    }
}