/**
 * Internationalization (i18n) support for the Gripday User Service.
 * <p>
 * This package provides utilities for multi-language support including:
 * <ul>
 *   <li>MessageService for retrieving localized messages</li>
 *   <li>Locale resolution based on user preferences or Accept-Language header</li>
 *   <li>Integration with email templates for localized communications</li>
 * </ul>
 * <p>
 * Supported locales: English (en), Spanish (es), French (fr)
 * <p>
 * Example usage:
 * <pre>
 * // Inject MessageService
 * private final MessageService messageService;
 *
 * // Get message for current locale (from LocaleContextHolder)
 * String message = messageService.getMessage("user.created");
 *
 * // Get message with parameters
 * String message = messageService.getMessage("validation.min.length", new Object[]{"Password", 8});
 *
 * // Get message for specific locale
 * String message = messageService.getMessage("auth.login.success", Locale.FRENCH);
 * </pre>
 *
 * @see org.gripday.userservice.infrastructure.i18n.MessageService
 * @see org.gripday.userservice.infrastructure.config.I18nConfig
 */
package org.gripday.userservice.infrastructure.i18n;
