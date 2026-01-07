package com.iqscaffold.userservice.infrastructure.config;

import java.nio.charset.StandardCharsets;

import com.iqscaffold.userservice.config.IqScaffoldProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Configuration for internationalization (i18n) support.
 * <p>
 * Configures message sources and locale resolution for multi-language support.
 * Uses a custom locale resolver that prioritizes user preferences over Accept-Language header.
 */
@Configuration
public class I18nConfig {

  private final IqScaffoldProperties properties;

  public I18nConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  /**
   * Configures the message source for internationalized messages.
   * <p>
   * Messages are loaded from configurable basename with UTF-8 encoding.
   *
   * @return configured MessageSource bean
   */
  @Bean
  public MessageSource messageSource() {
    var i18nConfig = properties.i18n();
    var messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename(i18nConfig.messageBasename());
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setUseCodeAsDefaultMessage(i18nConfig.useCodeAsDefaultMessage());
    messageSource.setFallbackToSystemLocale(i18nConfig.fallbackToSystemLocale());
    messageSource.setCacheSeconds((int) i18nConfig.messageCacheDuration().toSeconds());
    return messageSource;
  }

  /**
   * Configures locale resolution with priority order:
   * 1. X-User-Locale header (from gateway/user preferences)
   * 2. Accept-Language header
   * 3. Default locale (configurable)
   * <p>
   * Supports configurable locales from properties.
   *
   * @return configured LocaleResolver bean
   */
  @Bean
  public LocaleResolver localeResolver() {
    var i18nConfig = properties.i18n();
    var localeResolver = new UserPreferenceLocaleResolver();
    localeResolver.setDefaultLocale(i18nConfig.getDefaultLocaleObject());
    localeResolver.setSupportedLocales(i18nConfig.getSupportedLocaleObjects());
    return localeResolver;
  }
}
