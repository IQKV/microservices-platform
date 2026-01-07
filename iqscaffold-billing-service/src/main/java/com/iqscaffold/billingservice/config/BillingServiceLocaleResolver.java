package com.iqscaffold.billingservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Custom locale resolver for billing service that prioritizes X-User-Locale header.
 * <p>
 * Resolution priority:
 * 1. X-User-Locale header (from gateway/user preferences)
 * 2. Accept-Language header
 * 3. Default locale
 */
public class BillingServiceLocaleResolver extends AcceptHeaderLocaleResolver {

  private static final String USER_LOCALE_HEADER = "X-User-Locale";

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    try {
      // Priority 1: Check X-User-Locale header from gateway
      String userLocaleHeader = request.getHeader(USER_LOCALE_HEADER);
      if (StringUtils.hasText(userLocaleHeader)) {
        try {
          Locale userLocale = Locale.forLanguageTag(userLocaleHeader);
          // Validate the locale is well-formed and supported
          if (isValidLocale(userLocale, userLocaleHeader) && isSupportedLocale(userLocale)) {
            return userLocale;
          }
        } catch (final Exception e) {
          // Log and fall back to Accept-Language
        }
      }

      // Priority 2: Fall back to request locale (from Accept-Language header)
      Locale requestLocale = request.getLocale();
      if (requestLocale != null) {
        return requestLocale;
      }
    } catch (final Exception e) {
      // Handle any unexpected exceptions gracefully
    }

    // Priority 3: Fall back to default locale
    Locale defaultLocale = getDefaultLocale();
    return defaultLocale != null ? defaultLocale : Locale.getDefault();
  }

  private boolean isValidLocale(Locale locale, String originalTag) {
    // Check if locale has a valid language tag
    // Locale.forLanguageTag() can create locales with invalid tags
    if (locale == null || locale.getLanguage().isEmpty()) {
      return false;
    }
    
    // Check if the language code is a valid ISO 639 language
    // Invalid language codes will have the same value as the display language
    // For valid codes, getDisplayLanguage() returns a human-readable name
    String language = locale.getLanguage();
    String displayLanguage = locale.getDisplayLanguage(Locale.ENGLISH);
    
    // If display language equals the language code, it's likely invalid
    // Valid languages have different display names (e.g., "en" -> "English")
    if (language.equals(displayLanguage)) {
      return false;
    }
    
    return true;
  }

  private boolean isSupportedLocale(Locale locale) {
    List<Locale> supportedLocales = getSupportedLocales();
    if (supportedLocales == null || supportedLocales.isEmpty()) {
      return true; // If no supported locales configured, accept all
    }

    // Check exact match
    if (supportedLocales.contains(locale)) {
      return true;
    }

    // Check language-only match
    return supportedLocales.stream()
        .anyMatch(supported -> supported.getLanguage().equals(locale.getLanguage()));
  }

  @Override
  public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    // This resolver is read-only - locale changes should be handled through user preferences
    throw new UnsupportedOperationException(
        "Cannot change locale via BillingServiceLocaleResolver - update user preferences instead");
  }
}
