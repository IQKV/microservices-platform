package com.iqscaffold.gatewayservice.filter;

import com.iqscaffold.gatewayservice.common.GatewayConstants;
import com.iqscaffold.gatewayservice.config.IqScaffoldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

/**
 * Gateway filter for extracting and standardizing locale information from requests.
 * Establishes locale context for downstream services with priority order:
 * 1. X-User-Locale header (from authenticated user preferences)
 * 2. Accept-Language header
 * 3. Default locale (configurable)
 */
@Component
public class LocaleExtractionFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LocaleExtractionFilter.class);
    
    private final IqScaffoldProperties properties;

    public LocaleExtractionFilter(IqScaffoldProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        
        // Extract locale from various sources with priority order
        var resolvedLocale = extractLocale(request);
        
        if (StringUtils.hasText(resolvedLocale)) {
            // Add locale to MDC for structured logging
            MDC.put(GatewayConstants.MdcKeys.LOCALE, resolvedLocale);
            
            // Store locale context in exchange attributes
            exchange.getAttributes().put(GatewayConstants.Attributes.LOCALE, resolvedLocale);
            
            // Enrich request with standardized locale header for downstream services
            var mutatedRequest = request.mutate()
                .header(GatewayConstants.Headers.X_USER_LOCALE, resolvedLocale)
                .build();
            
            logger.debug("Established locale context: {}", resolvedLocale);
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> MDC.remove(GatewayConstants.MdcKeys.LOCALE));
        }
        
        return chain.filter(exchange)
            .doFinally(signalType -> MDC.remove(GatewayConstants.MdcKeys.LOCALE));
    }

    private String extractLocale(org.springframework.http.server.reactive.ServerHttpRequest request) {
        var i18nConfig = properties.i18n();
        
        // Priority 1: X-User-Locale header (from user preferences)
        var userLocaleHeader = request.getHeaders().getFirst(GatewayConstants.Headers.X_USER_LOCALE);
        if (StringUtils.hasText(userLocaleHeader) && i18nConfig.isLocaleSupported(userLocaleHeader)) {
            logger.debug("Locale extracted from user preference header: {}", userLocaleHeader);
            return userLocaleHeader;
        }
        
        // Priority 2: Accept-Language header
        var acceptLanguageHeader = request.getHeaders().getFirst("Accept-Language");
        if (StringUtils.hasText(acceptLanguageHeader)) {
            var parsedLocale = parseAcceptLanguage(acceptLanguageHeader, i18nConfig.getSupportedLocaleObjects());
            if (parsedLocale != null) {
                logger.debug("Locale extracted from Accept-Language header: {}", parsedLocale);
                return parsedLocale;
            }
        }
        
        // Priority 3: Default locale
        var defaultLocale = i18nConfig.defaultLocale();
        logger.debug("Using default locale: {}", defaultLocale);
        return defaultLocale;
    }

    private String parseAcceptLanguage(String acceptLanguage, List<Locale> supportedLocales) {
        try {
            var locales = Locale.LanguageRange.parse(acceptLanguage);
            for (var range : locales) {
                var languageTag = range.getRange();
                var locale = Locale.forLanguageTag(languageTag);
                
                // Check exact match first
                if (supportedLocales.contains(locale)) {
                    return locale.toLanguageTag();
                }
                
                // Check language-only match
                var languageOnlyLocale = new Locale(locale.getLanguage());
                for (var supported : supportedLocales) {
                    if (supported.getLanguage().equals(languageOnlyLocale.getLanguage())) {
                        return supported.toLanguageTag();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse Accept-Language header: {}", acceptLanguage, e);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return GatewayConstants.FilterOrder.LOCALE_EXTRACTION_FILTER;
    }
}