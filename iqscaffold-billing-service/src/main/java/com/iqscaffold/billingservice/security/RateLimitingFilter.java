package com.iqscaffold.billingservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.config.BillingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for rate limiting public endpoints.
 * 
 * <p>Uses Redis-backed rate limiting with IP-based tracking to prevent abuse
 * of public APIs. The filter applies to public endpoints that don't require
 * authentication, such as plan listing and webhook endpoints.
 * 
 * <p>Rate limiting is configured via {@link BillingProperties.Security.RateLimiting}
 * and can be disabled for testing or development environments.
 * 
 * <p>This filter follows the user-service pattern for consistent rate limiting
 * across the platform.
 */
@Component
@Order(1) // Execute before authentication filters
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

  private final RateLimitingService rateLimitingService;
  private final BillingProperties billingProperties;
  private final ObjectMapper objectMapper;

  // Public endpoints that should be rate limited
  private static final Set<String> RATE_LIMITED_ENDPOINTS = Set.of(
      "/api/v1/billing/plans",
      "/api/v1/billing/webhooks/stripe",
      "/api/v1/billing/webhooks/paypal"
  );

  public RateLimitingFilter(
      RateLimitingService rateLimitingService,
      BillingProperties billingProperties,
      ObjectMapper objectMapper
  ) {
    this.rateLimitingService = rateLimitingService;
    this.billingProperties = billingProperties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    // Skip if rate limiting is disabled
    if (!billingProperties.security().rateLimiting().enabled()) {
      filterChain.doFilter(request, response);
      return;
    }

    String requestPath = request.getRequestURI();

    // Only apply rate limiting to public endpoints
    if (!shouldRateLimit(requestPath)) {
      filterChain.doFilter(request, response);
      return;
    }

    String ipAddress = getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");

    logger.debug("Checking rate limit for IP: {} on path: {}", ipAddress, requestPath);

    // Check rate limit
    if (!rateLimitingService.isWithinRateLimit(ipAddress)) {
      // Rate limit exceeded
      logger.warn("Rate limit exceeded for IP: {} on path: {} (User-Agent: {})", 
          ipAddress, requestPath, userAgent);

      var remainingTime = rateLimitingService.getTimeUntilReset(ipAddress);
      sendRateLimitResponse(response, remainingTime.getSeconds());
      return;
    }

    // Add rate limit headers to response
    int remainingAttempts = rateLimitingService.getRemainingAttempts(ipAddress);
    int maxRequests = billingProperties.security().rateLimiting().requestsPerMinute();
    
    response.setHeader("X-RateLimit-Remaining", String.valueOf(remainingAttempts));
    response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
    response.setHeader("X-RateLimit-Window", "60");

    filterChain.doFilter(request, response);
  }

  /**
   * Check if the request path should be rate limited.
   * 
   * @param requestPath the request URI
   * @return true if the path should be rate limited
   */
  private boolean shouldRateLimit(String requestPath) {
    return RATE_LIMITED_ENDPOINTS.stream()
        .anyMatch(requestPath::startsWith);
  }

  /**
   * Get client IP address, considering proxy headers.
   * 
   * <p>Checks X-Forwarded-For and X-Real-IP headers commonly used by
   * load balancers and reverse proxies before falling back to remote address.
   * 
   * @param request the HTTP request
   * @return the client IP address
   */
  private String getClientIpAddress(HttpServletRequest request) {
    // Check for X-Forwarded-For header (common in load balancers)
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      // Take the first IP in the chain (original client)
      return xForwardedFor.split(",")[0].trim();
    }

    // Check for X-Real-IP header (nginx)
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    // Fall back to remote address
    return request.getRemoteAddr();
  }

  /**
   * Send rate limit exceeded response.
   * 
   * <p>Returns HTTP 429 Too Many Requests with RFC 7807 Problem Details format.
   * 
   * @param response the HTTP response
   * @param retryAfterSeconds seconds until rate limit resets
   * @throws IOException if writing response fails
   */
  private void sendRateLimitResponse(
      HttpServletResponse response,
      long retryAfterSeconds
  ) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType("application/problem+json");
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

    var errorResponse = Map.of(
        "type", "https://problems.iqscaffold.com/rate-limit-exceeded",
        "title", "Too Many Requests",
        "status", HttpStatus.TOO_MANY_REQUESTS.value(),
        "detail", "Too many requests from your IP address. Please try again later.",
        "instance", "/api/v1/billing",
        "code", "RATE_LIMIT_EXCEEDED",
        "retryAfter", retryAfterSeconds
    );

    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
    response.getWriter().write(jsonResponse);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String requestURI = request.getRequestURI();

    // Skip rate limiting for actuator, swagger, and error endpoints
    return requestURI.startsWith("/actuator/")
           || requestURI.startsWith("/swagger-ui/")
           || requestURI.startsWith("/v3/api-docs/")
           || requestURI.equals("/favicon.ico")
           || requestURI.equals("/error");
  }
}
