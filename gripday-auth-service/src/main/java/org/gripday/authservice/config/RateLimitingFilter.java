package org.gripday.authservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.authservice.domain.service.RateLimitingService;
import org.gripday.authservice.domain.service.SecurityAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for rate limiting authentication endpoints. Uses Redis-backed rate limiting with IP-based tracking.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private final RateLimitingService rateLimitingService;
  private final SecurityAuditService securityAuditService;
  private final ObjectMapper objectMapper;

  // Endpoints that should be rate limited
  private static final java.util.Set<String> RATE_LIMITED_ENDPOINTS = java.util.Set.of(
      "/api/v1/auth/login",
      "/api/v1/auth/signup",
      "/api/v1/auth/refresh"
  );

  public RateLimitingFilter(RateLimitingService rateLimitingService,
      SecurityAuditService securityAuditService,
      ObjectMapper objectMapper) {
    this.rateLimitingService = rateLimitingService;
    this.securityAuditService = securityAuditService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    var requestPath = request.getRequestURI();

    // Only apply rate limiting to specific endpoints
    if (!RATE_LIMITED_ENDPOINTS.contains(requestPath)) {
      filterChain.doFilter(request, response);
      return;
    }

    var ipAddress = getClientIpAddress(request);
    var userAgent = request.getHeader("User-Agent");

    // Check rate limit
    if (!rateLimitingService.isWithinRateLimit(ipAddress)) {
      // Rate limit exceeded
      securityAuditService.logRateLimitExceeded(ipAddress, userAgent, requestPath);

      var remainingTime = rateLimitingService.getTimeUntilReset(ipAddress);

      sendRateLimitResponse(response, remainingTime.getSeconds());
      return;
    }

    // Add rate limit headers to response
    var remainingAttempts = rateLimitingService.getRemainingAttempts(ipAddress);
    response.setHeader("X-RateLimit-Remaining", String.valueOf(remainingAttempts));
    response.setHeader("X-RateLimit-Limit", "5");
    response.setHeader("X-RateLimit-Window", "60");

    filterChain.doFilter(request, response);
  }

  /**
   * Get client IP address, considering proxy headers.
   */
  private String getClientIpAddress(HttpServletRequest request) {
    // Check for X-Forwarded-For header (common in load balancers)
    var headerxForwardedFor = request.getHeader("X-Forwarded-For");
    if (headerxForwardedFor != null && !headerxForwardedFor.isEmpty()) {
      // Take the first IP in the chain
      return headerxForwardedFor.split(",")[0].trim();
    }

    // Check for X-Real-IP header (nginx)
    var headerxRealIp = request.getHeader("X-Real-IP");
    if (headerxRealIp != null && !headerxRealIp.isEmpty()) {
      return headerxRealIp;
    }

    // Fall back to remote address
    return request.getRemoteAddr();
  }

  /**
   * Send rate limit exceeded response.
   */
  private void sendRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType("application/problem+json");
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

    var errorResponse = Map.of(
        "type", "https://problems.pynity.com/rate-limit-exceeded",
        "title", "Too Many Requests",
        "status", HttpStatus.TOO_MANY_REQUESTS.value(),
        "detail", "Too many requests. Please try again later.",
        "instance", "/api/v1/auth",
        "code", "RATE_LIMIT_EXCEEDED",
        "retryAfter", retryAfterSeconds
    );

    var jsonResponse = objectMapper.writeValueAsString(errorResponse);
    response.getWriter().write(jsonResponse);
  }
}