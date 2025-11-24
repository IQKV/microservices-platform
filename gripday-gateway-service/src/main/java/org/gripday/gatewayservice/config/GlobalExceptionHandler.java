package org.gripday.gatewayservice.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.gatewayservice.exception.CircuitBreakerOpenException;
import org.gripday.gatewayservice.exception.InvalidJwtTokenException;
import org.gripday.gatewayservice.exception.MissingTenantContextException;
import org.gripday.gatewayservice.exception.NoHealthyInstancesException;
import org.gripday.gatewayservice.exception.RateLimitExceededException;
import org.gripday.gatewayservice.exception.UnsupportedApiVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for the Gateway Service. Provides consistent error responses for authentication failures and tenant access violations.
 */
@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final ObjectMapper objectMapper;

  public GlobalExceptionHandler(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    var response = exchange.getResponse();
    var request = exchange.getRequest();

    var correlationId = MDC.get("correlationId");
    var tenantId = MDC.get("tenantId");

    HttpStatus status;
    String errorCode;
    String message;
    ProblemDetail pd;

    // Handle custom exceptions with specific logic
    if (ex instanceof RateLimitExceededException rateLimitEx) {
      status = HttpStatus.TOO_MANY_REQUESTS;
      errorCode = "RATE_LIMIT_EXCEEDED";
      message = rateLimitEx.getReason();
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      pd.setProperty("rateLimitType", rateLimitEx.getType().name());
      pd.setProperty("retryAfter", rateLimitEx.getRetryAfterSeconds());
      response.getHeaders().add("X-RateLimit-Limit", "60");
      response.getHeaders().add("X-RateLimit-Remaining", "0");
      response.getHeaders().add("Retry-After", String.valueOf(rateLimitEx.getRetryAfterSeconds()));
      logger.warn("Rate limit exceeded - Type: {}, Tenant: {}, Path: {}",
          rateLimitEx.getType(), rateLimitEx.getTenantId(), rateLimitEx.getPath());
    } else if (ex instanceof NoHealthyInstancesException noInstancesEx) {
      status = HttpStatus.SERVICE_UNAVAILABLE;
      errorCode = "NO_HEALTHY_INSTANCES";
      message = noInstancesEx.getReason();
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      pd.setProperty("serviceName", noInstancesEx.getServiceName());
      pd.setProperty("totalInstances", noInstancesEx.getTotalInstances());
      logger.error("No healthy instances - Service: {}, Total: {}",
          noInstancesEx.getServiceName(), noInstancesEx.getTotalInstances());
    } else if (ex instanceof UnsupportedApiVersionException versionEx) {
      status = HttpStatus.BAD_REQUEST;
      errorCode = "UNSUPPORTED_API_VERSION";
      message = versionEx.getReason();
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      pd.setProperty("requestedVersion", versionEx.getRequestedVersion());
      pd.setProperty("supportedVersions", versionEx.getSupportedVersions());
      logger.warn("Unsupported API version - Requested: {}, Supported: {}",
          versionEx.getRequestedVersion(), versionEx.getSupportedVersions());
    } else if (ex instanceof MissingTenantContextException tenantEx) {
      status = HttpStatus.BAD_REQUEST;
      errorCode = "MISSING_TENANT_CONTEXT";
      message = tenantEx.getReason();
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      pd.setProperty("path", tenantEx.getPath());
      logger.warn("Missing tenant context - Path: {}", tenantEx.getPath());
    } else if (ex instanceof InvalidJwtTokenException jwtEx) {
      status = HttpStatus.UNAUTHORIZED;
      errorCode = "AUTH_TOKEN_INVALID";
      message = jwtEx.getReason();
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      pd.setProperty("errorType", jwtEx.getErrorType().name());
      logger.warn("Invalid JWT token - Type: {}, Reason: {}", jwtEx.getErrorType(), jwtEx.getReasonDetail());
    } else if (ex instanceof CircuitBreakerOpenException circuitEx) {
      status = HttpStatus.SERVICE_UNAVAILABLE;
      errorCode = "CIRCUIT_BREAKER_OPEN";
      message = circuitEx.getReason();
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      pd.setProperty("circuitBreaker", circuitEx.getCircuitBreakerName());
      pd.setProperty("serviceName", circuitEx.getServiceName());
      pd.setProperty("retryAfter", circuitEx.getRetryAfterSeconds());
      response.getHeaders().add("X-Circuit-Breaker", circuitEx.getCircuitBreakerName());
      response.getHeaders().add("Retry-After", String.valueOf(circuitEx.getRetryAfterSeconds()));
      logger.warn("Circuit breaker open - Name: {}, Service: {}",
          circuitEx.getCircuitBreakerName(), circuitEx.getServiceName());
    } else if (ex instanceof ResponseStatusException rse) {
      status = HttpStatus.valueOf(rse.getStatusCode().value());
      errorCode = determineErrorCode(status);
      message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      logger.error("Gateway error - Status: {}, Code: {}, Message: {}", status.value(), errorCode, message, ex);
    } else if (ex instanceof SecurityException) {
      status = HttpStatus.FORBIDDEN;
      errorCode = "AUTH_ACCESS_DENIED";
      message = "Access denied";
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      logger.error("Security error - Access denied", ex);
    } else if (ex.getCause() instanceof io.jsonwebtoken.JwtException) {
      status = HttpStatus.UNAUTHORIZED;
      errorCode = "AUTH_TOKEN_INVALID";
      message = "Invalid authentication token";
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      logger.error("JWT error - Invalid token", ex);
    } else {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
      errorCode = "SYSTEM_INTERNAL_ERROR";
      message = "Internal server error";
      pd = createProblemDetail(status, message, errorCode, request.getPath().value(), correlationId, tenantId);
      logger.error("Unexpected error - Path: {}", request.getPath().value(), ex);
    }

    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    try {
      byte[] body = objectMapper.writeValueAsBytes(pd);
      var buffer = response.bufferFactory().wrap(body);
      return response.writeWith(Mono.just(buffer));
    } catch (final Exception writeEx) {
      var fallback = ("{\n  \"type\": \"" + pd.getType() + "\",\n" +
                      "  \"title\": \"" + pd.getTitle() + "\",\n" +
                      "  \"status\": " + pd.getStatus() + ",\n" +
                      "  \"detail\": \"" + message + "\",\n" +
                      "  \"instance\": \"" + request.getPath().value() + "\"\n}")
          .getBytes(StandardCharsets.UTF_8);
      var buffer = response.bufferFactory().wrap(fallback);
      return response.writeWith(Mono.just(buffer));
    }
  }

  private ProblemDetail createProblemDetail(HttpStatus status, String message, String errorCode,
                                            String path, String correlationId, String tenantId) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
    pd.setTitle(status.getReasonPhrase());
    pd.setInstance(URI.create(path));
    pd.setType(URI.create("/problems/" + errorCode.toLowerCase(java.util.Locale.ROOT)));
    pd.setProperty("code", errorCode);
    pd.setProperty("timestamp", Instant.now().toString());
    if (correlationId != null) {
      pd.setProperty("correlationId", correlationId);
    }
    if (tenantId != null) {
      pd.setProperty("tenantId", tenantId);
    }
    return pd;
  }

  private String determineErrorCode(HttpStatus status) {
    return switch (status) {
      case UNAUTHORIZED -> "AUTH_TOKEN_MISSING";
      case FORBIDDEN -> "AUTH_ACCESS_DENIED";
      case TOO_MANY_REQUESTS -> "RATE_LIMIT_EXCEEDED";
      case SERVICE_UNAVAILABLE -> "CIRCUIT_BREAKER_OPEN";
      case BAD_REQUEST -> "VALIDATION_ERROR";
      case NOT_FOUND -> "RESOURCE_NOT_FOUND";
      default -> "SYSTEM_ERROR";
    };
  }
}
