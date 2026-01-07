package com.iqscaffold.userservice.shared;

import java.util.Locale;

import com.iqscaffold.userservice.usermanagement.User;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for retrieving internationalized messages with user preference support.
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

  /**
   * Get message using user's preferred locale.
   * Falls back to current locale context if user locale is not available.
   */
  public String getMessage(final String code, final User user) {
    var userLocale = getUserLocale(user);
    return messageSource.getMessage(code, null, userLocale);
  }

  /**
   * Get message with arguments using user's preferred locale.
   * Falls back to current locale context if user locale is not available.
   */
  public String getMessage(final String code, final Object[] args, final User user) {
    var userLocale = getUserLocale(user);
    return messageSource.getMessage(code, args, userLocale);
  }

  /**
   * Determine user's preferred locale with fallback chain:
   * 1. User's stored preference (User.preferredLocale)
   * 2. Current locale context (from request)
   * 3. Default locale (English)
   */
  public Locale getUserLocale(final User user) {
    if (user != null && StringUtils.hasText(user.getPreferredLocale())) {
      try {
        return Locale.forLanguageTag(user.getPreferredLocale());
      } catch (final Exception e) {
        // Fall back to context locale if user locale is invalid
      }
    }

    var contextLocale = LocaleContextHolder.getLocale();
    return contextLocale != null ? contextLocale : Locale.ENGLISH;
  }
}
