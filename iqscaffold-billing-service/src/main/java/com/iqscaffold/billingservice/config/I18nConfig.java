package com.iqscaffold.billingservice.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Configuration for internationalization (i18n) support in billing service.
 * <p>
 * Configures message sources and locale resolution for multi-language support.
 */
@Configuration
public class I18nConfig {

  private final IqScaffoldProperties properties;

  public I18nConfig(IqScaffoldProperties properties) {
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
   * Configures locale resolution based on Accept-Language header and X-User-Locale.
   * <p>
   * Supports configurable locales from properties.
   *
   * @return configured LocaleResolver bean
   */
  @Bean
  public LocaleResolver localeResolver() {
    var i18nConfig = properties.i18n();
    var localeResolver = new BillingServiceLocaleResolver();
    localeResolver.setDefaultLocale(i18nConfig.getDefaultLocaleObject());
    localeResolver.setSupportedLocales(i18nConfig.getSupportedLocaleObjects());
    return localeResolver;
  }
}