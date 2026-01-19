package com.iqscaffold.contactservice.feature;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that extracts feature context from gateway headers and sets it in the thread-local context.
 * This filter runs early in the filter chain to ensure feature context is available for business logic.
 */
@Component
@Order(-150) // Run after tenant extraction but before business logic
public class FeatureExtractionFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(FeatureExtractionFilter.class);

  /**
   * Header constants for feature context propagation.
   * These should match the constants defined in the gateway service.
   */
  public static final class Headers {
    public static final String X_ENABLED_FEATURES = "X-Enabled-Features";
    public static final String X_PLAN_ID = "X-Plan-ID";
    public static final String X_PLAN_NAME = "X-Plan-Name";
    public static final String X_FEATURE_QUOTAS = "X-Feature-Quotas";
    public static final String X_FEATURE_LIMITS = "X-Feature-Limits";
    public static final String X_FEATURE_TIERS = "X-Feature-Tiers";
  }

  private final ObjectMapper objectMapper;

  public FeatureExtractionFilter(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // Extract feature context from headers
      FeatureContext featureContext = extractFeatureContext(request);

      // Set in thread-local context
      FeatureContextHolder.setContext(featureContext);

      logger.debug("Feature context set for request: {} enabled features", featureContext.enabledFeatures().size());

      // Continue with the request
      filterChain.doFilter(request, response);

    } finally {
      // Always clear context to prevent memory leaks
      FeatureContextHolder.clearContext();
    }
  }

  /**
   * Extracts feature context from request headers.
   */
  private FeatureContext extractFeatureContext(HttpServletRequest request) {
    try {
      // Extract enabled features
      Set<String> enabledFeatures = parseEnabledFeatures(request.getHeader(Headers.X_ENABLED_FEATURES));

      // Extract plan information
      String planId = request.getHeader(Headers.X_PLAN_ID);
      String planName = request.getHeader(Headers.X_PLAN_NAME);

      // Extract quotas, limits, and tiers
      Map<String, Integer> quotas = parseJsonMap(request.getHeader(Headers.X_FEATURE_QUOTAS), Integer.class);
      Map<String, Integer> limits = parseJsonMap(request.getHeader(Headers.X_FEATURE_LIMITS), Integer.class);
      Map<String, String> tiers = parseJsonMap(request.getHeader(Headers.X_FEATURE_TIERS), String.class);

      return new FeatureContext(enabledFeatures, planId, planName, quotas, limits, tiers);

    } catch (final Exception e) {
      logger.warn("Failed to parse feature context from headers: {}", e.getMessage());
      return FeatureContext.empty();
    }
  }

  /**
   * Parses comma-separated enabled features.
   */
  private Set<String> parseEnabledFeatures(String featuresHeader) {
    if (!StringUtils.hasText(featuresHeader)) {
      return Set.of();
    }

    return Arrays.stream(featuresHeader.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
  }

  /**
   * Parses JSON map from header value.
   */
  private <T> Map<String, T> parseJsonMap(String jsonHeader, Class<T> valueType) {
    if (!StringUtils.hasText(jsonHeader)) {
      return Map.of();
    }

    try {
      TypeReference<Map<String, T>> typeRef = new TypeReference<Map<String, T>>() {
      };
      return objectMapper.readValue(jsonHeader, typeRef);
    } catch (final Exception e) {
      logger.debug("Failed to parse JSON header: {}", e.getMessage());
      return Map.of();
    }
  }
}
