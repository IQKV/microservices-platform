package com.iqscaffold.billingservice.shared;

import java.util.Locale;

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

  public MessageService(final MessageSource messageSource) {
    this.messageSource = messageSource;
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
}