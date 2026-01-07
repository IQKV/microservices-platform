package com.iqscaffold.billingservice.shared;

import java.util.Locale;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving internationalized messages.
 * Provides convenient methods to access i18n messages with locale resolution.
 */
@Service
public class MessageService {

  private final MessageSource messageSource;
  private final IqScaffoldProperties properties;

  public MessageService(final MessageSource messageSource, final IqScaffoldProperties properties) {
    this.messageSource = messageSource;
    this.properties = properties;
  }

  public String getMessage(final String code) {
    return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
  }

  public String getMessage(final String code, final Object[] args) {
    return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
  }

  public String getMessage(final String code, final Locale locale) {
    return messageSource.getMessage(code, null, locale);
  }

  public String getMessage(final String code, final Object[] args, final Locale locale) {
    return messageSource.getMessage(code, args, locale);
  }

  /**
   * Get message using a locale string, with fallback to default locale if unsupported.
   */
  public String getMessage(final String code, final String localeString) {
    var locale = parseLocale(localeString);
    return messageSource.getMessage(code, null, locale);
  }

  /**
   * Get message with arguments using a locale string, with fallback to default locale if unsupported.
   */
  public String getMessage(final String code, final Object[] args, final String localeString) {
    var locale = parseLocale(localeString);
    return messageSource.getMessage(code, args, locale);
  }

  /**
   * Parse locale string and validate against supported locales.
   * Falls back to default locale if unsupported.
   */
  private Locale parseLocale(String localeString) {
    if (localeString == null || localeString.isBlank()) {
      return properties.i18n().getDefaultLocaleObject();
    }

    var locale = Locale.forLanguageTag(localeString);
    var i18nConfig = properties.i18n();
    
    if (i18nConfig.isLocaleSupported(locale)) {
      return locale;
    }

    // Fallback to default locale
    return i18nConfig.getDefaultLocaleObject();
  }
}