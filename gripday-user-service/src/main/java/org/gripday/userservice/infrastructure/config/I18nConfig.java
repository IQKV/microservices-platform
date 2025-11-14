package org.gripday.userservice.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Configuration for internationalization (i18n) support.
 * <p>
 * Configures message sources and locale resolution for multi-language support.
 */
@Configuration
public class I18nConfig {

  /**
   * Configures the message source for internationalized messages.
   * <p>
   * Messages are loaded from i18n/messages*.properties files with UTF-8 encoding.
   *
   * @return configured MessageSource bean
   */
  @Bean
  public MessageSource messageSource() {
    var messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n/messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setUseCodeAsDefaultMessage(true);
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setCacheSeconds(3600); // Cache for 1 hour
    return messageSource;
  }

  /**
   * Configures locale resolution based on Accept-Language header.
   * <p>
   * Supports English (default), Spanish, and French locales.
   *
   * @return configured LocaleResolver bean
   */
  @Bean
  public LocaleResolver localeResolver() {
    var localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setDefaultLocale(Locale.ENGLISH);
    localeResolver.setSupportedLocales(List.of(
        Locale.ENGLISH,
        Locale.forLanguageTag("es"),
        Locale.FRENCH
    ));
    return localeResolver;
  }
}
