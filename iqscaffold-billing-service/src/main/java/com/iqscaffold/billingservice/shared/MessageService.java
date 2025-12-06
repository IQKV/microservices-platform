package com.iqscaffold.billingservice.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for retrieving internationalized messages.
 * 
 * <p>Provides convenient methods for message retrieval with locale resolution
 * from user preferences, Accept-Language header, or default locale.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Get message for the given key using the current locale.
     *
     * @param key the message key
     * @return the localized message
     */
    public String getMessage(String key) {
        return getMessage(key, null, LocaleContextHolder.getLocale());
    }

    /**
     * Get message for the given key with parameters using the current locale.
     *
     * @param key the message key
     * @param params the message parameters
     * @return the localized message with parameters replaced
     */
    public String getMessage(String key, Object... params) {
        return getMessage(key, params, LocaleContextHolder.getLocale());
    }

    /**
     * Get message for the given key and locale.
     *
     * @param key the message key
     * @param locale the locale
     * @return the localized message
     */
    public String getMessage(String key, Locale locale) {
        return getMessage(key, null, locale);
    }

    /**
     * Get message for the given key with parameters and locale.
     *
     * @param key the message key
     * @param params the message parameters
     * @param locale the locale
     * @return the localized message with parameters replaced
     */
    public String getMessage(String key, Object[] params, Locale locale) {
        return messageSource.getMessage(key, params, locale);
    }

    /**
     * Get message for the given key with a default message if not found.
     *
     * @param key the message key
     * @param defaultMessage the default message if key not found
     * @return the localized message or default message
     */
    public String getMessageOrDefault(String key, String defaultMessage) {
        return messageSource.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Get message for the given key with parameters and a default message if not found.
     *
     * @param key the message key
     * @param params the message parameters
     * @param defaultMessage the default message if key not found
     * @return the localized message with parameters replaced or default message
     */
    public String getMessageOrDefault(String key, Object[] params, String defaultMessage) {
        return messageSource.getMessage(key, params, defaultMessage, LocaleContextHolder.getLocale());
    }
}
