package com.iqscaffold.contactservice.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class I18nConfig {

  private final IqScaffoldProperties properties;

  public I18nConfig(final IqScaffoldProperties properties) {
    this.properties = properties;
  }

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:" + properties.i18n().messageBasename());
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setCacheSeconds((int) properties.i18n().messageCacheDuration().getSeconds());
    messageSource.setFallbackToSystemLocale(properties.i18n().fallbackToSystemLocale());
    messageSource.setUseCodeAsDefaultMessage(properties.i18n().useCodeAsDefaultMessage());
    return messageSource;
  }

  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setDefaultLocale(Locale.forLanguageTag(properties.i18n().defaultLocale()));
    return localeResolver;
  }
}