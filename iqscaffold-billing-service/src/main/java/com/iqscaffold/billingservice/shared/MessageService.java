package com.iqscaffold.billingservice.shared;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for resolving localized messages.
 */
@Service
public class MessageService {

  private final MessageSource messageSource;

  public MessageService(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Get a message for the given key and args using the current locale.
   */
  public String getMessage(String key, Object... args) {
    return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
  }

  /**
   * Get a message for the given key and args using the given locale.
   */
  public String getMessage(String key, Locale locale, Object... args) {
    return messageSource.getMessage(key, args, locale);
  }
}
