package org.gripday.gatewayservice.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  public GlobalExceptionHandler(ObjectMapper objectMapper) {
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

    if (ex instanceof ResponseStatusException rse) {
      status = HttpStatus.valueOf(rse.getStatusCode().value());
      errorCode = determineErrorCode(status);
      message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
    } else if (ex instanceof SecurityException) {
      status = HttpStatus.FORBIDDEN;
      errorCode = "AUTH_ACCESS_DENIED";
      message = "Access denied";
    } else if (ex.getCause() instanceof io.jsonwebtoken.JwtException) {
      status = HttpStatus.UNAUTHORIZED;
      errorCode = "AUTH_TOKEN_INVALID";
      message = "Invalid authentication token";
    } else {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
      errorCode = "SYSTEM_INTERNAL_ERROR";
      message = "Internal server error";
    }

    logger.error("Gateway error - Status: {}, Code: {}, Message: {}, Path: {}, CorrelationId: {}, TenantId: {}",
        status.value(), errorCode, message, request.getPath().value(), correlationId, tenantId, ex);

    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
    pd.setTitle(status.getReasonPhrase());
    pd.setInstance(URI.create(request.getPath().value()));
    pd.setType(URI.create("/problems/" + errorCode.toLowerCase(java.util.Locale.ROOT)));
    pd.setProperty("code", errorCode);
    pd.setProperty("timestamp", Instant.now().toString());
    if (correlationId != null) {
      pd.setProperty("correlationId", correlationId);
    }
    if (tenantId != null) {
      pd.setProperty("tenantId", tenantId);
    }

    try {
      byte[] body = objectMapper.writeValueAsBytes(pd);
      var buffer = response.bufferFactory().wrap(body);
      return response.writeWith(Mono.just(buffer));
    } catch (Exception writeEx) {
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