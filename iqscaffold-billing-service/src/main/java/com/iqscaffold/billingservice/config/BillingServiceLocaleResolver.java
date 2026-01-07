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
    // Priority 1: Check X-User-Locale header from gateway
    String userLocaleHeader = request.getHeader(USER_LOCALE_HEADER);
    if (StringUtils.hasText(userLocaleHeader)) {
      try {
        Locale userLocale = Locale.forLanguageTag(userLocaleHeader);
        if (isSupportedLocale(userLocale)) {
          return userLocale;
        }
      } catch (final Exception e) {
        // Log and fall back to Accept-Language
      }
    }

    // Priority 2: Fall back to Accept-Language header resolution
    return super.resolveLocale(request);
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
